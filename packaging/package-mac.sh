#!/usr/bin/env bash
# Builds a macOS .dmg installer. Run on a Mac with JDK 17+ and Maven.
set -e
cd "$(dirname "$0")/.."
mvn clean package
jpackage \
  --type dmg \
  --name cal-nav \
  --app-version 1.0.0 \
  --input target \
  --main-jar cal-nav.jar \
  --main-class com.juttiga.calendar.Main \
  --dest dist
echo "Installer written to dist/"
