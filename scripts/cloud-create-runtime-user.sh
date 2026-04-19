#!/usr/bin/env bash

# Create or update a least-privileged runtime DB user for cloud deployment.
# Usage:
#   RUNTIME_DB_USER=dc_app RUNTIME_DB_PASSWORD='strong-pass' ./scripts/cloud-create-runtime-user.sh
# Optional:
#   RUNTIME_DB_HOST='%' RUNTIME_DB_PRIVILEGES='SELECT,INSERT,UPDATE,DELETE' ./scripts/cloud-create-runtime-user.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/lib/db-config.sh"

load_db_config
mapfile -t MYSQL_BASE_ARGS < <(mysql_base_args)
mapfile -t MYSQL_SSL_ARGS < <(mysql_ssl_args)

RUNTIME_DB_USER="${RUNTIME_DB_USER:-}"
RUNTIME_DB_PASSWORD="${RUNTIME_DB_PASSWORD:-}"
RUNTIME_DB_HOST="${RUNTIME_DB_HOST:-%}"
RUNTIME_DB_PRIVILEGES="${RUNTIME_DB_PRIVILEGES:-SELECT,INSERT,UPDATE,DELETE}"

if [[ -z "$RUNTIME_DB_USER" || -z "$RUNTIME_DB_PASSWORD" ]]; then
    echo "ERROR: RUNTIME_DB_USER and RUNTIME_DB_PASSWORD are required."
    exit 1
fi

echo "Target DB: $DB_HOST:$DB_PORT / $DB_NAME"
echo "Current admin user from config: $DB_USER"
echo "New runtime principal: '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST'"

read -r -p "Type 'YES' to apply least-privilege user changes: " confirm
if [[ "$confirm" != "YES" ]]; then
    echo "Cancelled."
    exit 0
fi

# Escape backticks in DB name for safety in SQL identifier context.
safe_db_name="${DB_NAME//\`/}"

sql=$(cat <<SQL
DROP USER IF EXISTS '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST';
CREATE USER '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST' IDENTIFIED BY '$RUNTIME_DB_PASSWORD' REQUIRE SSL;
GRANT $RUNTIME_DB_PRIVILEGES ON \`$safe_db_name\`.* TO '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST';
FLUSH PRIVILEGES;
SHOW GRANTS FOR '$RUNTIME_DB_USER'@'$RUNTIME_DB_HOST';
SQL
)

if MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" -e "$sql"; then
    echo "Least-privilege runtime user configured successfully."
    exit 0
fi

echo "ERROR: Failed to create/update runtime user."
exit 1
