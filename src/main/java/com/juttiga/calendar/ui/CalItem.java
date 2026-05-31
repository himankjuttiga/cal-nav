package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;

import java.awt.Color;

/**
 * An event together with the calendar layer it belongs to. Carries the helpers
 * the views need: what color to draw it, whether it is all-day, and whether the
 * user may edit it.
 */
final class CalItem {

    final Event event;
    final Category category;

    CalItem(Event event, Category category) {
        this.event = event;
        this.category = category;
    }

    Color color() {
        return category.titleColored ? CalendarTheme.colorFor(event) : category.swatch;
    }

    boolean allDay() {
        return category.allDay;
    }

    boolean editable() {
        return category.editable;
    }
}
