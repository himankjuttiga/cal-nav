package com.juttiga.calendar.ui;

import java.time.LocalDateTime;

/**
 * Lets the grid views ask the frame to act on a click without knowing how the
 * frame is built. The frame decides whether an item is editable or read-only.
 */
interface CalendarActions {

    /** User clicked an empty area; offer to create a personal event here. */
    void createEventAt(LocalDateTime start);

    /** User clicked an item; edit it if personal, otherwise show its details. */
    void openItem(CalItem item);
}
