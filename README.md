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
