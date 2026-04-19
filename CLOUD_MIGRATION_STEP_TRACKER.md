# Cloud Migration Step Tracker

Purpose: Track what is already done and what we will do next, one by one.

Last updated: 2026-04-19 (Step 5 completed)

## Current Status

- Step 1 is complete and validated.
- App is running on cloud DB with safer runtime permissions and operational scripts.
- Step 2 backup and restore drill is complete and validated on a test database.
- Step 3 backup scheduling started: anacron installer script and docs added.
- Step 4 is complete: credential hardening and runtime password rotation drill succeeded.
- Step 5 is complete: incident readiness artifacts created and mini drill executed.

## Step 1 (Completed): Security + Operational Baseline

Status: DONE

What we did in simple terms:
- Switched app DB access from broad/admin-style account to a limited runtime account.
- Added cloud-safe scripts for health checks, security audits, backup, restore, and credential switching.
- Documented the operations flow for repeatable execution.

Why this helps:
- Limits damage if credentials are leaked.
- Makes cloud health/security checks fast and repeatable.
- Reduces manual mistakes during operations.

Validation evidence:
- `./scripts/cloud-db-health-check.sh` passed.
- `./scripts/cloud-security-audit.sh` passed.
- Build verification passed (`ant clean jar`).

---

## Step 2 (Completed): Backup and Restore Drill

Status: DONE

Goal:
- Prove we can recover data quickly and correctly from backup.

Executed actions:
- Ran cloud backup script.
- Restored latest backup into safe target database: dental_clinic_restore_test.
- Verified restored schema and key row counts.
- Recorded issues and compatibility fix.

Validation evidence:
- Backup succeeded: /home/ivylxvie/backups/dental_clinic/backup_20260419_142448.sql.gz
- Restore succeeded to test DB: dental_clinic_restore_test
- Restored schema check: 14 tables
- Restored row counts: patients=8, appointments=7
- Spot-check sample (appointments): latest statuses returned (Declined/Approved/Approved)
- Post-drill runtime checks:
	- ./scripts/cloud-db-health-check.sh passed (TLS enabled, latency reported)
	- ./scripts/cloud-security-audit.sh passed (least privilege OK)

Issues found and resolved:
- Initial backup failed under least-privilege runtime user due FLUSH TABLES privilege requirement.
- Fixed by updating mysqldump flags in scripts/cloud-backup-dental.sh:
	- --skip-lock-tables
	- --no-tablespaces
	- --set-gtid-purged=OFF
- Restore to new test DB initially failed under runtime user (expected, no CREATE DATABASE privilege).
- Performed controlled temporary switch to pre-runtime admin config only for restore drill, then switched back to runtime config.

---

## Step 3 (Planned): Ongoing Operations Schedule

Status: IN PROGRESS

Goal:
- Turn scripts into routine operations.

Planned actions:
- Define daily/weekly schedule for health check, security audit, and backups.
- Define retention and cleanup policy.
- Assign owner and escalation path.

Completed in this step:
- Added `scripts/setup-cloud-backup-anacron.sh` to install/update an idempotent anacron daily backup job for laptops that are not always on.
- Updated README and cloud runbook with install/test/verification commands.

Success criteria:
- Team has a fixed run schedule and clear ownership.

---

## Step 4 (Completed): Secret and Access Hardening

Status: DONE

Goal:
- Improve credential handling and access governance.

Planned actions:
- Rotate runtime DB password on a schedule.
- Limit who can view/update DB credentials.
- Keep admin and runtime accounts strictly separate.

Completed in this step:
- Added `scripts/harden-db-config-perms.sh` and applied it to set `~/.dental_clinic/db.properties` to mode `600`.
- Added `scripts/cloud-rotate-runtime-password.sh` to perform safe runtime password rotation using admin config.
- Updated README and cloud runbook with Step 4 commands and rotation policy.
- Performed live rotation attempt and identified blocker: available admin credential backups no longer authenticate against provider.
- Restored active app config to runtime user (`dentalclinicsystem`) and re-validated health/security checks successfully.
- Hardened rotation script to require explicit `RUNTIME_DB_USER` and prevent targeting admin principal by mistake.
- Completed live runtime rotation drill successfully:
	- Command executed with `RUNTIME_DB_USER=dentalclinicsystem` and admin temp config.
	- DB grants remained least-privilege (USAGE + SELECT/INSERT/UPDATE/DELETE on `dental_clinic_db` only).
	- Health check passed after rotation.
	- Security audit passed after rotation.
	- New config backup generated: `/home/ivylxvie/.dental_clinic/db.properties.bak.20260419_163924`.

Success criteria:
- Rotation runbook exists and is tested.
- Access scope follows least privilege.

---

## Step 5 (Completed): Reliability and Incident Readiness

Status: DONE

Goal:
- Ensure predictable response during outages/slowness.

Planned actions:
- Define first-response checklist for cloud DB incidents.
- Add simple incident log template.
- Run a mini game day simulation.

Completed in this step:
- Added one-command incident first-response collector: `scripts/cloud-incident-snapshot.sh`.
- Added incident log template: `scripts/cloud-incident-log-template.md`.
- Added mini game-day checklist: `scripts/cloud-mini-gameday-checklist.md`.
- Executed a live non-destructive drill snapshot.

Drill evidence:
- Snapshot output file: `/home/ivylxvie/backups/logs/cloud_incident_snapshot_20260419_203922.txt`
- Health status: PASS
- Security status: PASS

Success criteria:
- Team can execute incident steps quickly and consistently.

---

## Notes

- Keep this file updated after each completed step.
- Do not skip validation evidence per step.
