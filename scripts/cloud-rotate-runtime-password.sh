#!/usr/bin/env bash

# Rotate runtime DB user password safely using admin credentials.
# This script updates the DB user password and local app config password.
#
# Required:
#   NEW_RUNTIME_PASSWORD='new-secret'
#   ADMIN_CONFIG_FILE=~/.dental_clinic/db.properties.bak.YYYYMMDD_HHMMSS
#
# Optional:
#   CONFIG_FILE=~/.dental_clinic/db.properties
#   RUNTIME_DB_HOST=%

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/lib/db-config.sh"

CONFIG_FILE="${CONFIG_FILE:-$HOME/.dental_clinic/db.properties}"
ADMIN_CONFIG_FILE="${ADMIN_CONFIG_FILE:-}"
NEW_RUNTIME_PASSWORD="${NEW_RUNTIME_PASSWORD:-}"
RUNTIME_DB_USER="${RUNTIME_DB_USER:-}"
RUNTIME_DB_HOST="${RUNTIME_DB_HOST:-%}"
RUNTIME_DB_PRIVILEGES="${RUNTIME_DB_PRIVILEGES:-SELECT,INSERT,UPDATE,DELETE}"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "ERROR: Runtime config not found: $CONFIG_FILE"
    exit 1
fi

if [[ -z "$ADMIN_CONFIG_FILE" || ! -f "$ADMIN_CONFIG_FILE" ]]; then
    echo "ERROR: ADMIN_CONFIG_FILE is required and must exist."
    exit 1
fi

if [[ -z "$NEW_RUNTIME_PASSWORD" ]]; then
    echo "ERROR: NEW_RUNTIME_PASSWORD is required."
    exit 1
fi

if [[ -z "$RUNTIME_DB_USER" ]]; then
    echo "ERROR: RUNTIME_DB_USER is required (example: dentalclinicsystem)."
    exit 1
fi

load_db_config "$ADMIN_CONFIG_FILE"
mapfile -t MYSQL_BASE_ARGS < <(mysql_base_args)
mapfile -t MYSQL_SSL_ARGS < <(mysql_ssl_args)

if [[ "$RUNTIME_DB_USER" == "$DB_USER" ]]; then
    echo "ERROR: RUNTIME_DB_USER matches admin-config db.user ($DB_USER)."
    echo "Use admin config only for privileged execution and set runtime user explicitly."
    exit 1
fi

echo "Target DB: $DB_HOST:$DB_PORT/$DB_NAME"
echo "Rotating runtime principal: '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST'"
echo "Using admin config: $ADMIN_CONFIG_FILE"
read -r -p "Type 'YES' to rotate runtime password now: " confirm
if [[ "$confirm" != "YES" ]]; then
    echo "Cancelled."
    exit 0
fi

safe_db_name="${DB_NAME//\`/}"

sql=$(cat <<SQL
ALTER USER '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST' IDENTIFIED BY '$NEW_RUNTIME_PASSWORD' REQUIRE SSL;
GRANT $RUNTIME_DB_PRIVILEGES ON \`$safe_db_name\`.* TO '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST';
FLUSH PRIVILEGES;
SHOW GRANTS FOR '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST';
SQL
)

if ! MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" -e "$sql"; then
    echo "ERROR: Failed to rotate password in database."
    exit 1
fi

backup="$CONFIG_FILE.bak.$(date +%Y%m%d_%H%M%S)"
cp "$CONFIG_FILE" "$backup"

tmp_file="$(mktemp)"
awk -v new_pass="$NEW_RUNTIME_PASSWORD" '
{
    if ($0 ~ /^[[:space:]]*db\.password[[:space:]]*=/) {
        print "db.password=" new_pass;
        done=1;
    } else {
        print $0;
    }
}
END {
    if (!done) print "db.password=" new_pass;
}
' "$CONFIG_FILE" > "$tmp_file"
mv "$tmp_file" "$CONFIG_FILE"
chmod 600 "$CONFIG_FILE"

echo "Rotation successful."
echo "Config backup: $backup"
echo "Next: run ./scripts/cloud-db-health-check.sh and ./scripts/cloud-security-audit.sh"
