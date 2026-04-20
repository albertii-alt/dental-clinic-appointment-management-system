# 🦷 Dental Clinic Appointment Management System

A Java-based desktop application for managing dental clinic operations — patient records, appointment scheduling, and role-based access control.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/language-Java-orange.svg)
![Database](https://img.shields.io/badge/database-MySQL-blue.svg)

---

## 🚀 Features

### 👤 Role-Based Access Control
| Role | Capabilities |
|------|-------------|
| **Admin** | Full system management, user accounts, clinic settings, audit logs |
| **Dentist** | Clinical management, medical history, schedule coordination |
| **Staff** | Patient registration, appointment handling, receptionist duties |
| **Patient** | Self-registration, appointment requests, view history |

### 📅 Appointment Management
- Schedule, approve, decline, and reschedule appointments
- Status tracking: `Pending → Approved / Declined / Rescheduled`
- Automated age calculation and visit contact tracking

### 📋 Patient & Clinical Records
- Comprehensive patient profiles and medical history
- Clinical notes restricted to authorized medical staff

### ⚙️ Clinic Coordination
- Block/unblock time slots for breaks or maintenance
- Configure operating hours and appointment lead times
- Detailed audit trails for all user actions

---

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java (JDK 8+) |
| UI | Java Swing |
| Database | MySQL / MariaDB via JDBC |
| Build | Apache Ant |
| Runtime | XAMPP (MySQL via `/opt/lampp`) |

---

## 📦 Installation & Setup

### Prerequisites
- Java Development Kit (JDK) 8+
- XAMPP (for MySQL via `/opt/lampp`) or any MySQL server

### 1. Database Setup
1. Start your MySQL server.
2. Create a database named `dental_clinic_db`.
3. Import the schema:
   ```bash
   mysql -u root -p dental_clinic_db < dental_clinic_db.sql
   ```
4. Create the application user:
   ```sql
   CREATE USER 'dental_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON dental_clinic_db.* TO 'dental_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

### 2. Database Configuration
On first launch, a **Database Setup Wizard** will appear automatically if no configuration is found.

Alternatively, create the config file manually at `~/.dental_clinic/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/dental_clinic_db?useSSL=true&serverTimezone=UTC
db.user=dental_user
db.password=your_password
```

You can also use environment variables instead:
```bash
export DB_URL=jdbc:mysql://localhost:3306/dental_clinic_db?useSSL=true&serverTimezone=UTC
export DB_USER=dental_user
export DB_PASSWORD=your_password
```

### 3. Build & Run

**Linux (with XAMPP):**
```bash
chmod +x run.sh
./run.sh
```
The script will:
- Prompt for your `sudo` password to start XAMPP
- Auto-start MySQL and verify the database exists
- Restore from backup (`~/backups/dental_clinic/`) if the database is missing
- Launch the application JAR

**Manual launch (if MySQL is already running):**
```bash
java -jar dist/DentalClinicAppointment_ManagementSystem.jar
```

### 3.1 Build Windows Installer

On a Windows machine with JDK 21 (with `jpackage`) and Ant installed:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows\build-desktop-installer.ps1 -InstallerType exe -AppVersion 1.0.0
```

Installer output:

```text
build\windows-installer\output
```

CI automation (GitHub Actions):

- Workflow file: `.github/workflows/windows-installer.yml`
- Manual run: Actions -> `Build Windows Installer` -> Run workflow
- Tag run: push tag like `v1.0.0` to auto-build and upload installer artifact

### 4. Service Health Check & Auto-Repair

Use the health-check script to verify both MySQL and Apache:

```bash
./scripts/mysql-health-check.sh
```

Run with auto-repair enabled to trigger repair tools only when needed:

```bash
./scripts/mysql-health-check.sh --repair
```

Direct repair tools:

```bash
./scripts/mysql-auto-repair.sh
./scripts/apache-auto-repair.sh
```

### 5. Cloud-Safe DB Operations (No Local XAMPP Assumptions)

When running against a managed cloud MySQL provider (Aiven, RDS, etc.), use these scripts:

```bash
./scripts/cloud-db-health-check.sh
./scripts/cloud-backup-dental.sh
./scripts/cloud-restore-latest.sh
./scripts/cloud-security-audit.sh
RUNTIME_DB_USER=dc_app RUNTIME_DB_PASSWORD='strong-pass' ./scripts/cloud-create-runtime-user.sh
NEW_DB_USER=dc_app NEW_DB_PASSWORD='strong-pass' ./scripts/cloud-switch-app-user.sh
```

Notes:
- These scripts read `db.url`, `db.user`, and `db.password` from `~/.dental_clinic/db.properties`.
- They use standard `mysql`, `mysqldump`, and `mysqladmin` clients from `PATH` (with `/opt/lampp/bin` fallback).
- `cloud-db-health-check.sh` verifies connectivity, active database, TLS session (when SSL is enabled in `db.url`), and round-trip latency.
- `cloud-backup-dental.sh` includes dump integrity checks and retention cleanup.
- `cloud-restore-latest.sh` restores the newest backup after explicit confirmation.
- `cloud-security-audit.sh` inspects runtime user grants and flags risky privilege patterns.
- `cloud-create-runtime-user.sh` provisions a least-privileged runtime account with SSL requirement.
- `cloud-switch-app-user.sh` updates `~/.dental_clinic/db.properties` runtime credentials with automatic backup.

Automatic backups for laptops (anacron):

```bash
sudo ./scripts/setup-cloud-backup-anacron.sh
```

What this configures:
- Creates a runner at `/usr/local/bin/dental-clinic-cloud-backup`.
- Adds/updates one idempotent entry in `/etc/anacrontab`.
- Runs `cloud-backup-dental.sh` daily even if the laptop was off at the scheduled hour (anacron executes missed jobs after boot).

Optional tuning examples:

```bash
sudo ANACRON_DELAY=5 ./scripts/setup-cloud-backup-anacron.sh
sudo APP_USER=$USER ANACRON_PERIOD=1 ANACRON_DELAY=10 ./scripts/setup-cloud-backup-anacron.sh
```

Test and verify:

```bash
sudo anacron -fn
tail -n 50 ~/backups/logs/cloud_backup_anacron.log
./scripts/cloud-backup-status.sh
```

Backup assurance signals:
- Status file: `~/backups/logs/cloud_backup_last_status.txt`
- Scheduler log: `~/backups/logs/cloud_backup_anacron.log`
- Optional desktop notification (best effort): shown as "Dental Clinic Backup" when graphical session is available.
- System log tag: `dental-clinic-cloud-backup`

Step 4 - secret and access hardening:

```bash
./scripts/harden-db-config-perms.sh
ADMIN_CONFIG_FILE=~/.dental_clinic/db.properties.bak.YYYYMMDD_HHMMSS NEW_RUNTIME_PASSWORD='new-strong-password' ./scripts/cloud-rotate-runtime-password.sh
./scripts/cloud-db-health-check.sh
./scripts/cloud-security-audit.sh
```

Notes:
- `harden-db-config-perms.sh` enforces owner-only access (`chmod 600`) on `~/.dental_clinic/db.properties`.
- `cloud-rotate-runtime-password.sh` rotates runtime user password in DB (using admin config) and updates local app config safely with backup.
- Set `RUNTIME_DB_USER` explicitly when rotating (example: `RUNTIME_DB_USER=dentalclinicsystem`).
- Recommended rotation policy: every 30 to 60 days, and immediately after suspected credential exposure.

For a strict one-by-one production checklist, use:

```bash
cat scripts/cloud-ops-runbook.md
```

### Why Step 1 Helps (Before vs After)

| Area | Before | After | Why This Matters |
|------|--------|-------|------------------|
| Runtime DB account scope | App could run with broad/admin-style privileges | App runs with least-privileged runtime account (`SELECT/INSERT/UPDATE/DELETE` on `dental_clinic_db.*`) | Reduces damage if credentials are leaked or app logic misbehaves |
| Privilege verification | Security posture was assumed manually | `cloud-security-audit.sh` reports actual grants and risky patterns | You can prove security posture with repeatable checks |
| Cloud connectivity confidence | App connection status checked ad hoc | `cloud-db-health-check.sh` validates DB reachability, active DB, TLS, and latency | Faster troubleshooting and fewer "works on my machine" issues |
| Credential changes | Manual edits with higher typo risk | `cloud-switch-app-user.sh` updates config and creates timestamped backup | Safer credential rotation with rollback path |
| Cloud operations consistency | Local/XAMPP assumptions could leak into cloud workflow | Cloud scripts use JDBC config and standard MySQL clients | Lower operational mistakes in managed MySQL environments |

Expected day-to-day impact:
- Better uptime because failures are detected early with consistent health checks.
- Better security because the app identity is intentionally limited.
- Better recoverability because config changes and backups are scripted, not improvised.
- Better team handoff because operations are documented and repeatable.

---

## 🖥️ Usage

1. **Login** — Use the default `superAdmin` credentials on first launch. Change them immediately.
2. **Dashboard** — Navigate based on your assigned role.
3. **Patient Registration** — Patients can self-register from the login screen.

---

## 📁 Project Structure

```
src/com/dentalclinic/
├── admin/       # Admin panels (users, roles, audit, settings)
├── dao/         # Data access objects (DB queries)
├── main/        # Application entry point
├── model/       # Data models (Appointment, Patient)
├── patient/     # Patient-facing panels
├── service/     # Business logic layer
├── staff/       # Staff-facing panels
├── ui/          # Dashboards, login, shared UI components
└── util/        # DB connection, auth, email, sanitization
```

---

## 🔒 Security Notes

- **Passwords** — The default schema uses plaintext for demonstration. Use `PasswordUtil` / BCrypt hashing before deploying to production.
- **DB Credentials** — Stored in `~/.dental_clinic/db.properties` (outside the project directory). Never commit this file.
- **`.sql` files** — Do not push `dental_clinic_db.sql` with real patient data to version control.

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
