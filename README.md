# cal-nav

A personal calendar with Google / Outlook style Day, Week, and Month views.
Click an empty area to create an event, click an event to edit or delete it.
Times are shown and entered in AM/PM. Overlaps are prevented and every change
is saved automatically to `~/.cal-nav/events.json`.

## Download

> No Java installation required — just download and run.

| Platform | Download |
|----------|----------|
| macOS    | [cal-nav.dmg](https://github.com/himankjuttiga/cal-nav/releases/download/latest/cal-nav.dmg) |
| Windows  | [cal-nav-1.0.0.msi](https://github.com/himankjuttiga/cal-nav/releases/download/latest/cal-nav-1.0.0.msi) |
| Linux    | [cal-nav_1.0.0_amd64.deb](https://github.com/himankjuttiga/cal-nav/releases/download/latest/cal-nav_1.0.0_amd64.deb) |

These links always point to the latest build. A new build is published
automatically every time the `main` branch is updated.

## Features

- Day, Week, and Month views
- Create, edit, and delete personal events
- **Weekly recurrence** — when creating an event, tick "Repeat weekly",
  choose which days of the week, and set an end date. All instances are
  saved automatically. Editing a recurring event shows a "Delete series"
  button to remove every instance at once.
- Calendar overlays — Religion and Miami University academic calendar
  (read-only, toggle with the checkboxes under the toolbar)
- All-day items appear in a banner in Day/Week view and as chips in Month view
- Events are stored in `~/.cal-nav/events.json` and survive restarts

## Run from source

Requires JDK 17+ and Maven:

    mvn clean package
    mvn exec:java

Or open in IntelliJ and run `com.juttiga.calendar.Main`.

## Project layout

    src/main/java/com/juttiga/calendar/
      Main.java                     entry point (FlatLaf light theme)
      model/Event.java              event domain object + seriesId for recurrence
      model/TimeSlot.java           free-slot value object
      service/CalendarService.java  add / remove / update / recurring queries
      storage/FileStorage.java      JSON persistence
      ui/CalendarFrame.java         window: toolbar, filters, data provider
      ui/WeekView.java              time grid + all-day banner (Day + Week)
      ui/MonthView.java             month grid with event chips
      ui/EventDialog.java           create / edit / delete / recurrence dialog
      ui/Category.java              calendar layers
      ui/CalItem.java               event + its layer
      ui/OverlayCalendars.java      loads read-only overlays from resources
      ui/CalendarTheme.java         colors + per-event color assignment
    src/main/resources/calendars/   religion.txt, miami.txt
    .github/workflows/build.yml     auto-build and release on push to main
    packaging/                      jpackage scripts per OS
    data/events.json                sample personal events (dev only)
