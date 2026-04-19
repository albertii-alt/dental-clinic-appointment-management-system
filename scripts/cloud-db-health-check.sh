#!/usr/bin/env bash

# Cloud-safe DB health check.
# Does not assume local XAMPP services or local sockets.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/lib/db-config.sh"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

load_db_config

mapfile -t MYSQL_BASE_ARGS < <(mysql_base_args)
mapfile -t MYSQL_SSL_ARGS < <(mysql_ssl_args)

echo -e "${YELLOW}=========================================${NC}"
echo -e "${YELLOW}   Cloud DB Health Check${NC}"
echo -e "${YELLOW}=========================================${NC}"
echo "Target: $DB_HOST:$DB_PORT / $DB_NAME (user: $DB_USER)"
echo ""

check_ok=true

# 1) mysqladmin ping
echo -n "1. MySQL Ping: "
if MYSQL_PWD="$DB_PASSWORD" "$MYSQLADMIN_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" ping >/dev/null 2>&1; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    check_ok=false
fi

# 2) Basic query
echo -n "2. Query Test (SELECT 1): "
if MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SELECT 1" >/dev/null 2>&1; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    check_ok=false
fi

# 3) DB context
echo -n "3. Active Database: "
active_db="$(MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SELECT DATABASE()" 2>/dev/null || true)"
if [[ "$active_db" == "$DB_NAME" ]]; then
    echo -e "${GREEN}$active_db${NC}"
else
    echo -e "${RED}Unexpected ($active_db)${NC}"
    check_ok=false
fi

# 4) TLS check
echo -n "4. TLS Session: "
ssl_cipher="$(MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SHOW SESSION STATUS LIKE 'Ssl_cipher'" 2>/dev/null | awk '{print $2}' || true)"
if [[ -n "$ssl_cipher" ]]; then
    echo -e "${GREEN}ENABLED ($ssl_cipher)${NC}"
else
    if [[ "$DB_USE_SSL" == "true" ]]; then
        echo -e "${RED}EXPECTED BUT NOT ACTIVE${NC}"
        check_ok=false
    else
        echo -e "${YELLOW}NOT ENFORCED (db.url has no SSL flag)${NC}"
    fi
fi

# 5) Round-trip latency snapshot
echo -n "5. Round-trip latency: "
start_ms="$(date +%s%3N)"
if MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SELECT 1" >/dev/null 2>&1; then
    end_ms="$(date +%s%3N)"
    echo -e "${GREEN}$((end_ms - start_ms)) ms${NC}"
else
    echo -e "${RED}FAILED${NC}"
    check_ok=false
fi

echo ""
if [[ "$check_ok" == "true" ]]; then
    echo -e "${GREEN}Cloud DB health check passed.${NC}"
    exit 0
fi

echo -e "${RED}Cloud DB health check failed.${NC}"
exit 1
