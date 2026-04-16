-- Dental Clinic DB migration - Phase 0 (preflight audit)
-- Read-only checks to identify blockers before running migrations.

USE dental_clinic_db;

SELECT 'Preflight started' AS status, NOW() AS audited_at;

-- 1) Service mapping gaps: appointments.service_type that do not map to services.service_name
SELECT COUNT(*) AS preflight_unmapped_service_type
FROM appointments a
LEFT JOIN services s ON s.service_name = a.service_type
WHERE s.service_id IS NULL;

SELECT a.appointment_id, a.service_type
FROM appointments a
LEFT JOIN services s ON s.service_name = a.service_type
WHERE s.service_id IS NULL
ORDER BY a.appointment_id
LIMIT 50;

-- 2) Time parse gaps for values expected like '08:00 AM'
SELECT COUNT(*) AS preflight_unparseable_appointment_time
FROM appointments
WHERE appointment_time IS NULL
   OR TRIM(appointment_time) = ''
   OR STR_TO_DATE(appointment_time, '%h:%i %p') IS NULL;

SELECT appointment_id, appointment_time
FROM appointments
WHERE appointment_time IS NULL
   OR TRIM(appointment_time) = ''
   OR STR_TO_DATE(appointment_time, '%h:%i %p') IS NULL
ORDER BY appointment_id
LIMIT 50;

-- 3) Staff role mapping gaps
SELECT COUNT(*) AS preflight_unmapped_staff_role
FROM staff st
LEFT JOIN roles r ON r.role_name = st.role
WHERE r.role_id IS NULL;

SELECT st.staff_id, st.username, st.role
FROM staff st
LEFT JOIN roles r ON r.role_name = st.role
WHERE r.role_id IS NULL
ORDER BY st.staff_id
LIMIT 50;

-- 4) Duplicate lookup values that would break unique constraints
SELECT COUNT(*) AS preflight_duplicate_service_names
FROM (
  SELECT service_name
  FROM services
  GROUP BY service_name
  HAVING COUNT(*) > 1
) d;

SELECT service_name, COUNT(*) AS duplicate_count
FROM services
GROUP BY service_name
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, service_name
LIMIT 50;

SELECT COUNT(*) AS preflight_duplicate_schedule_days
FROM (
  SELECT day_name
  FROM clinic_schedule
  GROUP BY day_name
  HAVING COUNT(*) > 1
) d;

SELECT day_name, COUNT(*) AS duplicate_count
FROM clinic_schedule
GROUP BY day_name
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, day_name
LIMIT 50;

SELECT COUNT(*) AS preflight_duplicate_time_slots
FROM (
  SELECT time_slot
  FROM clinic_hours
  GROUP BY time_slot
  HAVING COUNT(*) > 1
) d;

SELECT time_slot, COUNT(*) AS duplicate_count
FROM clinic_hours
GROUP BY time_slot
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, time_slot
LIMIT 50;

-- 5) Status values outside target domain for Phase 2
SELECT COUNT(*) AS preflight_invalid_appointment_status
FROM appointments
WHERE status NOT IN ('Pending', 'Approved', 'Declined', 'Rescheduled', 'Completed', 'Cancelled')
   OR status IS NULL;

SELECT appointment_id, status
FROM appointments
WHERE status NOT IN ('Pending', 'Approved', 'Declined', 'Rescheduled', 'Completed', 'Cancelled')
   OR status IS NULL
ORDER BY appointment_id
LIMIT 50;

-- 6) Password reset expiry anomalies
SELECT COUNT(*) AS preflight_invalid_reset_expiry
FROM password_reset_codes
WHERE expires_at IS NULL
   OR expires_at = '0000-00-00 00:00:00'
   OR expires_at <= created_at;

SELECT id, email, created_at, expires_at, used
FROM password_reset_codes
WHERE expires_at IS NULL
   OR expires_at = '0000-00-00 00:00:00'
   OR expires_at <= created_at
ORDER BY id
LIMIT 50;

-- 7) Existing orphan risk for staff.role_id FK alignment
SELECT COUNT(*) AS preflight_orphan_staff_role_id
FROM staff st
LEFT JOIN roles r ON r.role_id = st.role_id
WHERE st.role_id IS NOT NULL
  AND r.role_id IS NULL;

SELECT st.staff_id, st.username, st.role_id
FROM staff st
LEFT JOIN roles r ON r.role_id = st.role_id
WHERE st.role_id IS NOT NULL
  AND r.role_id IS NULL
ORDER BY st.staff_id
LIMIT 50;

SELECT 'Preflight completed' AS status, NOW() AS audited_at;
