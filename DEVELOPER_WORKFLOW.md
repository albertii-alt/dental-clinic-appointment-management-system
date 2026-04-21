# 🛠️ Developer Workflow Guide

## Project Structure (Quick Recap)

| Part | Location | What it is |
|------|----------|------------|
| Desktop App | `src/com/dentalclinic/` | Java Swing UI — all panels, dashboards, forms |
| Backend API | `backend-spring/` | Spring Boot REST API running on port 8081 |
| Build tool (desktop) | `build.xml` | Apache Ant |
| Build tool (backend) | `backend-spring/pom.xml` | Maven |

---

## Local Development

### When you change anything in `backend-spring/`
```bash
cd /home/ivylxvie/Applications/DentalClinicSystem/backend-spring
mvn clean package -DskipTests
```

### When you change anything in `src/com/dentalclinic/` (desktop app)
```bash
cd /home/ivylxvie/Applications/DentalClinicSystem
ant clean jar
```

### When you change both
Run both commands above, then restart the app.

### Launch the app
```bash
cd /home/ivylxvie/Applications/DentalClinicSystem
./run.sh
```

`run.sh` will:
1. Start the Spring backend JAR on port 8081
2. Wait for `/health` to respond
3. Launch the desktop app JAR

---

## Before Releasing — Always Build Locally First

```bash
# 1. Build backend
cd backend-spring && mvn clean package -DskipTests && cd ..

# 2. Build desktop app
ant clean jar

# 3. Test by running the app
./run.sh
```

If both builds succeed and the app runs correctly, you're ready to release.

---

## Releasing a New Windows Installer

```bash
git add .
git commit -m "your message"
git push
git tag v1.x.x
git push origin v1.x.x
```

GitHub Actions will automatically:
1. Build the Spring backend JAR
2. Compile the desktop app JAR
3. Package everything into a Windows `.exe` installer
4. Create a GitHub Release with the `.exe` attached

Users can then download the installer from **GitHub → Releases**.

---

## Database Configuration

- Local config: `~/.dental_clinic/db.properties`
- Cloud DB: Aiven MySQL
- Credentials for Render: set as environment variables in the Render dashboard

---

## Key Files

| File | Purpose |
|------|---------|
| `run.sh` | Linux launcher — starts backend then desktop app |
| `scripts/windows/run-dental-clinic.bat` | Windows launcher — same as run.sh but for Windows |
| `scripts/windows/DentalClinicSystem.iss` | Inno Setup script — defines the Windows installer |
| `scripts/windows/build-desktop-installer.ps1` | PowerShell script — builds the installer locally on Windows |
| `.github/workflows/windows-installer.yml` | GitHub Actions — automates the Windows installer build and release |
| `backend-spring/src/main/resources/application.yml` | Spring Boot config — DB and JWT settings |
| `~/.dental_clinic/db.properties` | Local DB credentials (never commit this) |
| `.env` | Local env vars (never commit this) |
