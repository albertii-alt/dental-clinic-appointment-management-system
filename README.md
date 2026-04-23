# Vantage Dental Clinic — Appointment Management System

A Java Swing desktop application for managing dental clinic operations, including patient records, appointment scheduling, role-based access control, and automated email notifications.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Build & Run](#build--run)
- [Roles & Permissions](#roles--permissions)
- [Security](#security)
- [License](#license)

---

## Features

### Appointment Management
- Book, approve, decline, reschedule, and cancel appointments
- One active appointment per patient enforced (Pending or Approved)
- Booking lead time validation against clinic settings
- Closed-day and blocked time slot enforcement
- Automatic expiry of past approved appointments on login
- Status lifecycle: `Pending → Approved / Declined / Rescheduled / Completed / Cancelled / Expired / No Show`

### Email Notifications (via JavaMail / SMTP)
- Welcome email on patient self-registration
- Appointment confirmation, cancellation, decline, and reschedule notifications
- Day-before reminder sent automatically on application startup
- Day-of reminder sent automatically on application startup
- Password reset code (6-digit, 15-minute expiry)
- All emails sent asynchronously (non-blocking thread pool, max 5 concurrent)

### Patient Portal
- Self-registration with cross-table username uniqueness check
- View today's appointments, upcoming schedule, history, and cancelled appointments
- In-app notification panel (unread badge, mark as read, archive)
- Profile management with password change

### Staff Portal
- Manage pending requests (approve / decline / reschedule)
- View today's schedule and upcoming appointments
- Treatment history with clinical notes
- Block / unblock individual time slots or entire days
- Register patients on behalf of walk-ins

### Admin Portal
- Full user management: create, edit, activate/deactivate, delete staff accounts
- Role and permission management (granular per-role permission assignment)
- Clinic settings: operating hours, time slots, services, booking lead time, closed days
- Audit trail (activity log per user action)
- System log (INFO / WARNING / ERROR events)
- Reports: Patient, Appointment, Pending Approvals, Completed Treatments, Cancelled, Service Popularity
- Excel export for all report types

### Dentist Portal
- View today's schedule and upcoming appointments
- Update treatment records and clinical notes
- View patient history

### Security
- BCrypt password hashing (cost factor 10) for all accounts
- Account lockout after 5 failed login attempts (30-minute auto-unlock using database time)
- Session timeout after 30 minutes of inactivity
- Force password reset flag for new or admin-reset accounts
- Cross-table username uniqueness (patients and staff share no usernames)
- Input sanitization (XSS prevention) on all user-facing fields
- Forgot password via username-based 6-digit email code (1-minute rate limit on requests)
- Credentials stored outside the project directory (`~/.dental_clinic/db.properties`)
- Database connection error messages sanitized (no IP/credential leakage in UI)

---

## Tech Stack

| Component        | Technology                              |
|------------------|-----------------------------------------|
| Language         | Java 21 (Temurin)                       |
| UI Framework     | Java Swing (Metal L&F, Segoe UI fonts)  |
| Database         | MySQL / MariaDB                         |
| JDBC Driver      | mysql-connector-j 8.0.33                |
| Connection Pool  | HikariCP 5.1.0                          |
| Password Hashing | bcrypt-0.9.0 (favre)                    |
| Email            | javax.mail 1.6.2                        |
| Icons            | Ikonli 12.3.1 (FontAwesome 5)           |
| Charts           | JFreeChart 1.5.3                        |
| Excel Export     | Apache POI 5.2.5                        |
| Date Picker      | JCalendar 1.4                           |
| Build Tool       | Apache Ant (NetBeans build.xml)         |
| Logging          | SLF4J 2.0.13 + Log4j API 2.21.1        |

---

## Project Structure

```
src/com/dentalclinic/
├── controller/       # Request handling between view and service
│   ├── AdminController.java
│   ├── AppointmentController.java
│   ├── AuthController.java
│   ├── ClinicSettingsController.java
│   ├── DashboardController.java
│   ├── LogController.java
│   ├── PatientController.java
│   ├── ReportsController.java
│   └── RolesController.java
├── dao/              # Database access (raw SQL via JDBC)
│   ├── AppointmentDAO.java
│   ├── ClinicConfigDAO.java
│   ├── DashboardDAO.java
│   ├── LogDAO.java
│   ├── PasswordResetDAO.java
│   ├── PatientDAO.java
│   ├── ReportsDAO.java
│   ├── RolesPermissionDAO.java
│   └── StaffDAO.java
├── dto/              # Data transfer objects
│   ├── appointment/  # AppointmentRequest, BookingResult
│   ├── auth/         # LoginRequest, LoginResult
│   └── report/       # ReportData, ReportRequest
├── main/             # Application entry point (Main.java)
├── model/            # Domain models
│   ├── Appointment.java / AppointmentStatus.java
│   ├── Patient.java / Staff.java / Role.java
│   ├── BlockedSlot.java / ClinicHour.java
│   ├── ClinicScheduleDay.java / ClinicSetting.java
│   ├── DentalService.java / Permission.java
│   ├── ActivityLogEntry.java / SystemLogEntry.java
│   ├── PasswordResetCode.java / UserType.java
│   └── RolePermission.java
├── service/          # Business logic layer
│   ├── AppointmentService.java
│   ├── AuthService.java
│   ├── ClinicConfigService.java
│   ├── DashboardService.java
│   ├── LogService.java
│   ├── PatientService.java
│   ├── ReportsService.java
│   ├── RolesService.java
│   └── StaffService.java
├── util/             # Shared utilities
│   ├── DBConnection.java      # HikariCP pool + config loading
│   ├── EmailUtil.java         # Async SMTP email sender
│   ├── PasswordUtil.java      # BCrypt hash/verify
│   ├── PasswordValidator.java # Complexity rules
│   ├── PasswordMigration.java # Plaintext → BCrypt migration helper
│   ├── Sanitizer.java         # XSS prevention + input validation
│   └── UserSession.java       # Session state + timeout timer
└── view/             # Swing UI panels and dialogs
    ├── admin/        # Admin dashboard panels
    ├── components/   # Shared UI components (Sidebar, Dialogs)
    ├── patient/      # Patient dashboard panels
    ├── staff/        # Staff dashboard panels
    ├── util/         # AppointmentUIUtils
    ├── AdminDashboard.java / DentistDashboard.java
    ├── StaffDashboard.java / PatientDashboard.java
    ├── LoginPage.java
    └── SplashScreen.java
```

---

## Prerequisites

- Java 21 (Temurin recommended — see `.sdkmanrc`)
- MySQL 5.7+ or MariaDB 10.4+
- Apache Ant (for building from source)

---

## Database Setup

1. Start your MySQL server.

2. Create the database:
   ```sql
   CREATE DATABASE dental_clinic_db;
   ```

3. Import the schema:
   ```bash
   mysql -u root -p dental_clinic_db < dental_clinic_db.sql
   ```

4. Create a least-privileged application user:
   ```sql
   CREATE USER 'dental_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON dental_clinic_db.* TO 'dental_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

---

## Configuration

All credentials are stored **outside the project directory** and are never committed to version control.

### Option 1 — Config file (recommended)

Create `~/.dental_clinic/db.properties`:

```properties
# Database
db.url=jdbc:mysql://localhost:3306/dental_clinic_db?useSSL=true&serverTimezone=UTC
db.user=dental_user
db.password=your_password

# Email (optional — disabling email is safe, notifications are skipped)
email.user=your_gmail@gmail.com
email.password=your_app_password
email.smtp.host=smtp.gmail.com
email.smtp.port=587
```

> For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833), not your account password.

### Option 2 — Environment variables

```bash
export DB_URL=jdbc:mysql://localhost:3306/dental_clinic_db?useSSL=true&serverTimezone=UTC
export DB_USER=dental_user
export DB_PASSWORD=your_password
```

### Option 3 — Setup Wizard

On first launch, if no config file is found, a **Database Setup Wizard** dialog appears automatically.

---

## Build & Run

### Build with Ant

```bash
ant clean build
```

### Run the JAR

```bash
java -jar dist/DentalClinicAppointment_ManagementSystem.jar
```

### Windows launcher

```bat
scripts\windows\run-dental-clinic.bat
```

### First login

Use the default `superAdmin` credentials created by the schema. **Change the password immediately after first login.**

The superAdmin account bypasses all permission checks and has full system access.

---

## Roles & Permissions

| Role           | Key Capabilities                                                                 |
|----------------|----------------------------------------------------------------------------------|
| **Super Admin**| Full access, bypasses all permission checks                                      |
| **Admin**      | User management, roles, clinic settings, audit logs, reports                     |
| **Dentist**    | Today's schedule, upcoming appointments, treatment records, clinical notes       |
| **Staff**      | Pending requests, patient registration, booking, schedule management, reminders  |
| **Patient**    | Self-registration, book appointments, view history, notifications, profile       |

Permissions are stored in the database and assigned per role. Admins can configure which permissions each role holds via the **Manage Roles** panel.

---

## Security

| Concern                  | Implementation                                                              |
|--------------------------|-----------------------------------------------------------------------------|
| Password storage         | BCrypt (cost 10) via `PasswordUtil`                                         |
| Password complexity      | Min 8 chars, uppercase, lowercase, digit, special char — `PasswordValidator`|
| Account lockout          | 5 failed attempts → 30-minute lockout (database-time based)                 |
| Session timeout          | 30-minute inactivity timeout via `UserSession` timer                        |
| Input sanitization       | XSS patterns, HTML tags, null bytes stripped via `Sanitizer`                |
| SQL injection            | All queries use `PreparedStatement`                                         |
| Credential storage       | `~/.dental_clinic/db.properties` (outside project, never committed)         |
| Password reset           | Username-based, 6-digit code, 15-minute expiry, 1-minute request rate limit |
| Email privacy            | Passwords never sent via email; email addresses masked in logs              |
| Error messages           | DB errors sanitized before display (no IP/credential leakage)               |
| Cross-table uniqueness   | Usernames checked across both `patients` and `staff` tables on registration |

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
