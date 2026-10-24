#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME}"
classpath="build/classes:build/test-classes:lib/*"
case "${1:-unit}" in
  unit) "$JAVA_HOME/bin/java" -cp "$classpath" com.atm.tests.SecurityTest
        "$JAVA_HOME/bin/java" -cp "$classpath" com.atm.tests.ThreadTest ;;
  integration) "$JAVA_HOME/bin/java" -cp "$classpath" com.atm.tests.BankingIntegrationTest ;;
  web) "$JAVA_HOME/bin/java" -cp "$classpath" com.atm.tests.WebSmokeTest ;;
  desktop) "$JAVA_HOME/bin/java" -cp "$classpath" com.atm.tests.DesktopSmokeTest ;;
  *) echo 'Usage: scripts/test.sh unit|integration|web|desktop' >&2; exit 2 ;;
esac
