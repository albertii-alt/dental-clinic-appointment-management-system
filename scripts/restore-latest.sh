#!/bin/bash

# Read credentials from db.properties (same source as the Java app)
# This way passwords are never hardcoded in scripts
CONFIG_FILE="$HOME/.dental_clinic/db.properties"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Config file not found at $CONFIG_FILE"
    echo "Please run the application first to set up the database configuration."
    exit 1
fi

DB_USER=$(grep "^db.user" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
DB_PASSWORD=$(grep "^db.password" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
DB_NAME="dental_clinic_db"

# Find the latest backup
LATEST=$(ls -t ~/backups/dental_clinic/backup_*.sql.gz | head -1)

if [ -z "$LATEST" ]; then
    echo "No backup found!"
    exit 1
fi

echo "Restoring from: $LATEST"
echo "WARNING: This will overwrite your current database!"
read -p "Type 'YES' to continue: " confirm

if [ "$confirm" = "YES" ]; then
    gunzip -c "$LATEST" | /opt/lampp/bin/mysql --socket=/opt/lampp/var/mysql/mysql.sock -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME"
    echo "Restore completed!"
else
    echo "Restore cancelled."
fi
