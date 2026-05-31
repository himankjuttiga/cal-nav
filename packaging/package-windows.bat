@echo off
REM Builds a Windows .msi installer. Run on Windows with JDK 17+, Maven,
REM and the WiX Toolset v3 installed (required by jpackage for msi/exe).
cd /d "%~dp0\.."
call mvn clean package || exit /b 1
jpackage ^
  --type msi ^
  --name cal-nav ^
  --app-version 1.0.0 ^
  --input target ^
  --main-jar cal-nav.jar ^
  --main-class com.juttiga.calendar.Main ^
  --win-shortcut --win-menu ^
  --dest dist
echo Installer written to dist\
