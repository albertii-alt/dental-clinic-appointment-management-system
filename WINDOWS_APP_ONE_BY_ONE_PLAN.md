# Windows App One-by-One Plan

Goal: A real Windows desktop app experience where users only install, sign in, and use the system.

Important rule:
- The desktop app must not connect directly to MySQL.
- Desktop app -> Backend API -> Database.

---

## Step 1 (Current): Define target architecture and acceptance checks

Status: DONE

What this step does:
- Locks the product direction before coding.
- Prevents unsafe direct-DB desktop release.

Decisions made:
- Keep desktop app as client UI.
- Introduce backend API service for auth and data operations.
- Keep database private to backend only.

Acceptance checks for this step:
- [x] Architecture rule documented.
- [x] "Install and sign in" user flow defined.

Install-and-sign-in target flow:
1. User installs Windows app.
2. User opens app.
3. User enters credentials.
4. App talks to backend API (HTTPS).
5. Backend validates against database.
6. App loads role-based dashboard.

---

## Step 2 (Completed): Build minimal backend API (Auth + Health)

Status: DONE

Scope:
- `POST /auth/login`
- `GET /health`

Definition of done:
- Desktop can call API login endpoint.
- No DB credentials inside desktop app.

Implemented in this step:
- Added API server entrypoint at `src/com/dentalclinic/main/ApiServerMain.java`.
- Added HTTP handlers for:
	- `GET /health`
	- `POST /auth/login`
- Added minimal JSON helper and API server classes:
	- `src/com/dentalclinic/api/ApiServer.java`
	- `src/com/dentalclinic/api/MiniJson.java`
- Smoke-tested endpoints locally on a test port.

---

## Step 3: Switch desktop login to API mode

Status: IN PROGRESS

Scope:
- Replace direct DB login call with API call.
- Keep same login UI behavior.

Progress in this step:
- [x] Desktop login now calls backend API endpoint POST /auth/login.
- [x] Startup availability check now validates backend API health endpoint.
- [ ] Remove remaining desktop direct-DB dependencies beyond login flow.

Spring Boot migration progress:
- [x] Created Spring Boot module at `backend-spring/`.
- [x] Added Spring endpoints `GET /health` and `POST /auth/login`.
- [x] Replaced login bridge with native Spring JDBC + BCrypt authentication.
- [ ] Add JWT/session token support for production API security.

Definition of done:
- Login works with backend API.
- Desktop no longer needs `db.url/db.user/db.password`.

---

## Step 4: Package Windows installer

Status: PLANNED

Scope:
- Build installer with bundled runtime.
- Add desktop and start-menu shortcuts.

Definition of done:
- Works on clean Windows machine without manual Java setup.

---

## Step 5: Pilot checklist on clean Windows machine

Status: PLANNED

Checklist:
- Install app.
- Sign in.
- Open dashboard.
- Open core pages.
- Confirm logout/login again.

Definition of done:
- End-user can install and use app with no technical setup.

---

## Notes

- Keep cloud scripts and ops runbook for backend operations.
- Keep least-privilege runtime DB user model.
- Admin credentials remain maintenance-only.
