package com.juttiga.calendar.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a calendar event. Recurring events share a seriesId so every
 * instance of a series can be found and deleted together. One-off events have
 * a null seriesId.
 */
public class Event {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String title;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String description;
    private final String location;
    private final String seriesId; // null for one-off events

    public Event(String title, LocalDateTime start, LocalDateTime end) {
        this(title, start, end, null, null, null);
    }

    public Event(String title, LocalDateTime start, LocalDateTime end,
                 String description, String location) {
        this(title, start, end, description, location, null);
    }

    public Event(String title, LocalDateTime start, LocalDateTime end,
                 String description, String location, String seriesId) {
        if (title == null || title.isEmpty())
            throw new IllegalArgumentException("Title cannot be empty");
        if (start == null || end == null)
            throw new IllegalArgumentException("Start and end times are required");
        if (!end.isAfter(start))
            throw new IllegalArgumentException("End time must be after start time");
        this.title       = title.trim();
        this.start       = start;
        this.end         = end;
        this.description = (description == null || description.isBlank()) ? null : description.trim();
        this.location    = (location == null || location.isBlank()) ? null : location.trim();
        this.seriesId    = (seriesId == null || seriesId.isBlank()) ? null : seriesId.trim();
    }

    public String getTitle()       { return title; }
    public LocalDateTime getStart(){ return start; }
    public LocalDateTime getEnd()  { return end; }
    public String getDescription() { return description; }
    public String getLocation()    { return location; }
    public String getSeriesId()    { return seriesId; }
    public boolean isRecurring()   { return seriesId != null; }

    /** Creates a new seriesId UUID string to stamp on all instances of a new series. */
    public static String newSeriesId() {
        return UUID.randomUUID().toString();
    }

    public boolean overlapsWith(Event other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public String toStorageLine() {
        return title.replace("|", "/") + "|" + start.format(ISO) + "|" + end.format(ISO);
    }

    public static Event fromStorageLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length != 3)
            throw new IllegalArgumentException("Malformed event line: " + line);
        return new Event(parts[0],
                LocalDateTime.parse(parts[1], ISO),
                LocalDateTime.parse(parts[2], ISO));
    }

    @Override
    public String toString() {
        DateTimeFormatter display = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
        return String.format("%s  [%s -> %s]", title, start.format(display), end.format(display));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return title.equals(event.title) && start.equals(event.start) && end.equals(event.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, start, end);
    }
}
