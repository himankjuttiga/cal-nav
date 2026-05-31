package com.juttiga.calendar.ui;

import com.juttiga.calendar.service.CalendarService;
import com.juttiga.calendar.storage.FileStorage;
import com.juttiga.calendar.model.Event;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Main window. A toolbar provides navigation and a Day/Week/Month switch; a
 * second row of checkboxes toggles the calendar layers. Personal events are
 * editable and never blocked by overlay events; the overlay layers (Sports,
 * Religion, Miami University) are read-only and hidden until their box is ticked.
 */
public class CalendarFrame extends JFrame implements CalendarActions, CalendarData {

    private enum ViewMode { DAY, WEEK, MONTH }

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");
    private static final DateTimeFormatter MON = DateTimeFormatter.ofPattern("MMM");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    private static final Color ACCENT = CalendarTheme.ACCENT;

    private final CalendarService service;
    private final FileStorage storage;
    private final OverlayCalendars overlays = new OverlayCalendars();
    private final Set<Category> visible = EnumSet.of(Category.PERSONAL);

    private ViewMode mode = ViewMode.WEEK;
    private LocalDate current = LocalDate.now();

    private final JLabel titleLabel = new JLabel();
    private final JPanel canvas = new JPanel(new BorderLayout());
    private WeekView weekView;
    private MonthView monthView;

    private final JToggleButton dayBtn = new JToggleButton("Day");
    private final JToggleButton weekBtn = new JToggleButton("Week");
    private final JToggleButton monthBtn = new JToggleButton("Month");

    public CalendarFrame(CalendarService service, FileStorage storage) {
        super("cal-nav");
        this.service = service;
        this.storage = storage;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(CalendarTheme.SURFACE);

        JPanel north = new JPanel(new BorderLayout());
        north.setBackground(CalendarTheme.TOOLBAR);
        north.add(buildToolbar(), BorderLayout.NORTH);
        north.add(buildFilterBar(), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        canvas.setBackground(CalendarTheme.SURFACE);
        canvas.setBorder(new EmptyBorder(0, 8, 8, 8));
        add(canvas, BorderLayout.CENTER);

        updateView();
        setMinimumSize(new Dimension(1040, 740));
        setLocationRelativeTo(null);
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        left.setOpaque(false);

        JButton create = new JButton("Create");
        create.setBackground(ACCENT);
        create.setForeground(Color.WHITE);
        create.setFocusPainted(false);
        create.setFont(create.getFont().deriveFont(Font.BOLD));
        create.addActionListener(e -> createEventAt(defaultNewStart()));

        JButton today = new JButton("Today");
        today.addActionListener(e -> { current = LocalDate.now(); updateView(); });
        JButton prev = new JButton("<");
        prev.addActionListener(e -> { current = step(-1); updateView(); });
        JButton next = new JButton(">");
        next.addActionListener(e -> { current = step(1); updateView(); });

        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(CalendarTheme.TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 10, 0, 0));

        left.add(create); left.add(today); left.add(prev); left.add(next); left.add(titleLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        right.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (JToggleButton b : new JToggleButton[]{dayBtn, weekBtn, monthBtn}) {
            group.add(b); b.setFocusPainted(false); right.add(b);
        }
        weekBtn.setSelected(true);
        dayBtn.addActionListener(e -> { mode = ViewMode.DAY; updateView(); });
        weekBtn.addActionListener(e -> { mode = ViewMode.WEEK; updateView(); });
        monthBtn.addActionListener(e -> { mode = ViewMode.MONTH; updateView(); });
        right.setBorder(new EmptyBorder(0, 0, 0, 12));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        bar.setOpaque(false);
        bar.setBorder(new MatteBorder(1, 0, 1, 0, CalendarTheme.GRID_LINE_STRONG));

        JLabel label = new JLabel("Show:");
        label.setForeground(CalendarTheme.TEXT_MUTED);
        bar.add(label);

        for (Category c : Category.values()) {
            JCheckBox box = new JCheckBox(c.label, visible.contains(c));
            box.setOpaque(false);
            box.setForeground(c.swatch.darker());
            box.setFont(box.getFont().deriveFont(Font.BOLD));
            box.addActionListener(e -> {
                if (box.isSelected()) visible.add(c); else visible.remove(c);
                refreshData();
            });
            bar.add(box);
        }
        return bar;
    }

    // ============================================================
    // CalendarData: combine personal + enabled overlays
    // ============================================================
    @Override
    public List<CalItem> itemsFor(LocalDate date) {
        List<CalItem> out = new ArrayList<>();
        if (visible.contains(Category.PERSONAL)) {
            for (Event e : service.getEventsForDay(date)) {
                out.add(new CalItem(e, Category.PERSONAL));
            }
        }
        for (Category c : new Category[]{Category.SPORTS, Category.RELIGION, Category.MIAMI}) {
            if (visible.contains(c)) {
                for (Event e : overlays.eventsFor(c, date)) out.add(new CalItem(e, c));
            }
        }
        return out;
    }

    // ============================================================
    // CalendarActions
    // ============================================================
    @Override
    public void createEventAt(LocalDateTime start) {
        EventDialog dialog = new EventDialog(this, service, null, start);
        dialog.setVisible(true);
        if (dialog.wasChanged()) afterChange();
    }

    @Override
    public void openItem(CalItem item) {
        if (item.editable()) {
            EventDialog dialog = new EventDialog(this, service, item.event, null);
            dialog.setVisible(true);
            if (dialog.wasChanged()) afterChange();
        } else {
            showInfo(item);
        }
    }

    private void showInfo(CalItem item) {
        Event e = item.event;
        String when = item.allDay()
                ? "All day"
                : e.getStart().toLocalTime().format(TIME) + " - " + e.getEnd().toLocalTime().format(TIME);
        String msg = e.getTitle() + "\n"
                + e.getStart().toLocalDate().format(LONG_DATE) + "\n"
                + when + "\n\n"
                + "Calendar: " + item.category.label + "  (read-only)";
        JOptionPane.showMessageDialog(this, msg, item.category.label, JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================
    // View management
    // ============================================================
    private List<LocalDate> weekDays() {
        LocalDate weekStart = current.minusDays(current.getDayOfWeek().getValue() % 7);
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) days.add(weekStart.plusDays(i));
        return days;
    }

    private void updateView() {
        canvas.removeAll();
        switch (mode) {
            case DAY -> {
                List<LocalDate> one = List.of(current);
                if (weekView == null) weekView = new WeekView(this, one, this);
                else weekView.setDays(one);
                canvas.add(weekView, BorderLayout.CENTER);
                titleLabel.setText(current.format(FULL_DAY));
            }
            case WEEK -> {
                List<LocalDate> days = weekDays();
                if (weekView == null) weekView = new WeekView(this, days, this);
                else weekView.setDays(days);
                canvas.add(weekView, BorderLayout.CENTER);
                titleLabel.setText(weekTitle(days));
            }
            case MONTH -> {
                if (monthView == null) monthView = new MonthView(this, current, this);
                else monthView.setAnchor(current);
                canvas.add(monthView, BorderLayout.CENTER);
                titleLabel.setText(current.format(MONTH_YEAR));
            }
        }
        canvas.revalidate();
        canvas.repaint();
    }

    private void refreshData() {
        if (weekView != null) weekView.refresh();
        if (monthView != null) monthView.refresh();
        canvas.repaint();
    }

    private String weekTitle(List<LocalDate> days) {
        LocalDate a = days.get(0), b = days.get(days.size() - 1);
        if (a.getMonth() == b.getMonth()) return a.format(MONTH_YEAR);
        if (a.getYear() == b.getYear()) return a.format(MON) + " - " + b.format(MON) + " " + b.getYear();
        return a.format(MON) + " " + a.getYear() + " - " + b.format(MON) + " " + b.getYear();
    }

    private LocalDate step(int dir) {
        return switch (mode) {
            case DAY -> current.plusDays(dir);
            case WEEK -> current.plusWeeks(dir);
            case MONTH -> current.plusMonths(dir);
        };
    }

    private LocalDateTime defaultNewStart() {
        int hour = Math.max(8, Math.min(17, LocalDateTime.now().getHour()));
        return current.atTime(hour, 0);
    }

    private void afterChange() {
        storage.save(service.getAllEvents());
        refreshData();
    }
}
