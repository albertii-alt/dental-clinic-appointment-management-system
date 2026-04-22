# DB Drop & Restore Test (Aiven Cloud)

A record of the live backup and restore test performed against the Aiven MySQL database.

---

## Step 1 — Run a fresh backup

```bash
./scripts/cloud-backup-dental.sh
```

Expected output:
```
Backup success: backup_YYYYMMDD_HHMMSS.sql.gz
```

---

## Step 2 — Switch to admin user (avnadmin)

Required because `dentalclinicsystem` has no `CREATE DATABASE` privilege.

```bash
NEW_DB_USER=avnadmin NEW_DB_PASSWORD='your-password' ./scripts/cloud-switch-app-user.sh
```

---

## Step 3 — Delete the database on Aiven

Go to **Aiven dashboard → Databases tab** → delete `dental_clinic_db`.

---

## Step 4 — Restore from latest backup

```bash
./scripts/cloud-restore-latest.sh
```

Type `YES` when prompted. Expected output:
```
Restore completed successfully.
```

---

## Step 5 — Switch back to least-privilege user

Do this immediately after restore.

```bash
NEW_DB_USER=dentalclinicsystem NEW_DB_PASSWORD='your-password' ./scripts/cloud-switch-app-user.sh
```

---

## Step 6 — Verify least-privilege user cannot drop the database

```bash
mysql --protocol=TCP \
  -h <host> -P <port> \
  -u dentalclinicsystem -p \
  --ssl-mode=REQUIRED \
  -e "DROP DATABASE dental_clinic_db;"
```

Expected:
```
ERROR 1044 (42000): Access denied
```

---

## Step 7 — Launch the app and confirm it works

```bash
./run.sh
```

Log in and verify data is intact.

---

## Test Results

| Step | Result |
|------|--------|
| Backup | ✅ Passed |
| Switch to avnadmin | ✅ Passed |
| Delete database | ✅ Passed |
| Restore from backup | ✅ Passed |
| Switch back to dentalclinicsystem | ✅ Passed |
| Drop attempt by least-privilege user | ✅ Correctly denied |
| App launch after restore | ✅ Passed |
