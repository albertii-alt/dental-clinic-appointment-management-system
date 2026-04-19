#!/bin/bash

# Dental Clinic Service Health Check Script
# Checks MySQL + Apache and can auto-run repair tools.
# Usage:
#   ./scripts/mysql-health-check.sh           (health check only)
#   ./scripts/mysql-health-check.sh --repair  (health check + auto-repair)

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Configuration
MYSQL_SOCKET="/opt/lampp/var/mysql/mysql.sock"
MYSQL_CMD="/opt/lampp/bin/mysql --socket=$MYSQL_SOCKET"
MYSQLADMIN_CMD="/opt/lampp/bin/mysqladmin --socket=$MYSQL_SOCKET"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTO_REPAIR=false

if [ "$1" = "--repair" ]; then
    AUTO_REPAIR=true
fi

# Read credentials from db.properties (same source as the Java app)
# This fixes Issues 1 & 2 - connects with proper credentials instead of anonymous
CONFIG_FILE="$HOME/.dental_clinic/db.properties"
if [ ! -f "$CONFIG_FILE" ]; then
    echo "ERROR: Config file not found at $CONFIG_FILE"
    exit 1
fi

DB_USER=$(grep "^db.user" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
DB_PASSWORD=$(grep "^db.password" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
ROOT_PASSWORD=$(grep "^db.root.password" "$CONFIG_FILE" | cut -d'=' -f2 | tr -d '[:space:]')
DB_NAME="dental_clinic_db"

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}   Dental Clinic Health Check${NC}"
echo -e "${YELLOW}=========================================${NC}"
echo ""

MYSQL_HEALTHY=true
APACHE_HEALTHY=true

# Check 1: Is MySQL process running?
echo -n "1. MySQL Process: "
if pgrep -f "/opt/lampp/sbin/mysqld" > /dev/null; then
    echo -e "${GREEN}RUNNING${NC}"
else
    echo -e "${RED}NOT RUNNING${NC}"
    MYSQL_HEALTHY=false
fi

# Check 2: Can we connect?
echo -n "2. MySQL Connection: "
if $MYSQLADMIN_CMD ping &> /dev/null; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    MYSQL_HEALTHY=false
fi

# Check 3: Database exists?
echo -n "3. Database '$DB_NAME': "
if $MYSQL_CMD -u "$DB_USER" -p"$DB_PASSWORD" -e "USE $DB_NAME" &> /dev/null; then
    echo -e "${GREEN}EXISTS${NC}"
else
    echo -e "${RED}MISSING${NC}"
    MYSQL_HEALTHY=false
fi

# Check 4: User exists?
# Uses root credentials since dental_user cannot read mysql.user system table
# Regular app users should never have access to system tables - this is correct security
echo -n "4. User '$DB_USER': "
if [ -z "$ROOT_PASSWORD" ]; then
    ROOT_MYSQL_CMD="/opt/lampp/bin/mysql --socket=$MYSQL_SOCKET -u root"
else
    ROOT_MYSQL_CMD="/opt/lampp/bin/mysql --socket=$MYSQL_SOCKET -u root -p$ROOT_PASSWORD"
fi

if $ROOT_MYSQL_CMD -e \
    "SELECT 1 FROM mysql.user WHERE user='$DB_USER'" 2>/dev/null | grep -q 1; then
    echo -e "${GREEN}EXISTS${NC}"
else
    echo -e "${RED}MISSING${NC}"
    MYSQL_HEALTHY=false
fi

# Check 5: Disk space
echo -n "5. Disk Space: "
AVAILABLE=$(df -h /opt/lampp | awk 'NR==2 {print $4}')
echo -e "${GREEN}$AVAILABLE available${NC}"

# Check 6: Active Connections
# FIXED Issue 2 - now uses proper credentials to run SHOW PROCESSLIST
echo -n "6. Active Connections: "
CONN_COUNT=$($MYSQL_CMD -u "$DB_USER" -p"$DB_PASSWORD" \
    -e "SHOW PROCESSLIST" 2>/dev/null | wc -l)
echo -e "${GREEN}$CONN_COUNT${NC}"

# Check 7: Error Log Size
# FIXED Issue 3 - dynamically finds the .err file instead of hardcoding machine name
echo -n "7. Error Log Size: "
ERR_LOG=$(ls /opt/lampp/var/mysql/*.err 2>/dev/null | head -1)
if [ -n "$ERR_LOG" ]; then
    LOG_SIZE=$(du -h "$ERR_LOG" | cut -f1)
    echo -e "${GREEN}$LOG_SIZE${NC}"
else
    echo -e "${YELLOW}Log file not found${NC}"
fi

# Check 8: Is XAMPP Apache process running?
echo -n "8. Apache Process (XAMPP): "
if pgrep -f "/opt/lampp/bin/httpd" > /dev/null; then
    echo -e "${GREEN}RUNNING${NC}"
else
    echo -e "${RED}NOT RUNNING${NC}"
    APACHE_HEALTHY=false
fi

# Check 9: Port 80 owner
echo -n "9. Port 80 Owner: "
PORT80_OWNER=$(sudo netstat -tulpn 2>/dev/null | awk '/:80 / {print $7}' | head -1)
if [ -z "$PORT80_OWNER" ]; then
    echo -e "${YELLOW}FREE${NC}"
elif echo "$PORT80_OWNER" | grep -q "apache2"; then
    echo -e "${YELLOW}SYSTEM APACHE (${PORT80_OWNER})${NC}"
    APACHE_HEALTHY=false
elif echo "$PORT80_OWNER" | grep -q "httpd"; then
    echo -e "${GREEN}XAMPP APACHE (${PORT80_OWNER})${NC}"
else
    echo -e "${YELLOW}${PORT80_OWNER}${NC}"
fi

echo ""
echo -e "${YELLOW}=========================================${NC}"

if [ "$AUTO_REPAIR" = true ]; then
    echo -e "${YELLOW}Auto-repair mode enabled${NC}"

    if [ "$MYSQL_HEALTHY" = false ]; then
        echo -e "${YELLOW}Running MySQL repair...${NC}"
        "$SCRIPT_DIR/mysql-auto-repair.sh"
    else
        echo -e "${GREEN}MySQL healthy - repair skipped${NC}"
    fi

    if [ "$APACHE_HEALTHY" = false ]; then
        echo -e "${YELLOW}Running Apache repair...${NC}"
        "$SCRIPT_DIR/apache-auto-repair.sh"
    else
        echo -e "${GREEN}Apache healthy - repair skipped${NC}"
    fi
fi
