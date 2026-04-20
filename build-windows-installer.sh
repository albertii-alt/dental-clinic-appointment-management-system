#!/bin/bash
set -euo pipefail

APP_VERSION="${1:-1.0.0}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_DIR="$REPO_ROOT/build/windows-installer/output"
INSTALLER_FILE="$OUTPUT_DIR/DentalClinicSystem-Setup-$APP_VERSION.exe"
CHECKSUM_FILE="$INSTALLER_FILE.sha256"
CHECKSUM_TXT="$OUTPUT_DIR/installer-checksum.txt"

cd "$REPO_ROOT"

./scripts/windows/build-wine-installer.sh "$APP_VERSION"

if [[ ! -f "$INSTALLER_FILE" ]]; then
  echo "Expected installer not found: $INSTALLER_FILE"
  exit 1
fi

sha256sum "$INSTALLER_FILE" > "$CHECKSUM_FILE"
cp "$CHECKSUM_FILE" "$CHECKSUM_TXT"

echo "Checksum generated: $CHECKSUM_FILE"
cat "$CHECKSUM_FILE"
echo "Checksum generated: $CHECKSUM_TXT"
