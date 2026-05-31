package com.juttiga.calendar.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Month grid: six rows of seven day cells, each listing that day's items as
 * small chips across every enabled layer. Clicking a personal chip edits it,
 * clicking an overlay chip shows its details, and clicking empty space in a
 * cell starts a new personal event on that day.
 */
class MonthView extends JPanel {

    private static final int HEADER_H = 26;
    private static final String[] DOW = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private final CalendarData data;
    private final CalendarActions actions;
    private LocalDate anchor;

    private final Map<Rectangle, CalItem> chipBoxes = new HashMap<>();
    private final Map<Rectangle, LocalDate> cellBoxes = new HashMap<>();

    MonthView(CalendarData data, LocalDate anchor, CalendarActions actions) {
        this.data = data;
        this.anchor = anchor;
        this.actions = actions;
        setBackground(CalendarTheme.SURFACE);
        MouseHandler mh = new MouseHandler();
        addMouseListener(mh);
        addMouseMotionListener(mh);
    }

    void setAnchor(LocalDate anchor) { this.anchor = anchor; repaint(); }
    void refresh() { repaint(); }

    private LocalDate gridStart() {
        LocalDate first = anchor.withDayOfMonth(1);
        return first.minusDays(first.getDayOfWeek().getValue() % 7);
    }

    private List<CalItem> itemsSorted(LocalDate date) {
        List<CalItem> items = new ArrayList<>(data.itemsFor(date));
        items.sort((a, b) -> {
            if (a.allDay() != b.allDay()) return a.allDay() ? -1 : 1; // all-day first
            return a.event.getStart().compareTo(b.event.getStart());
        });
        return items;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        chipBoxes.clear();
        cellBoxes.clear();
        int w = getWidth();
        int h = getHeight();
        int cellW = w / 7;
        int cellH = (h - HEADER_H) / 6;

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(CalendarTheme.TEXT_MUTED);
        for (int c = 0; c < 7; c++) {
            String s = DOW[c];
            g2.drawString(s, c * cellW + cellW - g2.getFontMetrics().stringWidth(s) - 8, 17);
        }

        LocalDate start = gridStart();
        LocalDate today = LocalDate.now();
        int thisMonth = anchor.getMonthValue();
        int maxChips = Math.max(1, (cellH - 26) / 18);

        for (int idx = 0; idx < 42; idx++) {
            int row = idx / 7, col = idx % 7;
            LocalDate date = start.plusDays(idx);
            int x = col * cellW, y = HEADER_H + row * cellH;
            boolean inMonth = date.getMonthValue() == thisMonth;
            boolean isToday = date.equals(today);

            if (!inMonth) { g2.setColor(new Color(0xF6F7F9)); g2.fillRect(x, y, cellW, cellH); }
            g2.setColor(CalendarTheme.GRID_LINE);
            g2.drawRect(x, y, cellW, cellH);
            cellBoxes.put(new Rectangle(x, y, cellW, cellH), date);

            String num = String.valueOf(date.getDayOfMonth());
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            int nw = g2.getFontMetrics().stringWidth(num);
            if (isToday) {
                g2.setColor(CalendarTheme.ACCENT);
                g2.fillOval(x + 6, y + 5, 22, 20);
                g2.setColor(CalendarTheme.ON_ACCENT);
                g2.drawString(num, x + 6 + (22 - nw) / 2, y + 19);
            } else {
                g2.setColor(inMonth ? CalendarTheme.TEXT_PRIMARY : CalendarTheme.TEXT_MUTED);
                g2.drawString(num, x + 8, y + 19);
            }

            List<CalItem> items = itemsSorted(date);
            int shown = Math.min(items.size(), maxChips);
            for (int k = 0; k < shown; k++) {
                CalItem item = items.get(k);
                int chipY = y + 28 + k * 18, chipX = x + 5, chipW = cellW - 10;
                Color base = item.color();
                chipBoxes.put(new Rectangle(chipX, chipY, chipW, 15), item);
                g2.setColor(CalendarTheme.tint(base, 0.82));
                g2.fillRoundRect(chipX, chipY, chipW, 15, 6, 6);
                g2.setColor(base);
                g2.fillRoundRect(chipX, chipY, 3, 15, 3, 3);
                Shape clip = g2.getClip();
                g2.setClip(chipX + 5, chipY, chipW - 7, 15);
                g2.setColor(base.darker());
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.drawString(item.event.getTitle(), chipX + 6, chipY + 11);
                g2.setClip(clip);
            }
            if (items.size() > shown) {
                g2.setColor(CalendarTheme.TEXT_MUTED);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.drawString("+" + (items.size() - shown) + " more", x + 8, y + 28 + shown * 18 + 10);
            }
        }
        g2.dispose();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 600); }

    private class MouseHandler extends java.awt.event.MouseAdapter {
        @Override public void mousePressed(java.awt.event.MouseEvent ev) {
            for (Map.Entry<Rectangle, CalItem> e : chipBoxes.entrySet()) {
                if (e.getKey().contains(ev.getPoint())) { actions.openItem(e.getValue()); return; }
            }
            for (Map.Entry<Rectangle, LocalDate> e : cellBoxes.entrySet()) {
                if (e.getKey().contains(ev.getPoint())) { actions.createEventAt(e.getValue().atTime(9, 0)); return; }
            }
        }
        @Override public void mouseMoved(java.awt.event.MouseEvent ev) {
            boolean over = false;
            for (Rectangle r : chipBoxes.keySet()) if (r.contains(ev.getPoint())) { over = true; break; }
            setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }
    }
}
