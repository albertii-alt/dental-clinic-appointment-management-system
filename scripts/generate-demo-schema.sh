#!/usr/bin/env bash

# ============================================
# Dental Clinic - Generate Clean Demo Schema
# ============================================
# Connects to the MAIN database and exports:
#   - Full schema (no data)
#   - Seed data only: roles, permissions,
#     role_permissions, services, clinic_hours,
#     clinic_schedule, clinic_settings, superAdmin
#
# Output: dental_clinic_schema.sql (project root)
# Usage:
#   ./scripts/generate-demo-schema.sh
#   ADMIN_USER=avnadmin ADMIN_PASSWORD='pass' ./scripts/generate-demo-schema.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=/dev/null
source "$SCRIPT_DIR/lib/db-config.sh"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

OUTPUT_FILE="${OUTPUT_FILE:-$PROJECT_ROOT/dental_clinic_schema.sql}"
ADMIN_USER="${ADMIN_USER:-}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"

# Seed tables — structure + data preserved
SEED_TABLES=(
    roles
    permissions
    role_permissions
    services
    clinic_hours
    clinic_schedule
    clinic_settings
)

# Only superAdmin from staff table
STAFF_FILTER="WHERE username = 'superAdmin'"

echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Dental Clinic - Generate Demo Schema${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Load db config (reads from ~/.dental_clinic/db.properties)
load_db_config

# Use admin credentials if provided, otherwise use configured user
if [[ -n "$ADMIN_USER" ]]; then
    DB_USER="$ADMIN_USER"
    DB_PASSWORD="$ADMIN_PASSWORD"
fi

mapfile -t BASE_ARGS < <(mysql_base_args)
mapfile -t SSL_ARGS < <(mysql_ssl_args)

echo -e "Source database : ${YELLOW}$DB_HOST:$DB_PORT/$DB_NAME${NC}"
echo -e "Output file     : ${YELLOW}$OUTPUT_FILE${NC}"
echo ""

# Test connectivity
echo -n "Testing DB connectivity... "
if ! MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" "${BASE_ARGS[@]}" "${SSL_ARGS[@]}" \
    "$DB_NAME" -Nse "SELECT 1" >/dev/null 2>&1; then
    echo -e "${RED}FAILED${NC}"
    echo "Cannot connect to database. Check credentials."
    exit 1
fi
echo -e "${GREEN}OK${NC}"

# Start output file with header
cat > "$OUTPUT_FILE" <<EOF
-- ============================================
-- Dental Clinic - Clean Demo Schema
-- Generated: $(date '+%Y-%m-%d %H:%M:%S')
-- Source: $DB_HOST/$DB_NAME
-- ============================================
-- This file contains:
--   - Full table structure (no real patient data)
--   - Seed data: roles, permissions, services,
--     clinic config, and superAdmin account only
-- ============================================

SET FOREIGN_KEY_CHECKS=0;
SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';

EOF

# Step 1 — Export schema only (no data)
echo -n "Exporting schema (structure only)... "
MYSQL_PWD="$DB_PASSWORD" "$MYSQLDUMP_BIN" \
    "${BASE_ARGS[@]}" "${SSL_ARGS[@]}" \
    --no-data \
    --single-transaction \
    --skip-lock-tables \
    --routines \
    --triggers \
    --no-tablespaces \
    --set-gtid-purged=OFF \
    --add-drop-table \
    "$DB_NAME" >> "$OUTPUT_FILE" 2>/dev/null
echo -e "${GREEN}OK${NC}"

# Step 2 — Export seed table data
echo -n "Exporting seed data... "
for table in "${SEED_TABLES[@]}"; do
    echo "" >> "$OUTPUT_FILE"
    echo "-- Seed data: $table" >> "$OUTPUT_FILE"
    MYSQL_PWD="$DB_PASSWORD" "$MYSQLDUMP_BIN" \
        "${BASE_ARGS[@]}" "${SSL_ARGS[@]}" \
        --no-create-info \
        --single-transaction \
        --skip-lock-tables \
        --no-tablespaces \
        --set-gtid-purged=OFF \
        --complete-insert \
        "$DB_NAME" "$table" >> "$OUTPUT_FILE" 2>/dev/null
done
echo -e "${GREEN}OK${NC}"

# Step 3 — Export superAdmin only from staff table
echo -n "Exporting superAdmin account... "
echo "" >> "$OUTPUT_FILE"
echo "-- Seed data: staff (superAdmin only)" >> "$OUTPUT_FILE"
MYSQL_PWD="$DB_PASSWORD" "$MYSQLDUMP_BIN" \
    "${BASE_ARGS[@]}" "${SSL_ARGS[@]}" \
    --no-create-info \
    --single-transaction \
    --skip-lock-tables \
    --no-tablespaces \
    --set-gtid-purged=OFF \
    --complete-insert \
    --where="username = 'superAdmin'" \
    "$DB_NAME" staff >> "$OUTPUT_FILE" 2>/dev/null
echo -e "${GREEN}OK${NC}"

# Close footer
cat >> "$OUTPUT_FILE" <<EOF

SET FOREIGN_KEY_CHECKS=1;
-- End of clean demo schema
EOF

SIZE="$(du -h "$OUTPUT_FILE" | cut -f1)"
echo ""
echo -e "${GREEN}✓ Schema generated successfully!${NC}"
echo -e "  File : $OUTPUT_FILE"
echo -e "  Size : $SIZE"
echo ""
echo -e "${YELLOW}To import into demo database:${NC}"
echo "  mysql --protocol=TCP \\"
echo "    -h $DB_HOST -P $DB_PORT \\"
echo "    -u avnadmin -p --ssl-mode=REQUIRED \\"
echo "    dental_clinic_demo < $OUTPUT_FILE"
echo ""
