#!/usr/bin/env bash

# Cloud-safe restore from latest compressed SQL backup.
# WARNING: This overwrites objects in the target database.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/lib/db-config.sh"

BACKUP_DIR="${BACKUP_DIR:-$HOME/backups/dental_clinic}"
TARGET_DB="${TARGET_DB:-}"

load_db_config
mapfile -t MYSQL_BASE_ARGS < <(mysql_base_args)
mapfile -t MYSQL_SSL_ARGS < <(mysql_ssl_args)

if [[ -z "$TARGET_DB" ]]; then
    TARGET_DB="$DB_NAME"
fi

LATEST="$(ls -t "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null | head -n 1 || true)"
if [[ -z "$LATEST" ]]; then
    echo "ERROR: No backup found in $BACKUP_DIR"
    exit 1
fi

echo "Restoring backup: $LATEST"
echo "Target: $DB_HOST:$DB_PORT / $TARGET_DB"
echo "WARNING: This will overwrite data in the target database."
read -r -p "Type 'YES' to continue: " confirm

if [[ "$confirm" != "YES" ]]; then
    echo "Restore cancelled."
    exit 0
fi

if ! gzip -t "$LATEST" 2>/dev/null; then
    echo "ERROR: Backup archive is corrupted: $LATEST"
    exit 1
fi

if ! MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" -Nse "SELECT 1" >/dev/null 2>&1; then
    echo "ERROR: Cannot connect to DB server"
    exit 1
fi

if ! MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" -Nse "CREATE DATABASE IF NOT EXISTS \`$TARGET_DB\`" >/dev/null 2>&1; then
    echo "ERROR: Could not ensure target database exists (permission issue likely)."
    exit 1
fi

if gunzip -c "$LATEST" | MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$TARGET_DB"; then
    echo "Restore completed successfully."
    exit 0
fi

echo "ERROR: Restore failed."
exit 1
