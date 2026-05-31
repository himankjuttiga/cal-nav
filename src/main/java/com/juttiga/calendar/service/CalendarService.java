package com.juttiga.calendar.service;

import com.juttiga.calendar.model.Event;
import com.juttiga.calendar.model.TimeSlot;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Core service for managing calendar events.
 * Events are stored in a TreeMap keyed by start time for O(log n) insertion
 * and naturally sorted iteration.
 */

public class CalendarService {

    // Default working hours used by the next available slot feature
    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DAY_END = LocalTime.of(17, 0);

    private final TreeMap<LocalDateTime, Event> events = new TreeMap<>();

    /**
     * Adds an event to the calendar.
     * @throws IllegalStateException if the event overlaps with an existing event
     */
    public void addEvent(Event event) {
        for (Event existing : events.values()) {
            if (event.overlapsWith(existing)) {
                throw new IllegalStateException(
                        "Event overlaps with existing event: " + existing);
            }
        }
        events.put(event.getStart(), event);
    }


    /**
     * Removes the given event from the calendar.
     * @throws IllegalStateException if the event is not found
     */
    public void removeEvent(Event event) {
        Event existing = events.get(event.getStart());
        if (existing == null || !existing.equals(event)) {
            throw new IllegalStateException("Event not found: " + event);
        }
        events.remove(event.getStart());
    }

    /**
     * Replaces an existing event with an updated one. The change is atomic:
     * if the new event would overlap another event the original is restored.
     * @throws IllegalStateException if oldEvent is not found or newEvent overlaps
     */
    public void updateEvent(Event oldEvent, Event newEvent) {
        removeEvent(oldEvent);
        try {
            addEvent(newEvent);
        } catch (RuntimeException ex) {
            addEvent(oldEvent); // rollback, original slot is guaranteed free
            throw ex;
        }
    }

    /**
     * Returns all events scheduled on the given date, sorted by start time.
     */
    public List<Event> getEventsForDay(LocalDate day) {
        List<Event> result = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getStart().toLocalDate().equals(day)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Returns events on the given date that have not yet started, relative to "now".
     */
    public List<Event> getRemainingEventsForDay(LocalDate day, LocalDateTime now) {
        List<Event> result = new ArrayList<>();
        for (Event event : getEventsForDay(day)) {
            if (event.getStart().isAfter(now)) {
                result.add(event);
            }
        }
        return result;
    }

    /**
     * Finds the next available time slot of the requested duration on the given day.
     * Searches between DAY_START and DAY_END. Returns null if no slot fits.
     *
     * @param day the day to search
     * @param duration minimum size of the slot
     * @param from the earliest acceptable start (use day start if scanning the whole day)
     */
    public TimeSlot findNextAvailableSlot(LocalDate day, Duration duration, LocalDateTime from) {
        LocalDateTime dayStart = day.atTime(DAY_START);
        LocalDateTime dayEnd = day.atTime(DAY_END);

        // Cursor starts at the later of dayStart or "from"
        LocalDateTime cursor = from.isAfter(dayStart) ? from : dayStart;

        if (!cursor.isBefore(dayEnd)) {
            return null;
        }

        List<Event> dayEvents = getEventsForDay(day);

        for (Event event : dayEvents) {
            // Skip events that end before our cursor
            if (!event.getEnd().isAfter(cursor)) {
                continue;
            }
            // Gap exists between cursor and this event's start
            if (event.getStart().isAfter(cursor)) {
                TimeSlot gap = new TimeSlot(cursor, event.getStart());
                if (gap.canFit(duration)) {
                    return new TimeSlot(cursor, cursor.plus(duration));
                }
            }
            // Move cursor past this event
            if (event.getEnd().isAfter(cursor)) {
                cursor = event.getEnd();
            }
        }

        // Check the tail of the day after the last event
        if (cursor.isBefore(dayEnd)) {
            TimeSlot tail = new TimeSlot(cursor, dayEnd);
            if (tail.canFit(duration)) {
                return new TimeSlot(cursor, cursor.plus(duration));
            }
        }

        return null;
    }

    /**
     * Returns all events currently stored, sorted by start time.
     */
    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    /**
     * Replaces all events in the service with the given list.
     * Used by the storage layer on load.
     */
    public void loadEvents(List<Event> loaded) {
        events.clear();
        for (Event event : loaded) {
            events.put(event.getStart(), event);
        }
    }

    public int size() {
        return events.size();
    }
}
