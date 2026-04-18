#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

status=0
echo "Checking strict MVC boundaries..."

check_pattern() {
  local path="$1"
  local pattern="$2"
  local message="$3"
  local tmp_file
  tmp_file="$(mktemp)"

  if [[ -d "$path" ]] && grep -RInE "$pattern" "$path" --include='*.java' >"$tmp_file"; then
    echo "ERROR: $message"
    cat "$tmp_file"
    status=1
  fi

  rm -f "$tmp_file"
}

# View constraints: no direct JDBC imports, no DAO imports.
check_pattern "src/com/dentalclinic/view" "import[[:space:]]+java\\.sql\\." \
  "View must not import java.sql.*"
check_pattern "src/com/dentalclinic/view" "import[[:space:]]+com\\.dentalclinic\\.dao\\." \
  "View must not import DAO packages"

# Controller constraints: no DAO imports, no DBConnection usage.
check_pattern "src/com/dentalclinic/controller" "import[[:space:]]+com\\.dentalclinic\\.dao\\." \
  "Controller must not import DAO packages"
check_pattern "src/com/dentalclinic/controller" "DBConnection" \
  "Controller must not reference DBConnection"

# Service constraints: no Swing/AWT imports.
check_pattern "src/com/dentalclinic/service" "import[[:space:]]+javax\\.swing\\." \
  "Service must not import Swing classes"
check_pattern "src/com/dentalclinic/service" "import[[:space:]]+java\\.awt\\." \
  "Service must not import AWT classes"

if [[ $status -ne 0 ]]; then
  echo "MVC boundary check failed."
  exit 1
fi

echo "MVC boundary check passed."
