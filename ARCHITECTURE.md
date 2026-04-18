# Dental Clinic System MVC Boundaries

This project is being migrated to a strict layered flow:

`View (Swing) -> Controller -> Service -> DAO -> DB`

## Layer Responsibilities

- **View (`com.dentalclinic.view...`)**
  - Render Swing components and collect user input.
  - Call controllers from event handlers.
  - Must not import `java.sql.*`, `DBConnection`, or DAO classes.
  - Must not call services directly.

- **Controller (`com.dentalclinic.controller`)**
  - Accepts UI requests and translates them into service calls.
  - Returns UI-ready results (success/error/navigation decisions).
  - Must not import DAO classes or reference `DBConnection`.
  - Must not execute SQL directly.

- **Service (`com.dentalclinic.service`)**
  - Holds business rules, validation, and cross-DAO workflow logic.
  - No Swing/AWT dependencies.

- **DAO (`com.dentalclinic.dao`)**
  - Owns all SQL/JDBC and `DBConnection` usage.
  - Maps between database rows and domain objects/DTO payloads.

- **Model (`com.dentalclinic.model`)**
  - Domain entities (e.g., `Patient`, `Appointment`, `Role`, `Permission`).
  - No UI or JDBC code.

## Enforcing Boundaries

Run:

```bash
./scripts/check-mvc-boundaries.sh
```

This static check fails when:
- Any `src/com/dentalclinic/view/**` class imports `java.sql.*` or `com.dentalclinic.dao.*`
- Any `src/com/dentalclinic/controller/**` class imports `com.dentalclinic.dao.*` or references `DBConnection`
- Any `src/com/dentalclinic/service/**` class imports `javax.swing.*` or `java.awt.*`

## Example Allowed Dependency Paths

- `LoginPage (view) -> AuthController -> AuthService -> PatientDAO/StaffDAO`
- `ManageRolesPanel (view.admin) -> RolesController -> RolesService -> RolesPermissionDAO`
- `ReportsPanel (view.admin) -> ReportsController -> ReportsService -> ReportsDAO`
- `StaffBookAppointmentPanel (view.staff) -> AppointmentController -> AppointmentService -> AppointmentDAO`
