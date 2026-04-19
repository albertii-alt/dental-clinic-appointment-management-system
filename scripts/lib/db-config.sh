#!/usr/bin/env bash

# Shared DB config loader for scripts.
# Reads ~/.dental_clinic/db.properties used by the Java app.

set -euo pipefail

CONFIG_FILE_DEFAULT="$HOME/.dental_clinic/db.properties"

trim() {
    local value="$1"
    # shellcheck disable=SC2001
    value="$(echo "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
    printf '%s' "$value"
}

read_prop() {
    local key="$1"
    local file="$2"
    local line
    line="$(grep -E "^[[:space:]]*${key}[[:space:]]*=" "$file" | tail -n 1 || true)"
    if [[ -z "$line" ]]; then
        return 1
    fi
    printf '%s' "${line#*=}"
}

resolve_mysql_bin() {
    local candidate="$1"
    local xampp_path="/opt/lampp/bin/$candidate"
    if command -v "$candidate" >/dev/null 2>&1; then
        command -v "$candidate"
        return 0
    fi
    if [[ -x "$xampp_path" ]]; then
        printf '%s' "$xampp_path"
        return 0
    fi
    return 1
}

load_db_config() {
    local config_file="${1:-$CONFIG_FILE_DEFAULT}"

    if [[ ! -f "$config_file" ]]; then
        echo "ERROR: Config file not found at $config_file"
        return 1
    fi

    local raw_url raw_user raw_password raw_root_password
    raw_url="$(read_prop "db.url" "$config_file" || true)"
    raw_user="$(read_prop "db.user" "$config_file" || true)"
    raw_password="$(read_prop "db.password" "$config_file" || true)"
    raw_root_password="$(read_prop "db.root.password" "$config_file" || true)"

    DB_URL="$(trim "$raw_url")"
    DB_USER="$(trim "$raw_user")"
    DB_PASSWORD="$(trim "$raw_password")"
    DB_ROOT_PASSWORD="$(trim "$raw_root_password")"

    if [[ -z "$DB_URL" || -z "$DB_USER" ]]; then
        echo "ERROR: db.url and db.user are required in $config_file"
        return 1
    fi

    export DB_URL DB_USER DB_PASSWORD DB_ROOT_PASSWORD
    parse_jdbc_mysql_url "$DB_URL"

    MYSQL_BIN="$(resolve_mysql_bin mysql)"
    MYSQLADMIN_BIN="$(resolve_mysql_bin mysqladmin)"
    MYSQLDUMP_BIN="$(resolve_mysql_bin mysqldump)"

    export MYSQL_BIN MYSQLADMIN_BIN MYSQLDUMP_BIN
}

parse_jdbc_mysql_url() {
    local jdbc_url="$1"
    local without_prefix host_port_db host_port

    without_prefix="${jdbc_url#jdbc:mysql://}"
    host_port_db="${without_prefix%%\?*}"
    host_port="${host_port_db%%/*}"

    DB_NAME="${host_port_db#*/}"
    if [[ "$DB_NAME" == "$host_port_db" ]]; then
        echo "ERROR: Could not parse database name from db.url: $jdbc_url"
        return 1
    fi

    if [[ "$host_port" == *:* ]]; then
        DB_HOST="${host_port%%:*}"
        DB_PORT="${host_port##*:}"
    else
        DB_HOST="$host_port"
        DB_PORT="3306"
    fi

    DB_USE_SSL="false"
    DB_SSL_MODE=""
    if [[ "$jdbc_url" == *"useSSL=true"* ]]; then
        DB_USE_SSL="true"
    fi
    if [[ "$jdbc_url" =~ sslMode=([A-Za-z_]+) ]]; then
        DB_SSL_MODE="${BASH_REMATCH[1]}"
        DB_USE_SSL="true"
    fi

    export DB_HOST DB_PORT DB_NAME DB_USE_SSL DB_SSL_MODE
}

mysql_base_args() {
    printf '%s\n' "--protocol=TCP" "--host=$DB_HOST" "--port=$DB_PORT" "--user=$DB_USER"
}

mysql_ssl_args() {
    if [[ "$DB_USE_SSL" == "true" ]]; then
        if [[ -n "$DB_SSL_MODE" ]]; then
            printf '%s\n' "--ssl-mode=$DB_SSL_MODE"
        else
            printf '%s\n' "--ssl"
        fi
    fi
}
