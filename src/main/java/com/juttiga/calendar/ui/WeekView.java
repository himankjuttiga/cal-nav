package com.juttiga.calendar.ui;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Time-grid view for one or more days. Timed items render as colored blocks in
 * the scrollable hour grid; all-day items (holidays, academic dates) render as
 * chips in a fixed banner under the day headers, the way Google Calendar shows
 * them. Used for both the Day view (one column) and Week view (seven columns).
 */
class WeekView extends JPanel {

    private static final int HOUR_HEIGHT = 44;
    private static final int TIME_COL_W = 64;
    private static final int LABEL_H = 56;        // day-of-week + date row
    private static final int ALLDAY_ROW_H = 17;   // height of one all-day chip row
    private static final int DAY_MINUTES = 24 * 60;

    private static final DateTimeFormatter TIME_LABEL =
            DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final CalendarData data;
    private final CalendarActions actions;
    private List<LocalDate> days;
    private int allDayRows = 0;

    private final Grid grid = new Grid();
    private final DayHeader header = new DayHeader();
    private final JScrollPane scroll;

    WeekView(CalendarData data, List<LocalDate> days, CalendarActions actions) {
        super(new BorderLayout());
        this.data = data;
        this.days = days;
        this.actions = actions;
        setBackground(CalendarTheme.SURFACE);

        scroll = new JScrollPane(grid);
        scroll.setColumnHeaderView(header);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        recomputeAllDayRows();
        SwingUtilities.invokeLater(() ->
                scroll.getViewport().setViewPosition(new Point(0, 7 * HOUR_HEIGHT)));
    }

    void setDays(List<LocalDate> days) {
        this.days = days;
        recomputeAllDayRows();
        refresh();
    }

    void refresh() {
        recomputeAllDayRows();
        header.revalidate();
        header.repaint();
        grid.repaint();
    }

    private void recomputeAllDayRows() {
        int max = 0;
        for (LocalDate d : days) {
            int count = 0;
            for (CalItem it : data.itemsFor(d)) if (it.allDay()) count++;
            max = Math.max(max, count);
        }
        allDayRows = max;
    }

    private int dayWidth(int totalWidth) {
        return Math.max(40, (totalWidth - TIME_COL_W) / Math.max(1, days.size()));
    }

    private int headerHeight() {
        return LABEL_H + (allDayRows > 0 ? allDayRows * ALLDAY_ROW_H + 6 : 0);
    }

    private static int minuteOfDay(LocalDateTime dt, LocalDate column) {
        if (dt.toLocalDate().isBefore(column)) return 0;
        if (dt.toLocalDate().isAfter(column)) return DAY_MINUTES;
        LocalTime t = dt.toLocalTime();
        return t.getHour() * 60 + t.getMinute();
    }

    private static int yForMinute(int minute) {
        return (int) Math.round(minute / 60.0 * HOUR_HEIGHT);
    }

    private List<CalItem> timedItems(LocalDate date) {
        List<CalItem> list = new ArrayList<>();
        for (CalItem it : data.itemsFor(date)) if (!it.allDay()) list.add(it);
        list.sort((a, b) -> a.event.getStart().compareTo(b.event.getStart()));
        return list;
    }

    // ============================================================
    // Scrollable time grid
    // ============================================================
    private class Grid extends JPanel implements Scrollable {

        private final Map<Rectangle, CalItem> hitBoxes = new HashMap<>();

        Grid() {
            setBackground(CalendarTheme.SURFACE);
            MouseHandler mh = new MouseHandler();
            addMouseListener(mh);
            addMouseMotionListener(mh);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int dayW = dayWidth(w);
            LocalDate today = LocalDate.now();

            for (int d = 0; d < days.size(); d++) {
                if (days.get(d).equals(today)) {
                    g2.setColor(CalendarTheme.TODAY_BG);
                    g2.fillRect(TIME_COL_W + d * dayW, 0, dayW, 24 * HOUR_HEIGHT);
                }
            }

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            for (int h = 0; h <= 24; h++) {
                int y = h * HOUR_HEIGHT;
                g2.setColor(CalendarTheme.GRID_LINE);
                g2.drawLine(TIME_COL_W, y, w, y);
                if (h > 0 && h < 24) {
                    g2.drawLine(TIME_COL_W, y - HOUR_HEIGHT / 2, w, y - HOUR_HEIGHT / 2);
                    g2.setColor(CalendarTheme.TEXT_MUTED);
                    String label = hourLabel(h);
                    int tw = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, TIME_COL_W - tw - 8, y + 4);
                }
            }

            g2.setColor(CalendarTheme.GRID_LINE_STRONG);
            for (int d = 0; d <= days.size(); d++) {
                int x = TIME_COL_W + d * dayW;
                g2.drawLine(x, 0, x, 24 * HOUR_HEIGHT);
            }

            hitBoxes.clear();
            for (int d = 0; d < days.size(); d++) {
                drawDay(g2, days.get(d), TIME_COL_W + d * dayW, dayW);
            }

            for (int d = 0; d < days.size(); d++) {
                if (days.get(d).equals(today)) {
                    int nowMin = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
                    int y = yForMinute(nowMin);
                    int x = TIME_COL_W + d * dayW;
                    g2.setColor(CalendarTheme.NOW_LINE);
                    g2.fillOval(x - 4, y - 4, 8, 8);
                    g2.drawLine(x, y, x + dayW, y);
                }
            }
            g2.dispose();
        }

        private void drawDay(Graphics2D g2, LocalDate date, int dayX, int dayW) {
            List<CalItem> items = timedItems(date);
            int n = items.size();
            int i = 0;
            while (i < n) {
                List<CalItem> cluster = new ArrayList<>();
                cluster.add(items.get(i));
                LocalDateTime clusterEnd = items.get(i).event.getEnd();
                int j = i + 1;
                while (j < n && items.get(j).event.getStart().isBefore(clusterEnd)) {
                    cluster.add(items.get(j));
                    if (items.get(j).event.getEnd().isAfter(clusterEnd)) {
                        clusterEnd = items.get(j).event.getEnd();
                    }
                    j++;
                }
                drawCluster(g2, cluster, date, dayX, dayW);
                i = j;
            }
        }

        private void drawCluster(Graphics2D g2, List<CalItem> cluster, LocalDate date,
                                 int dayX, int dayW) {
            List<LocalDateTime> laneEnd = new ArrayList<>();
            int[] laneOf = new int[cluster.size()];
            for (int k = 0; k < cluster.size(); k++) {
                LocalDateTime s = cluster.get(k).event.getStart();
                int lane = -1;
                for (int l = 0; l < laneEnd.size(); l++) {
                    if (!s.isBefore(laneEnd.get(l))) {
                        lane = l;
                        laneEnd.set(l, cluster.get(k).event.getEnd());
                        break;
                    }
                }
                if (lane == -1) {
                    lane = laneEnd.size();
                    laneEnd.add(cluster.get(k).event.getEnd());
                }
                laneOf[k] = lane;
            }
            int cols = laneEnd.size();
            int colW = (dayW - 4) / cols;

            for (int k = 0; k < cluster.size(); k++) {
                CalItem item = cluster.get(k);
                int yTop = yForMinute(minuteOfDay(item.event.getStart(), date));
                int yBot = yForMinute(minuteOfDay(item.event.getEnd(), date));
                int h = Math.max(20, yBot - yTop);
                int x = dayX + 2 + laneOf[k] * colW;
                int blockW = colW - 3;
                Color base = item.color();

                hitBoxes.put(new Rectangle(x, yTop, blockW, h), item);

                g2.setColor(CalendarTheme.tint(base, 0.85));
                g2.fillRoundRect(x, yTop, blockW, h, 8, 8);
                g2.setColor(base);
                g2.fillRoundRect(x, yTop, 4, h, 4, 4);

                Shape oldClip = g2.getClip();
                g2.setClip(x + 6, yTop, blockW - 8, h);
                g2.setColor(base.darker());
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(item.event.getTitle(), x + 8, yTop + 15);
                if (h > 30) {
                    g2.setColor(CalendarTheme.TEXT_MUTED);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    g2.drawString(item.event.getStart().toLocalTime().format(TIME_LABEL),
                            x + 8, yTop + 30);
                }
                g2.setClip(oldClip);
            }
        }

        private String hourLabel(int h) {
            String suffix = h < 12 ? "AM" : "PM";
            int hr = h % 12 == 0 ? 12 : h % 12;
            return hr + " " + suffix;
        }

        @Override public Dimension getPreferredSize() { return new Dimension(700, 24 * HOUR_HEIGHT); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return HOUR_HEIGHT / 2; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return HOUR_HEIGHT * 3; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        private class MouseHandler extends java.awt.event.MouseAdapter {
            @Override public void mousePressed(java.awt.event.MouseEvent ev) {
                for (Map.Entry<Rectangle, CalItem> e : hitBoxes.entrySet()) {
                    if (e.getKey().contains(ev.getPoint())) { actions.openItem(e.getValue()); return; }
                }
                int dayW = dayWidth(getWidth());
                if (ev.getX() < TIME_COL_W) return;
                int d = (ev.getX() - TIME_COL_W) / dayW;
                if (d < 0 || d >= days.size()) return;
                int minute = (int) (ev.getY() / (double) HOUR_HEIGHT * 60);
                minute = Math.max(0, Math.min(DAY_MINUTES - 60, (minute / 30) * 30));
                actions.createEventAt(days.get(d).atTime(minute / 60, minute % 60));
            }
            @Override public void mouseMoved(java.awt.event.MouseEvent ev) {
                boolean over = false;
                for (Rectangle r : hitBoxes.keySet()) if (r.contains(ev.getPoint())) { over = true; break; }
                setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        }
    }

    // ============================================================
    // Fixed header: day labels + all-day banner
    // ============================================================
    private class DayHeader extends JPanel implements Scrollable {

        private static final DateTimeFormatter DOW = DateTimeFormatter.ofPattern("EEE");
        private final Map<Rectangle, CalItem> hitBoxes = new HashMap<>();

        DayHeader() {
            setBackground(CalendarTheme.SURFACE);
            MouseHandler mh = new MouseHandler();
            addMouseListener(mh);
            addMouseMotionListener(mh);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int dayW = dayWidth(w);
            LocalDate today = LocalDate.now();
            hitBoxes.clear();

            g2.setColor(CalendarTheme.GRID_LINE_STRONG);
            g2.drawLine(0, headerHeight() - 1, w, headerHeight() - 1);

            for (int d = 0; d < days.size(); d++) {
                LocalDate date = days.get(d);
                int colX = TIME_COL_W + d * dayW;
                int cx = colX + dayW / 2;
                boolean isToday = date.equals(today);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.setColor(isToday ? CalendarTheme.ACCENT : CalendarTheme.TEXT_MUTED);
                String dow = date.format(DOW).toUpperCase();
                g2.drawString(dow, cx - g2.getFontMetrics().stringWidth(dow) / 2, 18);

                String num = String.valueOf(date.getDayOfMonth());
                g2.setFont(new Font("SansSerif", Font.BOLD, 17));
                int nw = g2.getFontMetrics().stringWidth(num);
                if (isToday) {
                    g2.setColor(CalendarTheme.ACCENT);
                    g2.fillOval(cx - 15, 24, 30, 26);
                    g2.setColor(CalendarTheme.ON_ACCENT);
                } else {
                    g2.setColor(CalendarTheme.TEXT_PRIMARY);
                }
                g2.drawString(num, cx - nw / 2, 44);

                // All-day chips
                int row = 0;
                for (CalItem it : data.itemsFor(date)) {
                    if (!it.allDay()) continue;
                    int cy = LABEL_H + row * ALLDAY_ROW_H;
                    int cw = dayW - 6;
                    int cxx = colX + 3;
                    Color base = it.color();
                    Rectangle chip = new Rectangle(cxx, cy, cw, ALLDAY_ROW_H - 2);
                    hitBoxes.put(chip, it);
                    g2.setColor(base);
                    g2.fillRoundRect(cxx, cy, cw, ALLDAY_ROW_H - 2, 6, 6);
                    Shape clip = g2.getClip();
                    g2.setClip(cxx + 4, cy, cw - 6, ALLDAY_ROW_H - 2);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                    g2.drawString(it.event.getTitle(), cxx + 5, cy + 11);
                    g2.setClip(clip);
                    row++;
                }
            }
            g2.dispose();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(700, headerHeight()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 10; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 50; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        private class MouseHandler extends java.awt.event.MouseAdapter {
            @Override public void mousePressed(java.awt.event.MouseEvent ev) {
                for (Map.Entry<Rectangle, CalItem> e : hitBoxes.entrySet()) {
                    if (e.getKey().contains(ev.getPoint())) { actions.openItem(e.getValue()); return; }
                }
            }
            @Override public void mouseMoved(java.awt.event.MouseEvent ev) {
                boolean over = false;
                for (Rectangle r : hitBoxes.keySet()) if (r.contains(ev.getPoint())) { over = true; break; }
                setCursor(Cursor.getPredefinedCursor(over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        }
    }
}
