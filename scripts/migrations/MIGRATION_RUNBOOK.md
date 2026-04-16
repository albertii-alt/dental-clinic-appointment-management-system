# Dental Clinic DB Migration Runbook

This runbook applies the schema hardening plan in safe phases.

## Phase order

1. `00_preflight_audit.sql`
2. `01_additive_backfill.sql`
3. `02_constraints_indexes.sql`
4. Update application code to use normalized columns.
5. `03_cleanup_cutover.sql`

## Pre-migration checklist

1. Take a full backup.
2. Run in staging first.
3. Confirm all app users are on a maintenance window for Phase 2/3.

## Execute Phase 0 (Preflight Audit)

```bash
mysql -u root -p dental_clinic_db < scripts/migrations/00_preflight_audit.sql
```

Proceed to Phase 1 only if all blocker counts are zero:

- `preflight_unmapped_service_type`
- `preflight_unparseable_appointment_time`
- `preflight_unmapped_staff_role`
- `preflight_duplicate_service_names`
- `preflight_duplicate_schedule_days`
- `preflight_duplicate_time_slots`
- `preflight_invalid_appointment_status`
- `preflight_invalid_reset_expiry`
- `preflight_orphan_staff_role_id`

## Execute Phase 1

```bash
mysql -u root -p dental_clinic_db < scripts/migrations/01_additive_backfill.sql
```

Review validation output at end of Phase 1:

- `appointments_missing_service_id` must be 0
- `appointments_missing_time_new` must be 0
- `staff_missing_role_id` must be 0
- `reset_codes_invalid_expiry` must be 0

If any value is not 0, stop and clean data before Phase 2.

## Execute Phase 2

```bash
mysql -u root -p dental_clinic_db < scripts/migrations/02_constraints_indexes.sql
```

## App compatibility window

At this point old and new columns both exist:

- `appointments.service_type` (legacy) and `appointments.service_id` (new)
- `appointments.appointment_time` (legacy text) and `appointments.appointment_time_new` (canonical TIME)
- `staff.role` (legacy enum) and `staff.role_id` (new required FK)

Update Java DAO/service code to read/write new normalized columns before Phase 3.

Phase 3 then removes legacy columns (`service_type`, `appointment_time`, `role`) and keeps normalized columns in place.

## Execute Phase 3

```bash
mysql -u root -p dental_clinic_db < scripts/migrations/03_cleanup_cutover.sql
```

## Rollback guidance

- If Phase 1 fails, fix data and rerun Phase 1.
- If Phase 2 fails, resolve offending data and rerun Phase 2.
- Before Phase 3, take a fresh backup so you can restore quickly if app changes are incomplete.
