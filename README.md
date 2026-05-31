# cal-nav

A Java Swing calendar with Google / Outlook style Day, Week, and Month views.
Click an empty area to create an event, click an event to edit or delete it.
Times are shown and entered in AM/PM. Overlaps between your own events are
prevented; every change is saved automatically.

## Calendar layers (filters)

Four toggleable layers, controlled by the checkboxes under the toolbar:

- My Calendar  - your own editable events (blue, varied per event)
- Sports       - sample/marquee sporting events (green, read-only)
- Religion     - major interfaith holidays 2026-2027 (purple, read-only)
- Miami University - official 2026-2027 academic calendar (red, read-only)

Only My Calendar shows by default, so the overlays never clutter or block your
schedule. Tick a box to overlay that layer; tick several to see them together.
All-day items (holidays, academic dates) appear in a banner under the day
headers in Day/Week view and as chips in Month view. Overlay items are
read-only: clicking one shows its details rather than an edit dialog.

The overlay data lives in `src/main/resources/calendars/` (sports.txt,
religion.txt, miami.txt) and is bundled into the app. Sports entries are
sample/approximate placeholders; replace them with your team's real schedule.
Miami and religious dates are real, though lunar holiday dates can vary by a day.

## Run from source

Requires JDK 17+ and Maven. From this folder:

    mvn clean package
    mvn exec:java

Or run com.juttiga.calendar.Main from IntelliJ after the Maven import.

## Give it to other people

`mvn clean package` produces a single runnable `target/cal-nav.jar`
(run with `java -jar cal-nav.jar`). For native installers that need no Java,
see DISTRIBUTING.md and the scripts in packaging/.

## Data

Your personal events are stored per user at `~/.cal-nav/events.txt`. New users
start empty. A sample personal data set lives in data/events.txt; load it in
development with `mvn exec:java -Dexec.args="data/events.txt"`.

## Layout

    src/main/java/com/juttiga/calendar/
      Main.java                 entry point (FlatLaf light theme)
      model/Event.java          event domain object + overlap rule
      model/TimeSlot.java       free-slot value object
      service/CalendarService.java  add / remove / update / queries
      storage/FileStorage.java  plain-text persistence
      ui/CalendarFrame.java     window: toolbar, filters, data provider
      ui/WeekView.java          time grid + all-day banner (Day + Week)
      ui/MonthView.java         month grid with event chips
      ui/EventDialog.java       create / edit / delete popup (AM/PM)
      ui/Category.java          the four calendar layers
      ui/CalItem.java           event + its layer
      ui/OverlayCalendars.java  loads the read-only layers from resources
      ui/CalendarData.java      per-day item provider interface
      ui/CalendarActions.java   view-to-frame callback interface
      ui/CalendarTheme.java     colors + per-event color assignment
    src/main/resources/calendars/  sports.txt, religion.txt, miami.txt
    packaging/                  jpackage build scripts per OS
    data/events.txt             sample personal events
