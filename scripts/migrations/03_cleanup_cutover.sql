-- Dental Clinic DB migration - Phase 3 (application cutover cleanup)
-- Run only after application code has been updated to use:
--   appointments.service_id and appointments.appointment_time_new
-- and no code depends on appointments.service_type or staff.role enum.

USE dental_clinic_db;

START TRANSACTION;

-- 1) Remove denormalized text service field (service_id is now source of truth)
ALTER TABLE appointments
  DROP COLUMN service_type;

-- 2) Remove legacy text time field; keep normalized appointment_time_new as canonical
ALTER TABLE appointments
  DROP COLUMN appointment_time;

-- 3) Remove duplicated role enum in staff (role_id is source of truth)
ALTER TABLE staff
  DROP COLUMN role;

COMMIT;
