package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;
import com.juttiga.calendar.service.CalendarService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Modal dialog for creating or editing a calendar event. Supports:
 *   - Title, date, start/end time, location, notes
 *   - Optional weekly recurrence: pick days of week + an end date
 *   - Deleting a single instance or the entire recurring series
 */
class EventDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US);

    private final CalendarService service;
    private final Event existing;   // null when creating
    private boolean changed = false;

    // Core fields
    private final JTextField titleField    = new JTextField(24);
    private final JTextField dateField     = new JTextField(10);
    private final JTextField startField    = new JTextField(8);
    private final JTextField endField      = new JTextField(8);
    private final JTextField locationField = new JTextField(24);
    private final JTextArea  descArea      = new JTextArea(3, 24);

    // Repeat section
    private final JCheckBox repeatCheck = new JCheckBox("Repeat weekly");
    private final Map<DayOfWeek, JCheckBox> dayBoxes = new EnumMap<>(DayOfWeek.class);
    private final JTextField untilField = new JTextField(10);
    private JPanel repeatPanel;

    // Saved button reference for getRootPane().setDefaultButton()
    private JButton saveButton;

    EventDialog(Frame owner, CalendarService service, Event existing, LocalDateTime defaultStart) {
        super(owner, existing == null ? "New event" : "Edit event", true);
        this.service  = service;
        this.existing = existing;

        populateFields(defaultStart);
        setContentPane(buildContent());
        pack();
        setMinimumSize(new Dimension(420, 0));
        setResizable(false);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(saveButton);
    }

    // ----------------------------------------------------------------
    // Field population
    // ----------------------------------------------------------------

    private void populateFields(LocalDateTime defaultStart) {
        if (existing != null) {
            titleField.setText(existing.getTitle());
            dateField.setText(existing.getStart().toLocalDate().format(DATE_FMT));
            startField.setText(existing.getStart().toLocalTime().format(TIME_FMT));
            endField.setText(existing.getEnd().toLocalTime().format(TIME_FMT));
            if (existing.getLocation()    != null) locationField.setText(existing.getLocation());
            if (existing.getDescription() != null) descArea.setText(existing.getDescription());
        } else {
            LocalDateTime s = defaultStart != null ? defaultStart : LocalDateTime.now();
            dateField.setText(s.toLocalDate().format(DATE_FMT));
            startField.setText(s.toLocalTime().format(TIME_FMT));
            endField.setText(s.plusHours(1).toLocalTime().format(TIME_FMT));
            // Default repeat-until to 4 weeks out
            untilField.setText(s.toLocalDate().plusWeeks(4).format(DATE_FMT));
        }
    }

    // ----------------------------------------------------------------
    // UI construction
    // ----------------------------------------------------------------

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(18, 18, 14, 18));
        root.add(buildForm(),    BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets  = new Insets(5, 6, 5, 6);
        c.anchor  = GridBagConstraints.WEST;

        int row = 0;

        // Title
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(label("Title *"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        form.add(titleField, c); c.weightx = 0;
        row++;

        // Date
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(label("Date *"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(dateField, c);
        row++;

        // Start / End
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(label("Start *"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(startField, c);
        c.gridx = 2; c.fill = GridBagConstraints.NONE;
        form.add(label("End *"), c);
        c.gridx = 3; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(endField, c);
        row++;

        // Location
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(label("Location"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(locationField, c);
        row++;

        // Notes
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        form.add(label("Notes"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        form.add(new JScrollPane(descArea), c);
        c.weighty = 0; c.anchor = GridBagConstraints.WEST;
        row++;

        // Hint
        c.gridx = 0; c.gridy = row; c.gridwidth = 4; c.fill = GridBagConstraints.NONE;
        JLabel hint = new JLabel("Date: yyyy-MM-dd   Time: h:mm AM/PM  (e.g. 9:00 AM)");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(CalendarTheme.TEXT_MUTED);
        form.add(hint, c);
        row++;

        // Repeat section — only shown when creating a new event
        if (existing == null) {
            c.gridx = 0; c.gridy = row; c.gridwidth = 4; c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 0, 0, 0);
            form.add(buildRepeatSection(), c);
        }

        return form;
    }

    private JPanel buildRepeatSection() {
        repeatPanel = new JPanel(new GridBagLayout());
        repeatPanel.setBorder(new CompoundBorder(
                new TitledBorder(null, "Recurrence",
                        TitledBorder.LEADING, TitledBorder.TOP,
                        null, CalendarTheme.TEXT_MUTED),
                new EmptyBorder(4, 6, 6, 6)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.anchor = GridBagConstraints.WEST;

        // Repeat toggle
        c.gridx = 0; c.gridy = 0; c.gridwidth = 7;
        repeatCheck.setOpaque(false);
        repeatCheck.setFont(repeatCheck.getFont().deriveFont(Font.BOLD));
        repeatCheck.addActionListener(e -> updateRepeatEnabled());
        repeatPanel.add(repeatCheck, c);

        // Day-of-week checkboxes
        DayOfWeek[] order = {
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        };
        String[] labels = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

        c.gridy = 1; c.gridwidth = 1;
        for (int i = 0; i < order.length; i++) {
            JCheckBox box = new JCheckBox(labels[i]);
            box.setOpaque(false);
            box.setEnabled(false);
            dayBoxes.put(order[i], box);
            c.gridx = i;
            repeatPanel.add(box, c);
        }

        // Until date
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        JLabel untilLabel = new JLabel("Repeat until:");
        untilLabel.setEnabled(false);
        untilLabel.setName("untilLabel");
        repeatPanel.add(untilLabel, c);

        c.gridx = 2; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        untilField.setEnabled(false);
        untilField.setToolTipText("yyyy-MM-dd");
        repeatPanel.add(untilField, c);

        return repeatPanel;
    }

    private void updateRepeatEnabled() {
        boolean on = repeatCheck.isSelected();
        for (JCheckBox box : dayBoxes.values()) box.setEnabled(on);
        untilField.setEnabled(on);
        // Also toggle the until label
        for (Component comp : repeatPanel.getComponents()) {
            if ("untilLabel".equals(comp.getName())) comp.setEnabled(on);
        }
    }

    private JPanel buildButtons() {
        JPanel bar = new JPanel(new BorderLayout());

        saveButton = new JButton(existing == null ? "Create" : "Save");
        saveButton.setBackground(CalendarTheme.ACCENT);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.addActionListener(e -> onSave());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(cancel);
        right.add(saveButton);
        bar.add(right, BorderLayout.EAST);

        if (existing != null) {
            JButton deleteSingle = new JButton("Delete");
            deleteSingle.setForeground(new Color(0xD93025));
            deleteSingle.addActionListener(e -> onDelete(false));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.add(deleteSingle);

            if (existing.isRecurring()) {
                JButton deleteSeries = new JButton("Delete series");
                deleteSeries.setForeground(new Color(0xD93025));
                deleteSeries.addActionListener(e -> onDelete(true));
                left.add(deleteSeries);
            }
            bar.add(left, BorderLayout.WEST);
        }
        return bar;
    }

    // ----------------------------------------------------------------
    // Actions
    // ----------------------------------------------------------------

    private void onSave() {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) { error("Title is required."); return; }

            LocalDate date  = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
            LocalTime start = LocalTime.parse(startField.getText().trim().toUpperCase(), TIME_FMT);
            LocalTime end   = LocalTime.parse(endField.getText().trim().toUpperCase(), TIME_FMT);
            String desc     = descArea.getText();
            String loc      = locationField.getText();

            if (existing == null && repeatCheck.isSelected()) {
                // Recurring create
                Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
                for (Map.Entry<DayOfWeek, JCheckBox> entry : dayBoxes.entrySet())
                    if (entry.getValue().isSelected()) days.add(entry.getKey());
                if (days.isEmpty()) { error("Select at least one repeat day."); return; }

                LocalDate until = LocalDate.parse(untilField.getText().trim(), DATE_FMT);
                int added = service.addRecurringEvents(
                        title, start, end, desc, loc, date, until, days);
                if (added == 0) {
                    error("No instances could be added — all slots conflict with existing events.");
                    return;
                }
                changed = true;
                dispose();
            } else {
                // Single event create or edit
                Event built = new Event(title, date.atTime(start), date.atTime(end),
                        desc, loc,
                        existing != null ? existing.getSeriesId() : null);
                if (existing == null) {
                    service.addEvent(built);
                } else {
                    service.updateEvent(existing, built);
                }
                changed = true;
                dispose();
            }
        } catch (DateTimeParseException ex) {
            error("Invalid date or time.\nDate: yyyy-MM-dd   Time: h:mm AM/PM (e.g. 9:00 AM)");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(ex.getMessage());
        }
    }

    private void onDelete(boolean wholeSeries) {
        String msg = wholeSeries
                ? "Delete ALL events in this recurring series?"
                : "Delete this event?";
        int choice = JOptionPane.showConfirmDialog(this, msg, "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        try {
            if (wholeSeries && existing.isRecurring()) {
                service.removeSeries(existing.getSeriesId());
            } else {
                service.removeEvent(existing);
            }
            changed = true;
            dispose();
        } catch (IllegalStateException ex) {
            error(ex.getMessage());
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(CalendarTheme.TEXT_PRIMARY);
        return l;
    }

    boolean wasChanged() { return changed; }
}
