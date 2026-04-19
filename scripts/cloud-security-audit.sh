#!/usr/bin/env bash

# Cloud DB security audit for runtime account.
# Checks connection security and inspects grants visible to CURRENT_USER.

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
echo -e "${YELLOW}   Cloud DB Security Audit${NC}"
echo -e "${YELLOW}=========================================${NC}"
echo "Target: $DB_HOST:$DB_PORT / $DB_NAME (runtime user: $DB_USER)"
echo ""

# 1) Verify connectivity.
echo -n "1. Connectivity: "
if MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SELECT 1" >/dev/null 2>&1; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    exit 1
fi

# 2) Verify TLS session status.
echo -n "2. TLS session: "
ssl_cipher="$(MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SHOW SESSION STATUS LIKE 'Ssl_cipher'" 2>/dev/null | awk '{print $2}' || true)"
if [[ -n "$ssl_cipher" ]]; then
    echo -e "${GREEN}ENABLED ($ssl_cipher)${NC}"
else
    if [[ "$DB_USE_SSL" == "true" ]]; then
        echo -e "${RED}EXPECTED BUT NOT ACTIVE${NC}"
    else
        echo -e "${YELLOW}NOT ENFORCED (db.url has no SSL flag)${NC}"
    fi
fi

# 3) Inspect grants available to current user.
echo "3. Runtime account grants:"
grants="$(MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SHOW GRANTS FOR CURRENT_USER" 2>/dev/null || true)"
if [[ -z "$grants" ]]; then
    echo -e "   ${YELLOW}Could not read grants (insufficient visibility).${NC}"
else
    while IFS= read -r line; do
        echo "   - $line"
    done <<< "$grants"
fi

# 4) Flag risky privileges when visible.
echo -n "4. Least-privilege check: "
if [[ -z "$grants" ]]; then
    echo -e "${YELLOW}SKIPPED (grants not visible)${NC}"
else
    risky=0
    if echo "$grants" | grep -Eiq "\b(ALL PRIVILEGES|SUPER|FILE|SHUTDOWN|PROCESS|RELOAD|REPLICATION CLIENT|REPLICATION SLAVE|CREATE USER|GRANT OPTION|DROP|ALTER|CREATE|TRIGGER|EVENT)\b"; then
        risky=1
    fi

    if [[ "$risky" -eq 1 ]]; then
        echo -e "${RED}RISK DETECTED${NC}"
        echo "   Recommendation: runtime app account should usually be limited to SELECT, INSERT, UPDATE, DELETE on $DB_NAME.*"
    else
        echo -e "${GREEN}OK${NC}"
    fi
fi

# 5) Runtime account identity check.
echo -n "5. Current account identity: "
identity="$(MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${MYSQL_BASE_ARGS[@]}" "${MYSQL_SSL_ARGS[@]}" "$DB_NAME" -Nse "SELECT CURRENT_USER()" 2>/dev/null || true)"
if [[ -n "$identity" ]]; then
    echo -e "${GREEN}$identity${NC}"
else
    echo -e "${YELLOW}Unknown${NC}"
fi

echo ""
echo "Audit complete."
