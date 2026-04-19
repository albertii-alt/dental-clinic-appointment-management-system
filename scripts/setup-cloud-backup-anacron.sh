#!/usr/bin/env bash

# Install or update an anacron job for cloud DB backups.
# Intended for laptops that may be offline at a fixed backup hour.

set -euo pipefail

PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
APP_USER="${APP_USER:-${SUDO_USER:-$USER}}"
ANACRON_PERIOD="${ANACRON_PERIOD:-1}"
ANACRON_DELAY="${ANACRON_DELAY:-10}"
ANACRON_JOB_ID="${ANACRON_JOB_ID:-dental-clinic-cloud-backup}"
ANACRONTAB="${ANACRONTAB:-/etc/anacrontab}"
RUNNER_SCRIPT="${RUNNER_SCRIPT:-/usr/local/bin/dental-clinic-cloud-backup}"

if [[ "${EUID}" -ne 0 ]]; then
    echo "ERROR: Run this installer with sudo/root."
    echo "Example: sudo ./scripts/setup-cloud-backup-anacron.sh"
    exit 1
fi

if [[ ! -f "$ANACRONTAB" ]]; then
    echo "ERROR: $ANACRONTAB not found. Install anacron first."
    exit 1
fi

if ! id "$APP_USER" >/dev/null 2>&1; then
    echo "ERROR: App user '$APP_USER' does not exist."
    exit 1
fi

APP_HOME="$(getent passwd "$APP_USER" | cut -d: -f6)"
if [[ -z "$APP_HOME" || ! -d "$APP_HOME" ]]; then
    echo "ERROR: Could not resolve home directory for '$APP_USER'."
    exit 1
fi

if [[ ! -x "$PROJECT_DIR/scripts/cloud-backup-dental.sh" ]]; then
    echo "ERROR: Missing executable backup script at $PROJECT_DIR/scripts/cloud-backup-dental.sh"
    echo "Run: chmod +x $PROJECT_DIR/scripts/cloud-backup-dental.sh"
    exit 1
fi

cat > "$RUNNER_SCRIPT" <<EOF
#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$PROJECT_DIR"
APP_USER="$APP_USER"
APP_HOME="$APP_HOME"
LOG_DIR="\$APP_HOME/backups/logs"
BACKUP_DIR="\$APP_HOME/backups/dental_clinic"
CRON_LOG="\$LOG_DIR/cloud_backup_anacron.log"
STATUS_FILE="\$LOG_DIR/cloud_backup_last_status.txt"
LOCK_FILE="/tmp/dental_clinic_cloud_backup.lock"

mkdir -p "\$LOG_DIR" "\$BACKUP_DIR"

exec 9>"\$LOCK_FILE"
if command -v flock >/dev/null 2>&1; then
    if ! flock -n 9; then
        echo "[\$(date '+%Y-%m-%d %H:%M:%S')] Backup skipped: another backup process is running." >> "\$CRON_LOG"
        exit 0
    fi
fi

echo "[\$(date '+%Y-%m-%d %H:%M:%S')] anacron backup start" >> "\$CRON_LOG"
set +e
if command -v runuser >/dev/null 2>&1; then
    runuser -u "\$APP_USER" -- env HOME="\$APP_HOME" BACKUP_DIR="\$BACKUP_DIR" LOG_DIR="\$LOG_DIR" "\$PROJECT_DIR/scripts/cloud-backup-dental.sh" >> "\$CRON_LOG" 2>&1
    backup_rc=\$?
else
    su - "\$APP_USER" -s /bin/bash -c "HOME='\$APP_HOME' BACKUP_DIR='\$BACKUP_DIR' LOG_DIR='\$LOG_DIR' '\$PROJECT_DIR/scripts/cloud-backup-dental.sh'" >> "\$CRON_LOG" 2>&1
    backup_rc=\$?
fi
set -e

latest_backup=""
if compgen -G "\$BACKUP_DIR/backup_*.sql.gz" > /dev/null; then
    latest_backup="\$(ls -1t "\$BACKUP_DIR"/backup_*.sql.gz | head -n 1)"
fi

timestamp="\$(date '+%Y-%m-%d %H:%M:%S')"
if [[ "\$backup_rc" -eq 0 ]]; then
    result="SUCCESS"
    summary="Cloud DB backup succeeded"
else
    result="FAILED"
    summary="Cloud DB backup failed (exit \$backup_rc)"
fi

{
    echo "last_run=\$timestamp"
    echo "status=\$result"
    echo "exit_code=\$backup_rc"
    echo "latest_backup=\$latest_backup"
} > "\$STATUS_FILE"

echo "[\$timestamp] anacron backup end status=\$result exit_code=\$backup_rc latest=\$latest_backup" >> "\$CRON_LOG"

if command -v logger >/dev/null 2>&1; then
    logger -t dental-clinic-cloud-backup "\$summary latest=\$latest_backup"
fi

if command -v notify-send >/dev/null 2>&1 && command -v runuser >/dev/null 2>&1; then
    user_uid="\$(id -u \"\$APP_USER\")"
    runuser -u "\$APP_USER" -- env \
        DISPLAY=":0" \
        XDG_RUNTIME_DIR="/run/user/\$user_uid" \
        notify-send "Dental Clinic Backup" "\$summary" >/dev/null 2>&1 || true
fi

exit "\$backup_rc"
EOF

chmod 755 "$RUNNER_SCRIPT"

ENTRY="$ANACRON_PERIOD $ANACRON_DELAY $ANACRON_JOB_ID $RUNNER_SCRIPT"
TMP_FILE="$(mktemp)"

awk -v job="$ANACRON_JOB_ID" -v entry="$ENTRY" '
BEGIN { replaced=0 }
{
    if ($0 ~ "^[[:space:]]*[0-9]+[[:space:]]+[0-9]+[[:space:]]+" job "([[:space:]]+|$)") {
        if (replaced == 0) {
            print entry
            replaced=1
        }
        next
    }
    print $0
}
END {
    if (replaced == 0) {
        print entry
    }
}
' "$ANACRONTAB" > "$TMP_FILE"

cp "$ANACRONTAB" "$ANACRONTAB.bak.$(date +%Y%m%d_%H%M%S)"
cat "$TMP_FILE" > "$ANACRONTAB"
rm -f "$TMP_FILE"

echo "Installed anacron backup job:"
echo "  $ENTRY"
echo
echo "App user: $APP_USER"
echo "App home: $APP_HOME"
echo "Project: $PROJECT_DIR"
echo "Runner : $RUNNER_SCRIPT"
echo
echo "Test now with: sudo anacron -fn"
echo "Check logs: $APP_HOME/backups/logs/cloud_backup_anacron.log"
echo "Check status: $APP_HOME/backups/logs/cloud_backup_last_status.txt"
