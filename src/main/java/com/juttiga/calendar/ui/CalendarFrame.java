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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Main window. Toolbar provides navigation and Day/Week/Month switch; filter
 * bar toggles calendar layers and includes the overlap-allow toggle.
 * Personal events are editable; overlay layers are read-only.
 */
public class CalendarFrame extends JFrame implements CalendarActions, CalendarData {

    private enum ViewMode { DAY, WEEK, MONTH }

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter FULL_DAY   = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy");
    private static final DateTimeFormatter MON        = DateTimeFormatter.ofPattern("MMM");
    private static final DateTimeFormatter TIME        = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter LONG_DATE   = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    private static final Color ACCENT = CalendarTheme.ACCENT;

    private final CalendarService service;
    private final FileStorage storage;
    private final OverlayCalendars overlays = new OverlayCalendars();
    private final Set<Category> visible = new HashSet<>();

    private ViewMode mode = ViewMode.WEEK;
    private LocalDate current = LocalDate.now();

    private final JLabel titleLabel = new JLabel();
    private final JPanel canvas = new JPanel(new BorderLayout());
    private WeekView weekView;
    private MonthView monthView;

    private final JToggleButton dayBtn   = new JToggleButton("Day");
    private final JToggleButton weekBtn  = new JToggleButton("Week");
    private final JToggleButton monthBtn = new JToggleButton("Month");

    // Overlap toggle
    private final JToggleButton overlapBtn = new JToggleButton("Allow Overlaps");

    // Filter bar panel — rebuilt when user creates new categories
    private JPanel filterBar;
    private final JPanel northWrapper = new JPanel(new BorderLayout());

    public CalendarFrame(CalendarService service, FileStorage storage) {
        super("cal-nav");
        this.service = service;
        this.storage = storage;

        visible.add(Category.PERSONAL);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(CalendarTheme.SURFACE);

        northWrapper.setBackground(CalendarTheme.TOOLBAR);
        northWrapper.add(buildToolbar(), BorderLayout.NORTH);
        filterBar = buildFilterBar();
        northWrapper.add(filterBar, BorderLayout.SOUTH);
        add(northWrapper, BorderLayout.NORTH);

        canvas.setBackground(CalendarTheme.SURFACE);
        canvas.setBorder(new EmptyBorder(0, 8, 8, 8));
        add(canvas, BorderLayout.CENTER);

        updateView();
        setMinimumSize(new Dimension(1040, 740));
        setLocationRelativeTo(null);
    }

    // ----------------------------------------------------------------
    // Toolbar
    // ----------------------------------------------------------------

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
        JButton prev  = new JButton("<");
        prev.addActionListener(e -> { current = step(-1); updateView(); });
        JButton next  = new JButton(">");
        next.addActionListener(e -> { current = step(1);  updateView(); });

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
        dayBtn.addActionListener(e ->   { mode = ViewMode.DAY;   updateView(); });
        weekBtn.addActionListener(e ->  { mode = ViewMode.WEEK;  updateView(); });
        monthBtn.addActionListener(e -> { mode = ViewMode.MONTH; updateView(); });

        // Manage categories button
        JButton manageBtn = new JButton("Manage Categories");
        manageBtn.setFocusPainted(false);
        manageBtn.addActionListener(e -> openCategoryManager());

        right.add(manageBtn);
        right.setBorder(new EmptyBorder(0, 0, 0, 12));

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ----------------------------------------------------------------
    // Filter bar (rebuilt when categories change)
    // ----------------------------------------------------------------

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        bar.setOpaque(false);
        bar.setBorder(new MatteBorder(1, 0, 1, 0, CalendarTheme.GRID_LINE_STRONG));

        JLabel showLabel = new JLabel("Show:");
        showLabel.setForeground(CalendarTheme.TEXT_MUTED);
        bar.add(showLabel);

        for (Category c : Category.all()) {
            JCheckBox box = new JCheckBox(c.getLabel(), visible.contains(c));
            box.setOpaque(false);
            box.setForeground(c.getSwatch().darker());
            box.setFont(box.getFont().deriveFont(Font.BOLD));
            box.addActionListener(e -> {
                if (box.isSelected()) visible.add(c); else visible.remove(c);
                refreshData();
            });
            bar.add(box);
        }

        // Separator
        bar.add(Box.createHorizontalStrut(18));

        // Overlap toggle
        overlapBtn.setSelected(service.isAllowOverlaps());
        overlapBtn.setFocusPainted(false);
        overlapBtn.setFont(overlapBtn.getFont().deriveFont(Font.BOLD));
        overlapBtn.setForeground(overlapBtn.isSelected() ? new Color(0x0B8043) : CalendarTheme.TEXT_MUTED);
        overlapBtn.addActionListener(e -> {
            service.setAllowOverlaps(overlapBtn.isSelected());
            overlapBtn.setForeground(overlapBtn.isSelected() ? new Color(0x0B8043) : CalendarTheme.TEXT_MUTED);
        });
        bar.add(overlapBtn);

        return bar;
    }

    private void rebuildFilterBar() {
        northWrapper.remove(filterBar);
        filterBar = buildFilterBar();
        northWrapper.add(filterBar, BorderLayout.SOUTH);
        northWrapper.revalidate();
        northWrapper.repaint();
    }

    // ----------------------------------------------------------------
    // Category manager dialog
    // ----------------------------------------------------------------

    private void openCategoryManager() {
        JDialog dlg = new JDialog(this, "Manage Categories", true);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        DefaultListModel<Category> model = new DefaultListModel<>();
        for (Category c : Category.all()) model.addElement(c);
        JList<Category> list = new JList<>(model);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> l, Object v, int idx, boolean sel, boolean foc) {
                super.getListCellRendererComponent(l, v, idx, sel, foc);
                if (v instanceof Category c) {
                    setText(c.getLabel() + (c.isBuiltin() ? " (built-in)" : ""));
                    setForeground(c.getSwatch());
                }
                return this;
            }
        });
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton addBtn = new JButton("Add Category");
        addBtn.addActionListener(e -> {
            JTextField nameF = new JTextField(14);
            Color[] col = {new Color(0x1A73E8)};
            JButton cp = new JButton("Color");
            cp.setBackground(col[0]);
            cp.addActionListener(ev -> {
                Color c = JColorChooser.showDialog(dlg, "Pick Color", col[0]);
                if (c != null) { col[0] = c; cp.setBackground(c); }
            });
            JPanel p = new JPanel();
            p.add(new JLabel("Name:")); p.add(nameF); p.add(cp);
            int r = JOptionPane.showConfirmDialog(dlg, p, "New Category",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (r == JOptionPane.OK_OPTION && !nameF.getText().trim().isEmpty()) {
                Category nc = Category.createUserCategory(nameF.getText().trim(), col[0]);
                model.addElement(nc);
                visible.add(nc);
                rebuildFilterBar();
                refreshData();
            }
        });

        JButton editColorBtn = new JButton("Change Color");
        editColorBtn.addActionListener(e -> {
            Category sel = list.getSelectedValue();
            if (sel == null) return;
            Color c = JColorChooser.showDialog(dlg, "Category Color", sel.getSwatch());
            if (c != null) {
                sel.setSwatch(c);
                list.repaint();
                rebuildFilterBar();
                refreshData();
            }
        });

        JButton removeBtn = new JButton("Remove");
        removeBtn.addActionListener(e -> {
            Category sel = list.getSelectedValue();
            if (sel == null) return;
            if (sel.isBuiltin()) {
                JOptionPane.showMessageDialog(dlg, "Built-in categories cannot be removed.");
                return;
            }
            if (Category.removeUserCategory(sel)) {
                model.removeElement(sel);
                visible.remove(sel);
                rebuildFilterBar();
                refreshData();
            }
        });

        btnRow.add(addBtn); btnRow.add(editColorBtn); btnRow.add(removeBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        dlg.setContentPane(panel);
        dlg.pack();
        dlg.setMinimumSize(new Dimension(320, 260));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    // ----------------------------------------------------------------
    // CalendarData
    // ----------------------------------------------------------------

    @Override
    public List<CalItem> itemsFor(LocalDate date) {
        List<CalItem> out = new ArrayList<>();
        if (visible.contains(Category.PERSONAL)) {
            for (Event e : service.getEventsForDay(date)) {
                // Determine effective category for this event
                Category cat = Category.PERSONAL;
                if (e.getCategoryLabel() != null) {
                    Category found = Category.byLabel(e.getCategoryLabel());
                    if (found != null) cat = found;
                }
                out.add(new CalItem(e, cat));
            }
        }
        for (Category c : Category.all()) {
            if (c == Category.PERSONAL || !visible.contains(c)) continue;
            if (!c.editable) { // overlay-style read-only
                for (Event e : overlays.eventsFor(c, date)) out.add(new CalItem(e, c));
            }
        }
        return out;
    }

    // ----------------------------------------------------------------
    // CalendarActions
    // ----------------------------------------------------------------

    @Override
    public void createEventAt(LocalDateTime start) {
        EventDialog dialog = new EventDialog(this, service, null, start);
        dialog.setVisible(true);
        if (dialog.wasChanged()) { afterChange(); rebuildFilterBar(); }
    }

    @Override
    public void openItem(CalItem item) {
        if (item.editable()) {
            EventDialog dialog = new EventDialog(this, service, item.event, null);
            dialog.setVisible(true);
            if (dialog.wasChanged()) { afterChange(); rebuildFilterBar(); }
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
                + when
                + (e.getLocation() != null ? "\nLocation: " + e.getLocation() : "")
                + (e.getDescription() != null ? "\n\n" + e.getDescription() : "")
                + "\n\nCalendar: " + item.category.getLabel() + "  (read-only)";
        JOptionPane.showMessageDialog(this, msg, item.category.getLabel(), JOptionPane.INFORMATION_MESSAGE);
    }

    // ----------------------------------------------------------------
    // View management
    // ----------------------------------------------------------------

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
        if (weekView  != null) weekView.refresh();
        if (monthView != null) monthView.refresh();
        canvas.repaint();
    }

    private String weekTitle(List<LocalDate> days) {
        LocalDate a = days.get(0), b = days.get(days.size() - 1);
        if (a.getMonth() == b.getMonth()) return a.format(MONTH_YEAR);
        if (a.getYear()  == b.getYear())  return a.format(MON) + " - " + b.format(MON) + " " + b.getYear();
        return a.format(MON) + " " + a.getYear() + " - " + b.format(MON) + " " + b.getYear();
    }

    private LocalDate step(int dir) {
        return switch (mode) {
            case DAY   -> current.plusDays(dir);
            case WEEK  -> current.plusWeeks(dir);
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
