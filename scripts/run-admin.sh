#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME}"
exec "$JAVA_HOME/bin/java" "-Datm.config=${ATM_CONFIG:-$PWD/resources/database.properties}" -cp 'build/classes:lib/*' com.atm.desktop.AdminDashboard
