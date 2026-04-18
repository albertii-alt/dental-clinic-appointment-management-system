# Dental Clinic System MVC Boundaries

This project is being migrated to a strict layered flow:

`View (Swing) -> Controller -> Service -> DAO -> DB`

## Layer Responsibilities

- **View (`com.dentalclinic.ui`, `com.dentalclinic.admin`, `com.dentalclinic.patient`, `com.dentalclinic.staff`)**
  - Render Swing components and collect user input.
  - Call controllers from event handlers.
  - Must not import `java.sql.*`, `DBConnection`, or DAO classes.

- **Controller (`com.dentalclinic.controller`)**
  - Accepts UI requests and translates them into service calls.
  - Returns UI-ready results (success/error/navigation decisions).
  - Must not execute SQL directly.

- **Service (`com.dentalclinic.service`)**
  - Holds business rules, validation, and cross-DAO workflow logic.
  - No Swing dependencies.

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

This static check fails if view classes import `java.sql.*` or DAO packages.

## Example Allowed Dependency Paths

- `LoginPage -> AuthController -> AuthService -> PatientDAO/StaffDAO`
- `ManageRolesPanel -> RolesController -> RolesService -> RolesPermissionDAO`
- `ReportsPanel -> ReportsController -> ReportsService -> ReportsDAO`
