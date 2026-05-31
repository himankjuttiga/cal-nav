# Distributing cal-nav

Two ways to give cal-nav to other people.

## 1. Runnable JAR (simplest, needs Java 17+ on their machine)

Build it:

    mvn clean package

This produces `target/cal-nav.jar` with your code and FlatLaf bundled in.
Anyone with Java 17 or newer can run it:

    java -jar cal-nav.jar

On most desktops they can also just double-click the jar.

## 2. Native installer (no Java needed by your users)

`jpackage` ships with the JDK and bundles a Java runtime inside a real
installer. It only builds for the OS you run it on, so use the matching script:

    packaging/package-mac.sh        ->  dist/cal-nav-1.0.0.dmg
    packaging/package-windows.bat   ->  dist/cal-nav-1.0.0.msi
    packaging/package-linux.sh      ->  dist/cal-nav_1.0.0_amd64.deb

Prerequisites:
- macOS / Linux: JDK 17+ and Maven.
- Windows: JDK 17+, Maven, and the WiX Toolset v3 (jpackage needs it for msi/exe).

## Build all three at once with CI

`.github/workflows/build.yml` builds the Mac, Windows, and Linux installers on
GitHub's runners. Push a tag like `v1.0.0`, or run the workflow manually, then
download the installers from the workflow's Artifacts.

## Signing (so users do not see scary warnings)

Unsigned apps trigger a warning the first time they open:
- macOS Gatekeeper: users must right-click the app and choose Open, unless you
  sign and notarize with an Apple Developer ID ($99/year).
- Windows SmartScreen: users click "More info" then "Run anyway", unless you
  sign with a code-signing certificate.

Signing is optional for sharing with friends or testers; it mainly matters for
wide public distribution.

## Where data is stored

Events are saved per user at `~/.cal-nav/events.txt` (the equivalent path on
each OS). New users start with an empty calendar. You can still launch with a
custom file by passing a path as the first argument.
