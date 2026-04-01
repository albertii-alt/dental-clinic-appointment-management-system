# 🦷 Dental Clinic Appointment Management System

A robust Java-based desktop application designed to streamline dental clinic operations, manage patient records, and coordinate appointment scheduling with role-based access control.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/language-Java-orange.svg)
![Database](https://img.shields.io/badge/database-MySQL-blue.svg)

---

## 🚀 Features

### 👤 Role-Based Access Control
- **Admin:** Full system management, including staff accounts, clinic settings, and audit logs.
- **Dentist:** Clinical management, medical history access, and schedule coordination.
- **Staff:** Receptionist duties, patient registration, and appointment handling.
- **Patient:** Self-registration and appointment requests.

### 📅 Appointment Management
- Real-time appointment scheduling and tracking.
- Status management (Pending, Approved, Declined, Rescheduled).
- Automated age calculation and visit contact tracking.

### 📋 Patient & Clinical Records
- Comprehensive patient profiles and medical history tracking.
- Secure clinical notes restricted to authorized medical staff.

### ⚙️ Clinic Coordination
- **Schedule Management:** Block/unblock specific time slots for staff breaks or maintenance.
- **Clinic Settings:** Configure lead times for appointments and operating hours.
- **Activity Logs:** Detailed audit trails of user actions for security and accountability.

---

## 🛠️ Technology Stack
- **Language:** Java (JDK 8 or higher)
- **Database:** MySQL / MariaDB (managed via JDBC)
- **UI Framework:** Java Swing
- **Build System:** Ant

---

## 📦 Installation & Setup

### Prerequisites
- Java Development Kit (JDK) 8+
- MySQL Server (e.g., XAMPP/MariaDB)

### 1. Database Configuration
1. Open your MySQL management tool (like phpMyAdmin).
2. Create a new database named `dental_clinic_db`.
3. Import the database schema (The `.sql` file you have locally).
   - *Note: Ensure your database connection settings in `src/com/dentalclinic/util/DatabaseConnection.java` (or similar) match your local environment.*

### 2. Build and Run
You can run the project using the provided shell scripts:

**On Linux/macOS:**
```bash
chmod +x run.sh
./run.sh
```

**On Windows:**
You can run the `run.sh` if using Git Bash, or compile using your IDE (NetBeans/IntelliJ).

---

## 🖥️ Usage

1. **Login:** Use the default `superAdmin` credentials (ensure you change these after the first login!).
2. **Dashboard:** Access the main navigation to manage appointments, patients, or clinic settings based on your assigned role.
3. **Registration:** Patients can register via the login screen to start requesting appointments.

---

## 🔒 Security Recommendations
- **Passwords:** This system uses plaintext storage in the provided schema for demonstration. For production use, please implement BCrypt hashing.
- **Git History:** Ensure no sensitive database credentials or `.sql` files with real patient data are pushed to the repository.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.