#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${JAVA_HOME:?Set JAVA_HOME to a JDK 21 directory}"
: "${TOMCAT_HOME:?Set TOMCAT_HOME to a Tomcat 10.1 directory}"
test -f "$TOMCAT_HOME/lib/servlet-api.jar"
compgen -G 'lib/mysql-connector-j-*.jar' >/dev/null || { echo 'Add Connector/J manually to lib/' >&2; exit 1; }
mkdir -p build
rm -rf build/classes build/test-classes build/war
mkdir -p build/classes build/test-classes build/war/WEB-INF/classes build/war/WEB-INF/lib
find src -name '*.java' | sort > build/sources.txt
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -cp "$TOMCAT_HOME/lib/servlet-api.jar:lib/*" -d build/classes @build/sources.txt
find tests -name '*.java' | sort > build/test-sources.txt
"$JAVA_HOME/bin/javac" --release 21 -encoding UTF-8 -cp "build/classes:$TOMCAT_HOME/lib/servlet-api.jar:lib/*" -d build/test-classes @build/test-sources.txt
cp -R webapp/. build/war/
cp -R build/classes/com build/war/WEB-INF/classes/
cp lib/mysql-connector-j-*.jar build/war/WEB-INF/lib/
"$JAVA_HOME/bin/jar" --create --file build/atm.war -C build/war .
echo 'Built build/atm.war (Java 21, Jakarta Servlet 6)'
