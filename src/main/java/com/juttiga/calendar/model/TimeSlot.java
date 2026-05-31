package com.juttiga.calendar.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a free time slot in the calendar.
 */
public class TimeSlot {

    private final LocalDateTime start;
    private final LocalDateTime end;

    public TimeSlot(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Duration getDuration() {
        return Duration.between(start, end);
    }

    /**
     If slot is 60 min and requested is 30 min,
     result is 30 min (not negative) -- fits.
     If slot is 20 min and requested is 30 min,
     result is -10 min (negative) -- doesn't fit.
     */
    public boolean canFit(Duration duration) {
        return !getDuration().minus(duration).isNegative();
    }

    @Override
    public String toString() {
        DateTimeFormatter display = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");
        long minutes = getDuration().toMinutes();
        return String.format("%s -> %s  (%d minutes)",
                start.format(display), end.format(display), minutes);
    }
}
