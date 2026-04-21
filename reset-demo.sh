#!/bin/bash

# =========================================
# Dental Clinic Demo Reset Script
# =========================================

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
DB_NAME="dental_clinic_db"

# Read app user credentials from db.properties (same source as the Java app)
# This way passwords are never hardcoded in scripts
CONFIG_FILE="$HOME/.dental_clinic/db.properties"
if [ ! -f "$CONFIG_FILE" ]; then
    echo -e "${RED}ERROR: Config file not found at $CONFIG_FILE${NC}"
    echo "Please run the application first to set up the database configuration."
    exit 1
fi

APP_USER=$(grep "^db.user" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
APP_PASSWORD=$(grep "^db.password" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')

# Root user (full permissions for restore)
# NOTE: In production, you should set a root password for MySQL.
# For XAMPP default setup, root has no password.
# To set one: /opt/lampp/bin/mysqladmin -u root password 'yourpassword'
ROOT_USER="root"
ROOT_PASSWORD=""  # Set this if you have configured a root password

MYSQL_SOCKET="/opt/lampp/var/mysql/mysql.sock"
MYSQL_CMD="/opt/lampp/bin/mysql --socket=$MYSQL_SOCKET"
MYSQLDUMP_CMD="/opt/lampp/bin/mysqldump --socket=$MYSQL_SOCKET"

clear
echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   Dental Clinic - Demo Reset Tool${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# Check if MySQL is running
echo -n "Checking MySQL... "
if ! /opt/lampp/bin/mysqladmin --socket=$MYSQL_SOCKET -u $ROOT_USER ping &> /dev/null; then
    echo -e "${RED}FAILED${NC}"
    echo ""
    echo -e "${YELLOW}MySQL is not running. Please start it:${NC}"
    echo "  sudo /opt/lampp/lampp startmysql"
    echo ""
    exit 1
fi
echo -e "${GREEN}OK${NC}"

# Check if original schema exists
ORIGINAL_SCHEMA="$HOME/Applications/DentalClinicSystem/dental_clinic_db.sql"
if [ ! -f "$ORIGINAL_SCHEMA" ]; then
    echo -e "${YELLOW}Warning: Original schema not found at:${NC}"
    echo "  $ORIGINAL_SCHEMA"
    echo ""
fi

# Ask what to restore
echo ""
echo "Choose reset option:"
echo "  ${GREEN}1${NC}) Restore to ORIGINAL CLEAN STATE (fresh install)"
echo "  ${GREEN}2${NC}) Restore to LAST DEMO BACKUP"
echo "  ${GREEN}3${NC}) Create a CLEAN STATE BACKUP (save current as clean)"
echo "  ${GREEN}4${NC}) Cancel"
echo ""
read -p "Enter choice (1-4): " choice

case $choice in
    1)
        echo ""
        echo -e "${YELLOW}Restoring to ORIGINAL CLEAN STATE...${NC}"
        
        if [ -f "$ORIGINAL_SCHEMA" ]; then
            echo "Using: $ORIGINAL_SCHEMA"
            
            # Drop and recreate database
            echo "Dropping database $DB_NAME..."
            $MYSQL_CMD -u $ROOT_USER -e "DROP DATABASE IF EXISTS $DB_NAME;"
            
            echo "Creating fresh database $DB_NAME..."
            $MYSQL_CMD -u $ROOT_USER -e "CREATE DATABASE $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
            
            # Restore from SQL file
            echo "Importing data..."
            $MYSQL_CMD -u $ROOT_USER $DB_NAME < "$ORIGINAL_SCHEMA"
            
            if [ $? -eq 0 ]; then
                echo -e "${GREEN}✓ Restored to original clean state successfully!${NC}"
            else
                echo -e "${RED}✗ Restore failed!${NC}"
                exit 1
            fi
        else
            echo -e "${RED}✗ Original schema file not found!${NC}"
            echo "Looking for: $ORIGINAL_SCHEMA"
            exit 1
        fi
        ;;
    2)
        echo ""
        echo -e "${YELLOW}Restoring to last demo backup...${NC}"
        
        LATEST=$(ls -t ~/backups/dental_clinic/backup_*.sql.gz 2>/dev/null | head -1)
        
        if [ -z "$LATEST" ]; then
            echo -e "${RED}✗ No backups found!${NC}"
            exit 1
        fi
        
        echo "Using: $(basename $LATEST)"
        
        # Drop and recreate database
        echo "Dropping database $DB_NAME..."
        $MYSQL_CMD -u $ROOT_USER -e "DROP DATABASE IF EXISTS $DB_NAME;"
        
        echo "Creating fresh database $DB_NAME..."
        $MYSQL_CMD -u $ROOT_USER -e "CREATE DATABASE $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        
        # Restore from backup
        echo "Importing data..."
        gunzip -c "$LATEST" | $MYSQL_CMD -u $ROOT_USER $DB_NAME
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Restored from demo backup successfully!${NC}"
        else
            echo -e "${RED}✗ Restore failed!${NC}"
            exit 1
        fi
        ;;
    3)
        echo ""
        echo -e "${YELLOW}Creating a clean state backup...${NC}"
        
        CLEAN_BACKUP="$HOME/backups/dental_clinic/clean_state_$(date +%Y%m%d_%H%M%S).sql.gz"
        echo "Saving to: $CLEAN_BACKUP"
        
        # Use app user for backup (has SELECT permissions)
        $MYSQLDUMP_CMD -u $APP_USER -p$APP_PASSWORD \
            --single-transaction \
            --quick \
            --routines \
            --triggers \
            --add-drop-table \
            --complete-insert \
            $DB_NAME | gzip > "$CLEAN_BACKUP"
        
        if [ -f "$CLEAN_BACKUP" ] && [ -s "$CLEAN_BACKUP" ]; then
            SIZE=$(du -h "$CLEAN_BACKUP" | cut -f1)
            echo -e "${GREEN}✓ Clean state backup created! (Size: $SIZE)${NC}"
            ln -sf "$CLEAN_BACKUP" ~/backups/dental_clinic/clean_state_latest.sql.gz
        else
            echo -e "${RED}✗ Backup creation failed!${NC}"
            exit 1
        fi
        ;;
    4)
        echo "Cancelled."
        exit 0
        ;;
    *)
        echo -e "${RED}Invalid choice.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}Reset Complete!${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""
echo "You can now run the application:"
echo -e "${YELLOW}  cd ~/Applications/DentalClinicSystem/dist${NC}"
echo -e "${YELLOW}  java -jar DentalClinicAppointment_ManagementSystem.jar${NC}"
echo ""
echo "Demo Accounts (if using original clean state):"
echo -e "  ${GREEN}Admin:${NC}   superAdmin"
echo -e "  ${GREEN}Staff:${NC}   staff"
echo -e "  ${GREEN}Patient:${NC} alberto"
echo -e "  ${YELLOW}Passwords are stored in your db.properties config file.${NC}"
