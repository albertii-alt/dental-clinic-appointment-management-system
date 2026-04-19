#!/usr/bin/env bash

# Switch app credentials in ~/.dental_clinic/db.properties to the runtime user.
# Creates a timestamped backup before changing credentials.
# Usage:
#   NEW_DB_USER=dc_app NEW_DB_PASSWORD='strong-pass' ./scripts/cloud-switch-app-user.sh
# Or run interactively:
#   ./scripts/cloud-switch-app-user.sh
# Auto-runs health check and security audit after switch by default.
# Optional opt-out:
#   AUTO_VALIDATE=false ./scripts/cloud-switch-app-user.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${CONFIG_FILE:-$HOME/.dental_clinic/db.properties}"
NEW_DB_USER="${NEW_DB_USER:-}"
NEW_DB_PASSWORD="${NEW_DB_PASSWORD:-}"
AUTO_VALIDATE="${AUTO_VALIDATE:-true}"

if [[ ! -f "$CONFIG_FILE" ]]; then
    echo "ERROR: Config file not found: $CONFIG_FILE"
    exit 1
fi

if [[ -z "$NEW_DB_USER" ]]; then
    read -r -p "Enter database user to activate (e.g. dentalclinicsystem or avnadmin): " NEW_DB_USER
fi

if [[ -z "$NEW_DB_PASSWORD" ]]; then
    read -r -s -p "Enter password for '$NEW_DB_USER': " NEW_DB_PASSWORD
    echo
fi

if [[ -z "$NEW_DB_USER" || -z "$NEW_DB_PASSWORD" ]]; then
    echo "ERROR: Database user and password are required."
    exit 1
fi

if [[ "$NEW_DB_USER" == "avnadmin" ]]; then
    echo "WARNING: You are switching to full-privilege admin user (avnadmin)."
    echo "Use avnadmin only for short maintenance tasks, then switch back to runtime user."
    read -r -p "Type 'YES' to continue: " confirm_admin
    if [[ "$confirm_admin" != "YES" ]]; then
        echo "Cancelled."
        exit 0
    fi
fi

backup="$CONFIG_FILE.bak.$(date +%Y%m%d_%H%M%S)"
cp "$CONFIG_FILE" "$backup"
echo "Backup created: $backup"

echo "Applying credentials for user: $NEW_DB_USER"

tmp_file="$(mktemp)"
awk -v new_user="$NEW_DB_USER" -v new_pass="$NEW_DB_PASSWORD" '
BEGIN { user_done=0; pass_done=0 }
{
    if ($0 ~ /^[[:space:]]*db\.user[[:space:]]*=/) {
        print "db.user=" new_user;
        user_done=1;
    } else if ($0 ~ /^[[:space:]]*db\.password[[:space:]]*=/) {
        print "db.password=" new_pass;
        pass_done=1;
    } else {
        print $0;
    }
}
END {
    if (!user_done) print "db.user=" new_user;
    if (!pass_done) print "db.password=" new_pass;
}
' "$CONFIG_FILE" > "$tmp_file"

mv "$tmp_file" "$CONFIG_FILE"
chmod 600 "$CONFIG_FILE"

echo "Updated app credentials in: $CONFIG_FILE"
if [[ "$AUTO_VALIDATE" == "true" ]]; then
    echo "Running post-switch validation..."
    "$SCRIPT_DIR/cloud-db-health-check.sh"
    "$SCRIPT_DIR/cloud-security-audit.sh"
else
    echo "Auto-validation skipped (AUTO_VALIDATE=$AUTO_VALIDATE)."
    echo "Run: ./scripts/cloud-db-health-check.sh && ./scripts/cloud-security-audit.sh"
fi

if [[ "$NEW_DB_USER" == "avnadmin" ]]; then
    echo "Reminder: You are on avnadmin (full privilege)."
    echo "After maintenance, switch back to dentalclinicsystem immediately."
fi
