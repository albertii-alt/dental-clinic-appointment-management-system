#!/bin/bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUNTIME_ROOT="$REPO_ROOT/build/windows-runtime"
RUNTIME_DIR="$RUNTIME_ROOT/runtime"
ZIP_FILE="$RUNTIME_ROOT/jre21-windows-x64.zip"
DOWNLOAD_URL="https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk"

if [[ -x "$RUNTIME_DIR/bin/javaw.exe" ]]; then
  echo "Windows runtime already prepared: $RUNTIME_DIR"
  exit 0
fi

if ! command -v unzip >/dev/null 2>&1; then
  echo "Missing required tool: unzip"
  echo "Install it first (example: sudo apt install unzip)"
  exit 1
fi

mkdir -p "$RUNTIME_ROOT"

echo "Downloading Windows Java runtime (Temurin JRE 21)..."
curl -fL "$DOWNLOAD_URL" -o "$ZIP_FILE"

TMP_DIR="$RUNTIME_ROOT/extract-tmp"
rm -rf "$TMP_DIR" "$RUNTIME_DIR"
mkdir -p "$TMP_DIR"

echo "Extracting runtime..."
unzip -q "$ZIP_FILE" -d "$TMP_DIR"

TOP_DIR="$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [[ -z "$TOP_DIR" ]]; then
  echo "Runtime extraction failed: no top-level directory found"
  exit 1
fi

# Move extracted runtime directory as-is to avoid copy issues with archive metadata.
mv "$TOP_DIR" "$RUNTIME_DIR"

if [[ ! -x "$RUNTIME_DIR/bin/javaw.exe" ]]; then
  echo "Runtime preparation failed: javaw.exe not found in $RUNTIME_DIR/bin"
  exit 1
fi

rm -rf "$TMP_DIR"

echo "Windows runtime ready: $RUNTIME_DIR"
