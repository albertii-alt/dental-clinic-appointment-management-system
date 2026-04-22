#!/bin/bash

# ============================================
# Dental Clinic Launcher (Cloud/Aiven ready)
# - No local XAMPP dependency
# - Connects directly to Aiven MySQL
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

APP_JAR="$SCRIPT_DIR/dist/DentalClinicAppointment_ManagementSystem.jar"
LOG_FILE="/tmp/dental_clinic_final.log"
CONFIG_FILE="$HOME/.dental_clinic/db.properties"

echo "[$(date)] ==========================================" >> "$LOG_FILE"
echo "[$(date)] Starting Dental Clinic (cloud launcher)" >> "$LOG_FILE"

# --- Java detection ---
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"

JAVA_CMD="$(which java 2>/dev/null)"
if [ -z "$JAVA_CMD" ] || [ ! -x "$JAVA_CMD" ]; then
    zenity --error --title="Java Not Found" \
           --text="Java is not installed or not in PATH.\n\nPlease install Java 21 and try again." 2>/dev/null
    exit 1
fi

JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | sed -n '1s/.*version "\([^"]*\)".*/\1/p')
if [[ ! "$JAVA_VERSION" =~ ^21\. ]]; then
    zenity --error --title="Wrong Java Version" \
           --text="Detected Java: $JAVA_VERSION\n\nThis app requires Java 21.\n\nPlease switch to Java 21 and try again." 2>/dev/null
    exit 1
fi

# --- App JAR check ---
if [ ! -f "$APP_JAR" ]; then
    zenity --error --title="Missing Application" \
           --text="Application JAR not found:\n$APP_JAR" 2>/dev/null
    exit 1
fi

# --- Config check ---
if [ ! -f "$CONFIG_FILE" ]; then
    echo "[$(date)] WARN: Config file missing at $CONFIG_FILE" >> "$LOG_FILE"
    zenity --warning --title="First Time Setup" \
           --text="No database configuration found.\n\nThe setup wizard will open in the app." 2>/dev/null
fi

# --- Display setup ---
if [ -z "${DISPLAY:-}" ]; then
    for d in 0 1; do
        if [ -S "/tmp/.X11-unix/X$d" ]; then
            export DISPLAY=":$d"
            break
        fi
    done
fi

if [ -z "${DISPLAY:-}" ]; then
    zenity --error --title="No Display" \
           --text="No GUI display found.\n\nPlease launch from your desktop session." 2>/dev/null
    exit 1
fi

# --- Launch app ---
echo "[$(date)] Launching application..." >> "$LOG_FILE"
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS JDK_JAVA_OPTIONS

DISPLAY="$DISPLAY" XAUTHORITY="${XAUTHORITY:-$HOME/.Xauthority}" \
"$JAVA_CMD" -Djava.awt.headless=false -jar "$APP_JAR" >> "/tmp/dental_clinic_app.log" 2>&1 &

APP_PID=$!
echo "$APP_PID" > "$SCRIPT_DIR/.app.pid"
echo "[$(date)] Application started with PID: $APP_PID" >> "$LOG_FILE"

zenity --info --title="Dental Clinic" --text="Dental Clinic is running." --timeout=4 2>/dev/null

wait "$APP_PID"
APP_EXIT_CODE=$?

rm -f "$SCRIPT_DIR/.app.pid"
echo "[$(date)] Application closed (exit code: $APP_EXIT_CODE)" >> "$LOG_FILE"

if [ "$APP_EXIT_CODE" -ne 0 ]; then
    zenity --error --title="Application Closed Unexpectedly" \
           --text="The app closed unexpectedly.\n\nCheck log: /tmp/dental_clinic_app.log" 2>/dev/null
fi

echo "[$(date)] Script completed" >> "$LOG_FILE"
exit 0
