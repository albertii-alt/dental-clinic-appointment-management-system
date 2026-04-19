#!/usr/bin/env bash

# Harden local DB config file permissions.
# Ensures only the app user can read/write db.properties.

set -euo pipefail

CONFIG_FILE="${CONFIG_FILE:-$HOME/.dental_clinic/db.properties}"
APP_USER="${APP_USER:-$USER}"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "ERROR: Config file not found: $CONFIG_FILE"
    exit 1
fi

if ! id "$APP_USER" >/dev/null 2>&1; then
    echo "ERROR: APP_USER does not exist: $APP_USER"
    exit 1
fi

if [[ "$(id -u)" -eq 0 ]]; then
    chown "$APP_USER":"$APP_USER" "$CONFIG_FILE"
else
    # Non-root mode: only allow owner changes if current user owns file.
    owner="$(stat -c '%U' "$CONFIG_FILE")"
    if [[ "$owner" != "$APP_USER" ]]; then
        echo "ERROR: $CONFIG_FILE is owned by $owner."
        echo "Run with sudo to change ownership: sudo APP_USER=$APP_USER $0"
        exit 1
    fi
fi

chmod 600 "$CONFIG_FILE"

echo "Hardened: $CONFIG_FILE"
echo "Owner: $(stat -c '%U:%G' "$CONFIG_FILE")"
echo "Mode : $(stat -c '%a' "$CONFIG_FILE")"
