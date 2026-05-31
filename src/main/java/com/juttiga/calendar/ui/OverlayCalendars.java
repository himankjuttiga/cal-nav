package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the read-only overlay calendars (Sports, Religion, Miami). For each
 * layer it first looks for a user file at ~/.cal-nav/&lt;name&gt; (so a refreshed
 * sports file from SportsImporter is used without rebuilding) and otherwise
 * falls back to the sample bundled on the classpath under /calendars.
 *
 * Timed layers use ISO date-times; all-day layers use date ranges
 * (title|startDate|endDate), expanded into one entry per covered day so
 * multi-day spans show on every day they cover.
 */
class OverlayCalendars {

    private final Map<Category, List<Event>> byCategory = new EnumMap<>(Category.class);

    OverlayCalendars() {
        load(Category.SPORTS, "sports.txt");
        load(Category.RELIGION, "religion.txt");
        load(Category.MIAMI, "miami.txt");
    }

    /** Events in the given layer that fall on the given day. */
    List<Event> eventsFor(Category category, LocalDate date) {
        List<Event> result = new ArrayList<>();
        for (Event e : byCategory.getOrDefault(category, List.of())) {
            if (e.getStart().toLocalDate().equals(date)) {
                result.add(e);
            }
        }
        return result;
    }

    private void load(Category category, String fileName) {
        List<Event> events = new ArrayList<>();
        for (String line : readLines(fileName)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            try {
                if (category.allDay) {
                    parseAllDay(trimmed, events);
                } else {
                    events.add(Event.fromStorageLine(trimmed));
                }
            } catch (Exception ex) {
                System.err.println("Skipping bad line in " + fileName + ": " + line);
            }
        }
        byCategory.put(category, events);
    }

    /** Reads from the user override file if present, else the bundled resource. */
    private List<String> readLines(String fileName) {
        Path override = Paths.get(System.getProperty("user.home"), ".cal-nav", fileName);
        try {
            if (Files.exists(override)) {
                return Files.readAllLines(override, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            System.err.println("Could not read " + override + ": " + ex.getMessage());
        }
        try (InputStream in = getClass().getResourceAsStream("/calendars/" + fileName)) {
            if (in == null) {
                System.err.println("Calendar resource not found: " + fileName);
                return List.of();
            }
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (Exception ex) {
            System.err.println("Failed to load " + fileName + ": " + ex.getMessage());
            return List.of();
        }
    }

    /** Expands title|startDate|endDate into one all-day Event per covered day. */
    private void parseAllDay(String line, List<Event> out) {
        String[] parts = line.split("\\|");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected title|startDate|endDate");
        }
        String title = parts[0];
        LocalDate start = LocalDate.parse(parts[1]);
        LocalDate end = LocalDate.parse(parts[2]);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.add(new Event(title, d.atStartOfDay(), d.atTime(23, 59)));
        }
    }
}
