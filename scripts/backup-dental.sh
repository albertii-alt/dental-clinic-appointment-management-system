#!/bin/bash

# =========================================
# Dental Clinic Database Backup Script
# =========================================

# Configuration
BACKUP_DIR="$HOME/backups/dental_clinic"
LOG_DIR="$HOME/backups/logs"
DATE=$(date +%Y%m%d_%H%M%S)
LOG_FILE="$LOG_DIR/backup_$DATE.log"
DB_NAME="dental_clinic_db"
KEEP_DAYS=30

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

# XAMPP MySQL paths with socket
MYSQL_CMD="/opt/lampp/bin/mysql --socket=/opt/lampp/var/mysql/mysql.sock"
MYSQLDUMP_CMD="/opt/lampp/bin/mysqldump --socket=/opt/lampp/var/mysql/mysql.sock"

# Create directories
mkdir -p "$BACKUP_DIR"
mkdir -p "$LOG_DIR"

# Start logging
echo "=========================================" > "$LOG_FILE"
echo "Dental Clinic Backup - $DATE" >> "$LOG_FILE"
echo "=========================================" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

# Function to log messages
log_message() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log_message "Starting database backup..."

# Test database connection
log_message "Testing database connection..."
if $MYSQL_CMD -u "$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1" "$DB_NAME" &> /dev/null; then
    log_message "Database connection successful"
else
    log_message "ERROR: Cannot connect to database"
    exit 1
fi

# Perform backup
BACKUP_FILE="$BACKUP_DIR/backup_$DATE.sql"
log_message "Creating backup: $(basename "$BACKUP_FILE")"

# Run mysqldump and capture output
$MYSQLDUMP_CMD -u "$DB_USER" -p"$DB_PASSWORD" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    --add-drop-table \
    --complete-insert \
    "$DB_NAME" > "$BACKUP_FILE" 2>> "$LOG_FILE"

# VERIFICATION CHECK 1: Did mysqldump exit without errors?
# mysqldump returns 0 on success, non-zero on failure
DUMP_EXIT_CODE=$?
if [ $DUMP_EXIT_CODE -ne 0 ]; then
    log_message "ERROR: mysqldump failed with exit code $DUMP_EXIT_CODE"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# Check if backup was created
if [ -f "$BACKUP_FILE" ] && [ -s "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    log_message "Backup file created (Size: $SIZE)"

    # VERIFICATION CHECK 2: Does the SQL file contain valid content?
    # Every valid mysqldump ends with '-- Dump completed on ...'
    # If this line is missing the backup is incomplete or corrupted
    if ! grep -q "Dump completed" "$BACKUP_FILE"; then
        log_message "ERROR: Backup file is incomplete - missing completion marker!"
        log_message "This usually means mysqldump was interrupted (e.g. disk full)"
        rm -f "$BACKUP_FILE"
        exit 1
    fi
    log_message "Verification passed: SQL content is valid"

    # Compress backup
    log_message "Compressing backup..."
    gzip "$BACKUP_FILE"

    # VERIFICATION CHECK 3: Is the compressed file valid?
    # gzip -t tests the integrity of the compressed file
    if ! gzip -t "$BACKUP_FILE.gz" 2>/dev/null; then
        log_message "ERROR: Compressed backup file is corrupted!"
        rm -f "$BACKUP_FILE.gz"
        exit 1
    fi

    SIZE_COMPRESSED=$(du -h "$BACKUP_FILE.gz" | cut -f1)
    log_message "Verification passed: Compressed file is valid (Size: $SIZE_COMPRESSED)"
    log_message "Backup created successfully!"
else
    log_message "ERROR: Backup file is empty or was not created!"
    exit 1
fi

# Clean old backups
log_message "Cleaning backups older than $KEEP_DAYS days..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +$KEEP_DAYS -delete
find "$LOG_DIR" -name "backup_*.log" -mtime +$KEEP_DAYS -delete
log_message "Old backups and logs cleaned"

# Count total backups
TOTAL_BACKUPS=$(ls -1 "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null | wc -l)
log_message "Total backups in storage: $TOTAL_BACKUPS"

log_message "========================================="
log_message "Backup completed successfully!"
log_message "Backup location: $BACKUP_DIR"
log_message "Log file: $LOG_FILE"

exit 0
