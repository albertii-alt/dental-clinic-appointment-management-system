#!/bin/bash

# MySQL Auto-Repair Script
# This script attempts to fix common MySQL issues automatically
# Exit codes:
#   0 = something was repaired (triggers backup in cron)
#   1 = critical failure (MySQL could not be started)
#   2 = nothing needed repair (backup not needed)

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}   MySQL Auto-Repair Tool${NC}"
echo -e "${YELLOW}=========================================${NC}"
echo ""

# Read credentials from db.properties (same source as the Java app)
CONFIG_FILE="$HOME/.dental_clinic/db.properties"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Config file not found at $CONFIG_FILE"
    exit 1
fi

DB_USER=$(grep "^db.user" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
DB_PASSWORD=$(grep "^db.password" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
ROOT_PASSWORD=$(grep "^db.root.password" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
DB_NAME="dental_clinic_db"

# Build root mysql command based on whether root has a password or not
# FIXED Issue 4 - handles root password properly instead of always assuming no password
if [ -z "$ROOT_PASSWORD" ]; then
    ROOT_MYSQL="/opt/lampp/bin/mysql --socket=/opt/lampp/var/mysql/mysql.sock -u root"
else
    ROOT_MYSQL="/opt/lampp/bin/mysql --socket=/opt/lampp/var/mysql/mysql.sock -u root -p$ROOT_PASSWORD"
fi

# Flag to track if anything was actually repaired
REPAIRED=false

# Function to log actions
log_action() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> ~/backups/logs/mysql-auto-repair.log
}

# Check if MySQL is running
if pgrep -f "/opt/lampp/sbin/mysqld" > /dev/null; then
    echo -e "${GREEN}✓ MySQL is running${NC}"
else
    echo -e "${YELLOW}✗ MySQL is not running. Attempting to start...${NC}"
    log_action "MySQL not running, attempting to start"

    sudo /opt/lampp/lampp startmysql
    sleep 3

    if pgrep -f "/opt/lampp/sbin/mysqld" > /dev/null; then
        echo -e "${GREEN}✓ MySQL started successfully${NC}"
        log_action "MySQL started successfully"
        REPAIRED=true
    else
        echo -e "${RED}✗ Failed to start MySQL. Attempting repair...${NC}"

        # Kill orphaned processes
        sudo killall mysqld mysqld_safe 2>/dev/null
        sleep 2

        # Try starting again
        sudo /opt/lampp/lampp startmysql
        sleep 3

        if pgrep -f "/opt/lampp/sbin/mysqld" > /dev/null; then
            echo -e "${GREEN}✓ MySQL started after cleanup${NC}"
            log_action "MySQL started after process cleanup"
            REPAIRED=true
        else
            echo -e "${RED}✗ Still cannot start MySQL. Please check manually.${NC}"
            log_action "CRITICAL: Cannot start MySQL"
            exit 1
        fi
    fi
fi

# Check if we can connect
if /opt/lampp/bin/mysqladmin ping --socket=/opt/lampp/var/mysql/mysql.sock &> /dev/null; then
    echo -e "${GREEN}✓ MySQL connection OK${NC}"
else
    echo -e "${RED}✗ Cannot connect to MySQL${NC}"
    log_action "Cannot connect to MySQL"
    exit 1
fi

# Check if database exists
if ! $ROOT_MYSQL -e "USE $DB_NAME" &> /dev/null; then
    echo -e "${YELLOW}⚠ Database '$DB_NAME' missing. Restoring from backup...${NC}"
    log_action "Database missing, attempting restore"

    # Find latest backup
    LATEST_BACKUP=$(ls -t ~/backups/dental_clinic/backup_*.sql.gz 2>/dev/null | head -1)

    if [ -n "$LATEST_BACKUP" ]; then
        # Create database
        $ROOT_MYSQL -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"

        # Restore from backup and capture exit code
        gunzip -c "$LATEST_BACKUP" | $ROOT_MYSQL $DB_NAME
        RESTORE_EXIT=$?

        # FIXED Issue 4 - verify restore actually succeeded instead of blindly reporting success
        if [ $RESTORE_EXIT -eq 0 ]; then
            echo -e "${GREEN}✓ Database restored from backup${NC}"
            log_action "Database restored from $LATEST_BACKUP"
            REPAIRED=true
        else
            echo -e "${RED}✗ Restore failed with exit code $RESTORE_EXIT${NC}"
            log_action "ERROR: Restore failed with exit code $RESTORE_EXIT - check root password in db.properties"
        fi
    else
        echo -e "${RED}✗ No backup found! Cannot restore.${NC}"
        log_action "No backup found for database restoration"
    fi
fi

echo ""

# Exit with correct code based on whether anything was repaired
if [ "$REPAIRED" = true ]; then
    echo -e "${GREEN}MySQL repair complete! Backup will now run.${NC}"
    log_action "Repair completed - triggering backup"
    exit 0
else
    echo -e "${GREEN}MySQL is healthy - no repair needed.${NC}"
    log_action "Health check passed - no repair needed"
    exit 2
fi
