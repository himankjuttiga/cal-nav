#!/usr/bin/env bash
# Builds a Linux .deb package. Run on Linux with JDK 17+ and Maven.
# For an .rpm instead, change --type deb to --type rpm.
set -e
cd "$(dirname "$0")/.."
mvn clean package
jpackage \
  --type deb \
  --name cal-nav \
  --app-version 1.0.0 \
  --input target \
  --main-jar cal-nav.jar \
  --main-class com.juttiga.calendar.Main \
  --dest dist
echo "Installer written to dist/"
