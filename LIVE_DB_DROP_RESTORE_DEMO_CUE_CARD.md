# Live DB Drop + Restore Demo Cue Card

Purpose: Present a safe, instructor-facing live demo where the production database is deleted and restored correctly.

## Demo Rules (Say This First)

- We stop writes first to avoid inconsistent restore state.
- We create and verify a fresh backup before any destructive action.
- We use admin DB identity only for drop/create/restore tasks, then switch back to least-privilege runtime identity.

---

## Step 0: Maintenance Mode

Talking point:
- We are entering a short maintenance window to ensure no active writes during recovery operations.

Commands:
```bash
cd /home/ivylxvie/Applications/DentalClinicSystem
pwd
```

Action:
- Stop the running app process before continuing.

---

## Step 1: Pre-Operation Health and Security Check

Talking point:
- We establish a clean baseline before making changes.

Commands:
```bash
./scripts/cloud-db-health-check.sh
./scripts/cloud-security-audit.sh
```

Expected:
- Health check passed.
- Security audit passed.

---

## Step 2: Create and Verify Fresh Backup

Talking point:
- Backup without verification is not enough; we verify the backup artifact integrity.

Commands:
```bash
./scripts/cloud-backup-dental.sh
LATEST_BACKUP="$(ls -1t "$HOME/backups/dental_clinic"/backup_*.sql.gz | head -n 1)"
echo "$LATEST_BACKUP"
gzip -t "$LATEST_BACKUP" && echo "Backup archive OK"
```

Expected:
- Backup success message.
- Latest backup path displayed.
- Backup archive OK displayed.

Hard stop rule:
- If this step fails, do not proceed to delete the database.

---

## Step 3: Preserve Runtime Config and Switch to Admin Config

Talking point:
- Runtime user is intentionally limited; destructive DB operations require temporary admin identity.

Commands:
```bash
cp "$HOME/.dental_clinic/db.properties" "$HOME/.dental_clinic/db.properties.runtime.pre_live_restore.bak"
cp "$HOME/.dental_clinic/db.properties.bak.20260419_140520" "$HOME/.dental_clinic/db.properties"
./scripts/cloud-security-audit.sh
```

Expected:
- Config backup created.
- Admin config active for privileged operations.

---

## Step 4: Destructive Operation (Drop and Recreate Live DB)

Talking point:
- This is the controlled destructive phase; backup is already verified.

Commands:
```bash
source scripts/lib/db-config.sh
load_db_config
MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" \
  --protocol=TCP --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
  ${DB_USE_SSL:+--ssl-mode=REQUIRED} \
  -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\`;"
```

Expected:
- Command completes without SQL errors.

---

## Step 5: Restore Latest Backup into Live DB

Talking point:
- Restore is the real proof that the backup is recoverable.

Commands:
```bash
printf 'YES\n' | ./scripts/cloud-restore-latest.sh
```

Expected:
- Restore completed successfully.

---

## Step 6: Data Validation

Talking point:
- We validate both schema and business data counts, not just command completion.

Commands:
```bash
source scripts/lib/db-config.sh
load_db_config
MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" \
  --protocol=TCP --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
  ${DB_USE_SSL:+--ssl-mode=REQUIRED} \
  -D "$DB_NAME" -e "SHOW TABLES;"

MYSQL_PWD="$DB_PASSWORD" "$MYSQL_BIN" \
  --protocol=TCP --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
  ${DB_USE_SSL:+--ssl-mode=REQUIRED} \
  -D "$DB_NAME" -e "SELECT COUNT(*) AS patients FROM patients; SELECT COUNT(*) AS appointments FROM appointments;"
```

Expected:
- Tables listed.
- Reasonable row counts returned.

---

## Step 7: Return to Runtime Least-Privilege Identity

Talking point:
- We immediately return the app to least-privilege posture after admin tasks.

Commands:
```bash
cp "$HOME/.dental_clinic/db.properties.runtime.pre_live_restore.bak" "$HOME/.dental_clinic/db.properties"
./scripts/cloud-db-health-check.sh
./scripts/cloud-security-audit.sh
```

Expected:
- Health check passed.
- TLS enabled.
- Least-privilege check OK.
- Runtime identity shown as dentalclinicsystem@%.

---

## Step 8: Application Smoke Test

Talking point:
- Recovery is complete only when the app is usable, not only when SQL commands succeed.

Action checklist:
- Start app.
- Login.
- Open dashboard.
- Open patient list.
- Open appointments list.

---

## If Something Fails During Demo

- Stop and report the exact failed step.
- Do not continue to next step until resolved.
- If restore fails after drop/create, rerun Step 5 immediately using the verified latest backup.

---

## 30-Second Closing Script

- We executed a full live recovery workflow: baseline checks, verified backup, controlled DB deletion, successful restore, data validation, and least-privilege re-hardening.
- This demonstrates that our system is recoverable, secure in normal operation, and operationally repeatable.
