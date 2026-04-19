#!/usr/bin/env bash

# Shows latest cloud backup status from anacron runner and latest backup artifact.

set -euo pipefail

STATUS_FILE="${STATUS_FILE:-$HOME/backups/logs/cloud_backup_last_status.txt}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups/dental_clinic}"

if [[ ! -f "$STATUS_FILE" ]]; then
    echo "No backup status file found yet: $STATUS_FILE"
    echo "Run a backup first (manual or anacron test)."
    exit 1
fi

echo "=== Cloud Backup Status ==="
cat "$STATUS_FILE"

echo
if compgen -G "$BACKUP_DIR/backup_*.sql.gz" > /dev/null; then
    latest_backup="$(ls -1t "$BACKUP_DIR"/backup_*.sql.gz | head -n 1)"
    echo "Latest backup file: $latest_backup"
    echo "Latest backup mtime: $(date -r "$latest_backup" '+%Y-%m-%d %H:%M:%S')"
else
    echo "No backup files found in: $BACKUP_DIR"
fi
