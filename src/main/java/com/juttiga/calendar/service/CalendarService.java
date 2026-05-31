package com.juttiga.calendar.service;

import com.juttiga.calendar.model.Event;
import com.juttiga.calendar.model.TimeSlot;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class CalendarService {

    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DAY_END   = LocalTime.of(17, 0);

    // Keyed by start time; duplicate starts are illegal (same slot = overlap).
    private final TreeMap<LocalDateTime, Event> events = new TreeMap<>();

    // ----------------------------------------------------------------
    // Single event CRUD
    // ----------------------------------------------------------------

    public void addEvent(Event event) {
        for (Event existing : events.values()) {
            if (event.overlapsWith(existing))
                throw new IllegalStateException("Event overlaps with: " + existing);
        }
        events.put(event.getStart(), event);
    }

    public void removeEvent(Event event) {
        Event existing = events.get(event.getStart());
        if (existing == null || !existing.equals(event))
            throw new IllegalStateException("Event not found: " + event);
        events.remove(event.getStart());
    }

    public void updateEvent(Event oldEvent, Event newEvent) {
        removeEvent(oldEvent);
        try {
            addEvent(newEvent);
        } catch (RuntimeException ex) {
            addEvent(oldEvent);
            throw ex;
        }
    }

    // ----------------------------------------------------------------
    // Recurring events
    // ----------------------------------------------------------------

    /**
     * Creates one event instance per occurrence of the given days-of-week
     * from startDate through repeatUntil (inclusive). All instances share the
     * same seriesId so they can be deleted as a group.
     *
     * Instances that would overlap an existing event are skipped with a
     * warning rather than aborting the whole series.
     *
     * @param title        event title
     * @param startTime    time-of-day the event begins
     * @param endTime      time-of-day the event ends
     * @param description  optional notes
     * @param location     optional location
     * @param startDate    first possible date
     * @param repeatUntil  last possible date (inclusive)
     * @param repeatDays   which days of the week to repeat on
     * @return number of instances actually added
     */
    public int addRecurringEvents(String title,
                                   LocalTime startTime,
                                   LocalTime endTime,
                                   String description,
                                   String location,
                                   LocalDate startDate,
                                   LocalDate repeatUntil,
                                   Set<DayOfWeek> repeatDays) {
        if (repeatDays == null || repeatDays.isEmpty())
            throw new IllegalArgumentException("At least one repeat day must be selected");
        if (repeatUntil.isBefore(startDate))
            throw new IllegalArgumentException("Repeat-until date must be on or after the start date");

        String seriesId = Event.newSeriesId();
        int added = 0;

        for (LocalDate d = startDate; !d.isAfter(repeatUntil); d = d.plusDays(1)) {
            if (!repeatDays.contains(d.getDayOfWeek())) continue;
            LocalDateTime s = d.atTime(startTime);
            LocalDateTime e = d.atTime(endTime);
            // If end is before or equal to start (e.g. cross-midnight), push end to next day
            if (!e.isAfter(s)) e = e.plusDays(1);
            Event instance = new Event(title, s, e, description, location, seriesId);
            try {
                addEvent(instance);
                added++;
            } catch (IllegalStateException ex) {
                System.err.println("Skipping overlap on " + d + ": " + ex.getMessage());
            }
        }
        return added;
    }

    /**
     * Removes every event that shares the given seriesId.
     * @return number of instances removed
     */
    public int removeSeries(String seriesId) {
        if (seriesId == null) return 0;
        List<Event> toRemove = new ArrayList<>();
        for (Event e : events.values()) {
            if (seriesId.equals(e.getSeriesId())) toRemove.add(e);
        }
        for (Event e : toRemove) events.remove(e.getStart());
        return toRemove.size();
    }

    // ----------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------

    public List<Event> getEventsForDay(LocalDate day) {
        List<Event> result = new ArrayList<>();
        for (Event e : events.values())
            if (e.getStart().toLocalDate().equals(day)) result.add(e);
        return result;
    }

    public List<Event> getRemainingEventsForDay(LocalDate day, LocalDateTime now) {
        List<Event> result = new ArrayList<>();
        for (Event e : getEventsForDay(day))
            if (e.getStart().isAfter(now)) result.add(e);
        return result;
    }

    public TimeSlot findNextAvailableSlot(LocalDate day, Duration duration, LocalDateTime from) {
        LocalDateTime dayStart = day.atTime(DAY_START);
        LocalDateTime dayEnd   = day.atTime(DAY_END);
        LocalDateTime cursor   = from.isAfter(dayStart) ? from : dayStart;
        if (!cursor.isBefore(dayEnd)) return null;

        for (Event event : getEventsForDay(day)) {
            if (!event.getEnd().isAfter(cursor)) continue;
            if (event.getStart().isAfter(cursor)) {
                TimeSlot gap = new TimeSlot(cursor, event.getStart());
                if (gap.canFit(duration)) return new TimeSlot(cursor, cursor.plus(duration));
            }
            if (event.getEnd().isAfter(cursor)) cursor = event.getEnd();
        }
        if (cursor.isBefore(dayEnd)) {
            TimeSlot tail = new TimeSlot(cursor, dayEnd);
            if (tail.canFit(duration)) return new TimeSlot(cursor, cursor.plus(duration));
        }
        return null;
    }

    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    public void loadEvents(List<Event> loaded) {
        events.clear();
        for (Event e : loaded) events.put(e.getStart(), e);
    }

    public int size() { return events.size(); }
}
