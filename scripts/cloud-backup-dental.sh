#!/usr/bin/env bash

# Cloud-safe database backup script.
# Uses db.url/db.user/db.password from ~/.dental_clinic/db.properties.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/lib/db-config.sh"

BACKUP_DIR="${BACKUP_DIR:-$HOME/backups/dental_clinic}"
LOG_DIR="${LOG_DIR:-$HOME/backups/logs}"
KEEP_DAYS="${KEEP_DAYS:-30}"
DATE="$(date +%Y%m%d_%H%M%S)"
LOG_FILE="$LOG_DIR/cloud_backup_$DATE.log"
TMP_SQL="$BACKUP_DIR/backup_$DATE.sql"
OUT_GZ="$TMP_SQL.gz"

mkdir -p "$BACKUP_DIR" "$LOG_DIR"

load_db_config
mapfile -t MYSQL_BASE_ARGS < <(mysql_base_args)
mapfile -t MYSQL_SSL_ARGS < <(mysql_ssl_args)

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "Starting cloud backup for $DB_HOST:$DB_PORT/$DB_NAME"

log "Testing DB connectivity"
if ! MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SELECT 1" >/dev/null 2>&1; then
    log "ERROR: Cannot connect to database"
    exit 1
fi

log "Running mysqldump -> $(basename "$TMP_SQL")"
if ! MYSQL_PWD="$DB_PASSWORD" "$MYSQLDUMP_BIN" \
    "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" \
    --single-transaction --skip-lock-tables --quick --routines --triggers \
    --no-tablespaces --set-gtid-purged=OFF \
    --add-drop-table --complete-insert \
    "$DB_NAME" > "$TMP_SQL" 2>>"$LOG_FILE"; then
    log "ERROR: mysqldump failed"
    rm -f "$TMP_SQL"
    exit 1
fi

if [[ ! -s "$TMP_SQL" ]]; then
    log "ERROR: Dump file is missing or empty"
    rm -f "$TMP_SQL"
    exit 1
fi

if ! grep -q "Dump completed" "$TMP_SQL"; then
    log "ERROR: Dump appears incomplete (missing completion marker)"
    rm -f "$TMP_SQL"
    exit 1
fi

log "Compressing backup"
gzip -f "$TMP_SQL"

if ! gzip -t "$OUT_GZ" 2>/dev/null; then
    log "ERROR: Compressed backup is corrupted"
    rm -f "$OUT_GZ"
    exit 1
fi

ln -sfn "$OUT_GZ" "$BACKUP_DIR/latest_backup.sql.gz"

log "Applying retention policy: keep $KEEP_DAYS days"
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +"$KEEP_DAYS" -delete
find "$LOG_DIR" -name "cloud_backup_*.log" -mtime +"$KEEP_DAYS" -delete

size="$(du -h "$OUT_GZ" | cut -f1)"
count="$(ls -1 "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null | wc -l | tr -d ' ')"
log "Backup success: $(basename "$OUT_GZ") (size: $size, total backups: $count)"

exit 0
