#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

view_paths=(
  "src/com/dentalclinic/ui"
  "src/com/dentalclinic/admin"
  "src/com/dentalclinic/patient"
  "src/com/dentalclinic/staff"
)

status=0
echo "Checking MVC boundaries for view classes..."

for path in "${view_paths[@]}"; do
  [[ -d "$path" ]] || continue

  if grep -RInE "import[[:space:]]+java\\.sql\\." "$path" --include='*.java' >/tmp/mvc_sql_hits.txt; then
    echo "ERROR: java.sql import found in view classes under $path"
    cat /tmp/mvc_sql_hits.txt
    status=1
  fi

  if grep -RInE "import[[:space:]]+com\\.dentalclinic\\.dao\\." "$path" --include='*.java' >/tmp/mvc_dao_hits.txt; then
    echo "ERROR: DAO import found in view classes under $path"
    cat /tmp/mvc_dao_hits.txt
    status=1
  fi
done

rm -f /tmp/mvc_sql_hits.txt /tmp/mvc_dao_hits.txt

if [[ $status -ne 0 ]]; then
  echo "MVC boundary check failed."
  exit 1
fi

echo "MVC boundary check passed."
