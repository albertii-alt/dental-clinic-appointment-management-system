#!/bin/bash
set -euo pipefail

APP_VERSION="${1:-1.0.0}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ISCC="$HOME/.wine/drive_c/Program Files (x86)/Inno Setup 6/ISCC.exe"
ISS_FILE="$REPO_ROOT/scripts/windows/DentalClinicSystem.iss"
OUTPUT_DIR="$REPO_ROOT/build/windows-installer/output"

if [[ ! -f "$ISCC" ]]; then
  echo "Inno Setup compiler not found at: $ISCC"
  exit 1
fi

if [[ ! -f "$ISS_FILE" ]]; then
  echo "Installer script not found at: $ISS_FILE"
  exit 1
fi

cd "$REPO_ROOT"

# Prevent Ant clean failures caused by old Wine test prefixes under build/.
if [[ -d "$REPO_ROOT/build/wine-smoke-prefix" ]]; then
  echo "Removing stale build/wine-smoke-prefix before Ant clean..."
  rm -rf "$REPO_ROOT/build/wine-smoke-prefix"
fi

ant clean jar
"$REPO_ROOT/scripts/windows/prepare-windows-runtime.sh"
mkdir -p "$OUTPUT_DIR"

WINE_OUTPUT_DIR="$(winepath -w "$OUTPUT_DIR")"
WINE_ISS_FILE="$(winepath -w "$ISS_FILE")"

wine "$ISCC" /DMyAppVersion="$APP_VERSION" /Qp /O"$WINE_OUTPUT_DIR" "$WINE_ISS_FILE"

echo "Installer build complete. Output directory: $OUTPUT_DIR"
ls -lh "$OUTPUT_DIR"
