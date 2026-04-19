#!/usr/bin/env bash

# Capture a first-response incident snapshot for cloud DB operations.
# This script is non-destructive and collects key checks into one report file.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TS="$(date +%Y%m%d_%H%M%S)"
OUT_DIR="${OUT_DIR:-$HOME/backups/logs}"
OUT_FILE="$OUT_DIR/cloud_incident_snapshot_$TS.txt"

mkdir -p "$OUT_DIR"

{
    echo "Cloud Incident Snapshot"
    echo "timestamp=$TS"
    echo "host=$(hostname)"
    echo "pwd=$(pwd)"
    echo

    echo "=== 1) Health Check ==="
    if "$SCRIPT_DIR/cloud-db-health-check.sh"; then
        echo "health_status=PASS"
    else
        echo "health_status=FAIL"
    fi
    echo

    echo "=== 2) Security Audit ==="
    if "$SCRIPT_DIR/cloud-security-audit.sh"; then
        echo "security_status=PASS"
    else
        echo "security_status=FAIL"
    fi
    echo

    echo "=== 3) Backup Status Summary ==="
    if [[ -x "$SCRIPT_DIR/cloud-backup-status.sh" ]]; then
        "$SCRIPT_DIR/cloud-backup-status.sh" || true
    else
        echo "cloud-backup-status.sh not found"
    fi
    echo

    echo "=== 4) Latest Backup Artifact ==="
    latest_backup="$(ls -1t "$HOME"/backups/dental_clinic/backup_*.sql.gz 2>/dev/null | head -n 1 || true)"
    if [[ -n "$latest_backup" ]]; then
        echo "latest_backup=$latest_backup"
        echo "latest_backup_mtime=$(date -r "$latest_backup" '+%Y-%m-%d %H:%M:%S')"
    else
        echo "latest_backup=NONE"
    fi
    echo

    echo "=== 5) Operator Notes ==="
    echo "- Fill incident notes in scripts/cloud-incident-log-template.md"
    echo "- If health or security failed, escalate immediately and avoid destructive actions"
} | tee "$OUT_FILE"

echo
echo "Incident snapshot saved: $OUT_FILE"
