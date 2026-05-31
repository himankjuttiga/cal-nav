package com.juttiga.calendar.ui;

import java.awt.Color;

/**
 * The calendar layers a user can toggle. PERSONAL is the user's own editable
 * calendar; the others are read-only overlays loaded from bundled data.
 */
enum Category {
    PERSONAL("My Calendar", new Color(0x1A73E8), false, true, true),
    SPORTS("Sports", new Color(0x0B8043), false, false, false),
    RELIGION("Religion", new Color(0x8E24AA), true, false, false),
    MIAMI("Miami University", new Color(0xC3142D), true, false, false);

    final String label;
    final Color swatch;     // color shown in the filter checkbox + event blocks
    final boolean allDay;   // rendered in the all-day banner rather than the time grid
    final boolean editable; // can the user create/edit/delete in this layer
    final boolean titleColored; // personal events vary color by title

    Category(String label, Color swatch, boolean allDay, boolean editable, boolean titleColored) {
        this.label = label;
        this.swatch = swatch;
        this.allDay = allDay;
        this.editable = editable;
        this.titleColored = titleColored;
    }
}
