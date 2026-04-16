-- Dental Clinic DB migration - Phase 2 (constraints + index hardening)
-- Run only after Phase 1 validation counts are all zero.

USE dental_clinic_db;

START TRANSACTION;

-- 1) Appointments should reference services by key
ALTER TABLE appointments
  ADD INDEX idx_appointments_service_id (service_id);

ALTER TABLE appointments
  ADD CONSTRAINT fk_appointments_service
  FOREIGN KEY (service_id) REFERENCES services(service_id)
  ON UPDATE CASCADE
  ON DELETE RESTRICT;

-- 2) Require normalized role FK for staff
ALTER TABLE staff
  MODIFY role_id INT NOT NULL;

-- 3) Enforce normalized time column
ALTER TABLE appointments
  MODIFY appointment_time_new TIME NOT NULL;

-- 4) Uniqueness on key lookup values
ALTER TABLE clinic_schedule
  ADD CONSTRAINT uq_clinic_schedule_day_name UNIQUE (day_name);

ALTER TABLE clinic_hours
  ADD CONSTRAINT uq_clinic_hours_time_slot UNIQUE (time_slot);

ALTER TABLE services
  ADD CONSTRAINT uq_services_service_name UNIQUE (service_name);

-- 5) Enforce appointment status domain (includes existing values)
ALTER TABLE appointments
  MODIFY status ENUM('Pending', 'Approved', 'Declined', 'Rescheduled', 'Completed', 'Cancelled')
  NOT NULL DEFAULT 'Pending';

-- 6) Tighten password reset code expiration rules
ALTER TABLE password_reset_codes
  MODIFY expires_at TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL 15 MINUTE);

ALTER TABLE password_reset_codes
  ADD CONSTRAINT chk_password_reset_expiry CHECK (expires_at > created_at);

ALTER TABLE password_reset_codes
  ADD INDEX idx_password_reset_active (email, username, used, expires_at);

COMMIT;
