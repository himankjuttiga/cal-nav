package com.juttiga.calendar.ui;

import java.time.LocalDate;
import java.util.List;

/**
 * Supplies the visible items for a given day, combining the user's personal
 * events with whichever overlay layers are currently enabled by the filters.
 */
interface CalendarData {
    List<CalItem> itemsFor(LocalDate date);
}
