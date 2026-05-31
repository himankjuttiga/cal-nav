package com.juttiga.calendar.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a calendar event with a title, start time, and end time.
 * Two events are considered overlapping when their time ranges intersect.
 * Touching boundaries are permitted (one event may end exactly when another begins).
 */
public class Event {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String title;
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Event(String title, LocalDateTime start, LocalDateTime end) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.title = title.trim();
        this.start = start;
        this.end = end;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * Returns true if this event overlaps with the given event.
     * Touching boundaries are not considered overlapping.
     */
    public boolean overlapsWith(Event other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    /**
     * Converts an Event to a single line of text.
     * Escape pipes in the title (replace | with /) because pipe is the delimiter.
     */
    public String toStorageLine() {
        String escapedTitle = title.replace("|", "/");
        return escapedTitle + "|" + start.format(ISO) + "|" + end.format(ISO);
    }

    /**
     * Parses a pipe delimited storage line back into an Event.
     */
    public static Event fromStorageLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed event line: " + line);
        }
        return new Event(
                parts[0],
                LocalDateTime.parse(parts[1], ISO),
                LocalDateTime.parse(parts[2], ISO)
        );
    }

    /**
     * produce a human-readable representation of an Event
     * @return
     */
    @Override
    public String toString() {
        DateTimeFormatter display = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
        return String.format("%s  [%s -> %s]",
                title, start.format(display), end.format(display));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return title.equals(event.title) && start.equals(event.start) && end.equals(event.end);
    }

    /**
     * If two objects are equal according to .equals but produce different hash codes
     * HashMap would store them in different buckets and never find them later.
     * The map would break.
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(title, start, end);
    }
}
