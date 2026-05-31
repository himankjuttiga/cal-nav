package com.juttiga.calendar.ui;

import java.awt.Color;

/**
 * Calendar layers the user can toggle. PERSONAL is the user's own editable
 * calendar; the others are read-only overlays loaded from bundled data.
 */
enum Category {
    PERSONAL("My Calendar", new Color(0x1A73E8), false, true,  true),
    RELIGION("Religion",    new Color(0x8E24AA), true,  false, false),
    MIAMI   ("Miami University", new Color(0xC3142D), true, false, false);

    final String label;
    final Color  swatch;
    final boolean allDay;
    final boolean editable;
    final boolean titleColored;

    Category(String label, Color swatch, boolean allDay, boolean editable, boolean titleColored) {
        this.label        = label;
        this.swatch       = swatch;
        this.allDay       = allDay;
        this.editable     = editable;
        this.titleColored = titleColored;
    }
}
