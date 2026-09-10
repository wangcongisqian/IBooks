#!/usr/bin/env sh
# Thin wrapper: prefer the Gradle wrapper JAR when present, else system gradle.
DIR=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
if [ -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Open this folder in IntelliJ IDEA (File → Open) and use the bundled Gradle."
echo "Or install Gradle 8.10+ and run: gradle wrapper --gradle-version 8.10.2"
exit 1
