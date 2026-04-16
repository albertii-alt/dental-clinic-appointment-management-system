-- Dental Clinic DB migration - Phase 1 (safe additive + backfill)
-- Target: MariaDB/MySQL
-- Goal: Add new schema elements without breaking existing code.

USE dental_clinic_db;

START TRANSACTION;

-- 1) Introduce service_id on appointments (keep existing service_type for compatibility)
ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS service_id INT NULL AFTER patient_id;

-- Backfill service_id from service_type text
UPDATE appointments a
JOIN services s ON s.service_name = a.service_type
SET a.service_id = s.service_id
WHERE a.service_id IS NULL;

-- 2) Keep legacy text time, add normalized TIME column
ALTER TABLE appointments
  ADD COLUMN IF NOT EXISTS appointment_time_new TIME NULL AFTER appointment_time;

-- Backfill normalized time from values like '08:00 AM'
UPDATE appointments
SET appointment_time_new = TIME(STR_TO_DATE(appointment_time, '%h:%i %p'))
WHERE appointment_time_new IS NULL
  AND appointment_time IS NOT NULL
  AND appointment_time <> '';

-- 3) Normalize staff role references using roles table
UPDATE staff s
JOIN roles r ON r.role_name = s.role
SET s.role_id = r.role_id
WHERE s.role_id IS NULL;

-- 4) Repair password reset expiry anomalies
UPDATE password_reset_codes
SET expires_at = DATE_ADD(created_at, INTERVAL 15 MINUTE)
WHERE expires_at IS NULL
   OR expires_at = '0000-00-00 00:00:00'
   OR expires_at <= created_at;

-- 5) Add standard audit timestamps (non-breaking)
ALTER TABLE services
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE clinic_hours
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE clinic_schedule
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE roles
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE permissions
  ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

COMMIT;

-- Validation checks (run after COMMIT)
-- 1) Any appointments with unmatched service?
SELECT COUNT(*) AS appointments_missing_service_id
FROM appointments
WHERE service_id IS NULL;

-- 2) Any appointments with unparseable time?
SELECT COUNT(*) AS appointments_missing_time_new
FROM appointments
WHERE appointment_time_new IS NULL;

-- 3) Any staff without role_id?
SELECT COUNT(*) AS staff_missing_role_id
FROM staff
WHERE role_id IS NULL;

-- 4) Any invalid reset code expiry rows left?
SELECT COUNT(*) AS reset_codes_invalid_expiry
FROM password_reset_codes
WHERE expires_at <= created_at;
