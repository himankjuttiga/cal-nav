package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;

import java.awt.Color;

/**
 * An event together with the calendar layer it belongs to. Carries the helpers
 * the views need: what color to draw it, whether it is all-day, and whether
 * the user may edit it.
 *
 * Color priority: event-level custom color > category swatch > palette hash.
 */
final class CalItem {

    final Event event;
    final Category category;

    CalItem(Event event, Category category) {
        this.event    = event;
        this.category = category;
    }

    Color color() {
        // 1. Event has its own explicit color
        if (event.getCategoryColor() != null) return event.getCategoryColor();
        // 2. Category is a user-defined one whose swatch IS the color
        if (!category.titleColored)           return category.swatch;
        // 3. Fall back to palette hash (PERSONAL default)
        return CalendarTheme.colorFor(event);
    }

    boolean allDay() {
        return event.isAllDay() || category.allDay;
    }

    boolean editable() {
        return category.editable;
    }
}
