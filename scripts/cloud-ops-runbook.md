# Cloud Operations Runbook (Step-by-Step)

## Step 1: Validate cloud connectivity and TLS

- Run: `./scripts/cloud-db-health-check.sh`
- Expected:
  - MySQL ping OK
  - Query OK
  - Active database matches app DB
  - TLS session enabled

## Step 2: Audit runtime account privileges

- Run: `./scripts/cloud-security-audit.sh`
- If output shows `RISK DETECTED`, continue to Step 3.

## Step 3: Create least-privileged runtime user

- Run:
  - `RUNTIME_DB_USER=dc_app RUNTIME_DB_PASSWORD='strong-pass' ./scripts/cloud-create-runtime-user.sh`
- Defaults:
  - Host: `%`
  - Privileges: `SELECT,INSERT,UPDATE,DELETE`
  - SSL required for the runtime user

## Step 4: Switch app config to runtime user

- Run:
  - `NEW_DB_USER=dc_app NEW_DB_PASSWORD='strong-pass' ./scripts/cloud-switch-app-user.sh`
- The script creates a timestamped backup of `~/.dental_clinic/db.properties`.

## Step 5: Re-validate app DB access

- Run:
  - `./scripts/cloud-db-health-check.sh`
  - `./scripts/cloud-security-audit.sh`
- Confirm:
  - Health check passes
  - Security audit no longer reports broad global grants for runtime principal

## Step 6: Backup and restore drill

- Backup:
  - `./scripts/cloud-backup-dental.sh`
- Restore (confirmation required):
  - `./scripts/cloud-restore-latest.sh`

## Step 7: Enable automatic daily backup for laptops (anacron)

- Install or update job (idempotent):
  - `sudo ./scripts/setup-cloud-backup-anacron.sh`
- Optional tuning:
  - `sudo ANACRON_DELAY=5 ./scripts/setup-cloud-backup-anacron.sh`
- Force-run test now:
  - `sudo anacron -fn`
- Verify log output:
  - `tail -n 50 ~/backups/logs/cloud_backup_anacron.log`
- Verify latest status summary:
  - `./scripts/cloud-backup-status.sh`

Expected:
- An anacron job exists in `/etc/anacrontab` for `dental-clinic-cloud-backup`.
- Missed daily backups run after the machine powers on.
- Last status file shows SUCCESS/FAILED with latest backup path.

## Step 8: Secret and access hardening (runtime credentials)

- Harden local credential file permissions:
  - `./scripts/harden-db-config-perms.sh`
- Rotate runtime password using admin config:
  - `RUNTIME_DB_USER=dentalclinicsystem ADMIN_CONFIG_FILE=~/.dental_clinic/db.properties.bak.YYYYMMDD_HHMMSS NEW_RUNTIME_PASSWORD='new-strong-password' ./scripts/cloud-rotate-runtime-password.sh`
- Re-validate runtime access:
  - `./scripts/cloud-db-health-check.sh`
  - `./scripts/cloud-security-audit.sh`

Expected:
- `~/.dental_clinic/db.properties` is owner-only (`600`).
- Runtime password is rotated without broadening runtime privileges.
- Health and security checks pass after rotation.

## Operational notes

- Keep admin-level principal (for schema/admin work) separate from runtime app principal.
- Rotate runtime credentials regularly.
- Restrict network ACL/allowlist at the provider side.
- Keep at least one backup copy outside the DB provider when possible.

## Step 9: Incident readiness and mini game-day

- Run first-response snapshot:
  - `./scripts/cloud-incident-snapshot.sh`
- Fill incident log template:
  - `scripts/cloud-incident-log-template.md`
- Run mini game-day checklist:
  - `scripts/cloud-mini-gameday-checklist.md`

Expected:
- Snapshot file saved under `~/backups/logs/cloud_incident_snapshot_*.txt`.
- Team can execute first-response checks in one command.
- Incident notes and one improvement action are recorded.
