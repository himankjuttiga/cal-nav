package com.juttiga.calendar.model;

import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a calendar event. Supports:
 *   - Timed events (start/end datetime)
 *   - All-day events (allDay flag, stored with midnight start/end on same date)
 *   - Custom category label and color per event
 *   - Recurring series via seriesId
 */
public class Event {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String title;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final String description;
    private final String location;
    private final String seriesId;
    private final boolean allDay;
    private final String categoryLabel; // custom category name, may be null
    private final Color  categoryColor; // custom color, may be null

    // Full constructor
    public Event(String title, LocalDateTime start, LocalDateTime end,
                 String description, String location, String seriesId,
                 boolean allDay, String categoryLabel, Color categoryColor) {
        if (title == null || title.isEmpty())
            throw new IllegalArgumentException("Title cannot be empty");
        if (start == null || end == null)
            throw new IllegalArgumentException("Start and end times are required");
        if (!allDay && !end.isAfter(start))
            throw new IllegalArgumentException("End time must be after start time");
        this.title         = title.trim();
        this.start         = start;
        this.end           = end;
        this.description   = (description == null || description.isBlank()) ? null : description.trim();
        this.location      = (location == null || location.isBlank()) ? null : location.trim();
        this.seriesId      = (seriesId == null || seriesId.isBlank()) ? null : seriesId.trim();
        this.allDay        = allDay;
        this.categoryLabel = (categoryLabel == null || categoryLabel.isBlank()) ? null : categoryLabel.trim();
        this.categoryColor = categoryColor;
    }

    // Backwards-compat constructors
    public Event(String title, LocalDateTime start, LocalDateTime end) {
        this(title, start, end, null, null, null, false, null, null);
    }

    public Event(String title, LocalDateTime start, LocalDateTime end,
                 String description, String location) {
        this(title, start, end, description, location, null, false, null, null);
    }

    public Event(String title, LocalDateTime start, LocalDateTime end,
                 String description, String location, String seriesId) {
        this(title, start, end, description, location, seriesId, false, null, null);
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
    public String getDescription() {
        return description;
    }
    public String getLocation() {
        return location;
    }
    public String getSeriesId() {
        return seriesId;
    }
    public boolean isRecurring() {
        return seriesId != null;
    }
    public boolean isAllDay() {
        return allDay;
    }
    public String getCategoryLabel() {
        return categoryLabel;
    }
    public Color getCategoryColor() {
        return categoryColor;
    }

    public static String newSeriesId() {
        return UUID.randomUUID().toString();
    }

    public boolean overlapsWith(Event other) {
        if (this.allDay || other.allDay) return false; // all-day events don't block timed slots
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    /** Storage format: title|start|end|allDay|categoryLabel|categoryColorRGB */
    public String toStorageLine() {
        String cat = categoryLabel != null ? categoryLabel.replace("|", "/") : "";
        String col = categoryColor != null ? Integer.toHexString(categoryColor.getRGB() & 0xFFFFFF) : "";
        return title.replace("|", "/") + "|" + start.format(ISO) + "|" + end.format(ISO)
                + "|" + allDay + "|" + cat + "|" + col;
    }

    public static Event fromStorageLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3)
            throw new IllegalArgumentException("Malformed event line: " + line);
        boolean ad  = parts.length > 3 && "true".equals(parts[3]);
        String  cat = parts.length > 4 ? parts[4] : "";
        Color   col = null;
        if (parts.length > 5 && !parts[5].isBlank()) {
            try { col = new Color(Integer.parseUnsignedInt(parts[5], 16)); } catch (NumberFormatException ignored) {}
        }
        return new Event(parts[0],
                LocalDateTime.parse(parts[1], ISO),
                LocalDateTime.parse(parts[2], ISO),
                null, null, null, ad,
                cat.isBlank() ? null : cat, col);
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
