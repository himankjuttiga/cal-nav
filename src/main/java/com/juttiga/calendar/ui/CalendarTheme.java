package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;

import java.awt.Color;

/**
 * Central place for the calendar's visual theme. Colors are chosen to echo the
 * light, airy look of Google Calendar and Outlook on the web.
 */
final class CalendarTheme {

    private CalendarTheme() {
    }

    // Surfaces
    static final Color SURFACE = new Color(0xFFFFFF);
    static final Color TOOLBAR = new Color(0xFFFFFF);
    static final Color GRID_LINE = new Color(0xE7EAED);
    static final Color GRID_LINE_STRONG = new Color(0xDADCE0);

    // Text
    static final Color TEXT_PRIMARY = new Color(0x3C4043);
    static final Color TEXT_MUTED = new Color(0x70757A);
    static final Color ON_ACCENT = Color.WHITE;

    // Accents
    static final Color ACCENT = new Color(0x1A73E8);   // Google blue
    static final Color NOW_LINE = new Color(0xEA4335);  // red current-time marker
    static final Color TODAY_BG = new Color(0xE8F0FE);  // soft blue wash for today

    // A small, friendly palette for personal event blocks.
    // Green (0x0B8043) and purple (0x8E24AA) are intentionally excluded here
    // because they are reserved for the Sports and Religion overlay swatches —
    // keeping them out prevents personal events from looking like overlay items.
    private static final Color[] EVENT_PALETTE = {
            new Color(0x1A73E8), // blue
            new Color(0xD93025), // red
            new Color(0xF09300), // amber
            new Color(0x009688), // teal
            new Color(0xE8710A), // orange
            new Color(0x3949AB), // indigo
            new Color(0xC2185B), // pink
            new Color(0x00838F), // cyan
            new Color(0x039BE5), // light blue
            new Color(0xAD1457), // rose
    };

    /**
     * Picks a stable color for an event based on its title, so the same event
     * always renders in the same color across sessions.
     */
    static Color colorFor(Event event) {
        int idx = Math.floorMod(event.getTitle().hashCode(), EVENT_PALETTE.length);
        return EVENT_PALETTE[idx];
    }

    /** A lightened tint of a base color, used for event block fills. */
    static Color tint(Color base, double amount) {
        int r = (int) Math.round(base.getRed() + (255 - base.getRed()) * amount);
        int g = (int) Math.round(base.getGreen() + (255 - base.getGreen()) * amount);
        int b = (int) Math.round(base.getBlue() + (255 - base.getBlue()) * amount);
        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
