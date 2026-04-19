#!/bin/bash

# Apache Auto-Repair Script
# This script attempts to fix common XAMPP Apache issues automatically
# Exit codes:
#   0 = something was repaired (service recovered)
#   1 = critical failure (Apache could not be started)
#   2 = nothing needed repair (Apache already healthy)

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

LOG_FILE="$HOME/backups/logs/apache-auto-repair.log"

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}   Apache Auto-Repair Tool${NC}"
echo -e "${YELLOW}=========================================${NC}"
echo ""

log_action() {
    mkdir -p "$(dirname "$LOG_FILE")"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

is_xampp_apache_running() {
    pgrep -f "/opt/lampp/bin/httpd" > /dev/null 2>&1
}

is_system_apache_on_80() {
    sudo netstat -tulpn 2>/dev/null | grep -q ":80 .*apache2"
}

REPAIRED=false

# 1) If XAMPP Apache already runs, nothing to do.
if is_xampp_apache_running; then
    echo -e "${GREEN}✓ XAMPP Apache is already running${NC}"
    log_action "Health check passed - Apache already running"
    exit 2
fi

# 2) Detect and stop system Apache if it occupies port 80.
if is_system_apache_on_80; then
    echo -e "${YELLOW}⚠ System Apache is using port 80. Stopping apache2...${NC}"
    log_action "Detected system apache2 on port 80 - stopping service"

    sudo systemctl stop apache2 2>/dev/null || sudo service apache2 stop 2>/dev/null
    sleep 2

    if is_system_apache_on_80; then
        echo -e "${RED}✗ system apache2 is still occupying port 80${NC}"
        log_action "ERROR: Failed to stop system apache2"
        exit 1
    fi

    echo -e "${GREEN}✓ Port 80 is now free${NC}"
    REPAIRED=true
fi

# 3) Kill stale XAMPP httpd processes if any.
if pgrep -f "/opt/lampp/bin/httpd" > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Found stale XAMPP httpd process. Cleaning up...${NC}"
    log_action "Found stale XAMPP httpd process - attempting cleanup"

    sudo pkill -f "/opt/lampp/bin/httpd" 2>/dev/null
    sleep 2

    if pgrep -f "/opt/lampp/bin/httpd" > /dev/null 2>&1; then
        echo -e "${RED}✗ Could not terminate stale Apache process${NC}"
        log_action "ERROR: Could not terminate stale XAMPP httpd process"
        exit 1
    fi

    REPAIRED=true
fi

# 4) Validate Apache configuration before start.
if ! sudo /opt/lampp/bin/httpd -t > /tmp/xampp_httpd_test.out 2>&1; then
    echo -e "${RED}✗ Apache configuration test failed${NC}"
    cat /tmp/xampp_httpd_test.out
    log_action "ERROR: Apache config test failed"
    rm -f /tmp/xampp_httpd_test.out
    exit 1
fi
rm -f /tmp/xampp_httpd_test.out

# 5) Try starting Apache.
echo -e "${YELLOW}Starting XAMPP Apache...${NC}"
log_action "Attempting to start Apache"
sudo /opt/lampp/lampp startapache
sleep 3

if is_xampp_apache_running; then
    echo -e "${GREEN}✓ Apache started successfully${NC}"
    log_action "Apache started successfully"
    REPAIRED=true
else
    echo -e "${RED}✗ Apache failed to start${NC}"
    echo "Recent Apache log entries:"
    tail -20 /opt/lampp/logs/error_log 2>/dev/null
    log_action "CRITICAL: Apache failed to start after repair attempts"
    exit 1
fi

echo ""
if [ "$REPAIRED" = true ]; then
    echo -e "${GREEN}Apache repair complete!${NC}"
    log_action "Repair completed"
    exit 0
else
    echo -e "${GREEN}Apache is healthy - no repair needed.${NC}"
    log_action "Health check passed - no repair needed"
    exit 2
fi
