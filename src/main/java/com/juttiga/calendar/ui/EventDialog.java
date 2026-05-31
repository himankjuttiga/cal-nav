package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;
import com.juttiga.calendar.service.CalendarService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Modal popup for creating a new event or editing an existing one. In edit
 * mode it also offers a Delete button. All persistence rules (overlap checks,
 * validation) are enforced by the service; this dialog just surfaces errors.
 */
class EventDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US);

    private final CalendarService service;
    private final Event existing; // null when creating
    private boolean changed = false;

    private final JTextField titleField = new JTextField(22);
    private final JTextField dateField = new JTextField(10);
    private final JTextField startField = new JTextField(6);
    private final JTextField endField = new JTextField(6);

    EventDialog(Frame owner, CalendarService service, Event existing, LocalDateTime defaultStart) {
        super(owner, existing == null ? "New event" : "Edit event", true);
        this.service = service;
        this.existing = existing;

        if (existing != null) {
            titleField.setText(existing.getTitle());
            dateField.setText(existing.getStart().toLocalDate().format(DATE_FMT));
            startField.setText(existing.getStart().toLocalTime().format(TIME_FMT));
            endField.setText(existing.getEnd().toLocalTime().format(TIME_FMT));
        } else {
            LocalDateTime s = defaultStart != null ? defaultStart : LocalDateTime.now();
            dateField.setText(s.toLocalDate().format(DATE_FMT));
            startField.setText(s.toLocalTime().format(TIME_FMT));
            endField.setText(s.plusHours(1).toLocalTime().format(TIME_FMT));
        }

        setContentPane(buildContent());
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(findSaveButton());
    }

    private JButton saveButton;

    private JButton findSaveButton() {
        return saveButton;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBorder(new EmptyBorder(18, 18, 14, 18));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Title"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(titleField, gbc);

        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("Date"), gbc);
        gbc.gridx = 1;
        form.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("Start"), gbc);
        gbc.gridx = 1;
        form.add(startField, gbc);
        gbc.gridx = 2;
        form.add(new JLabel("End"), gbc);
        gbc.gridx = 3;
        form.add(endField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        JLabel hint = new JLabel("Date as yyyy-MM-dd, times as h:mm AM/PM (e.g. 9:00 AM)");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(CalendarTheme.TEXT_MUTED);
        form.add(hint, gbc);

        root.add(form, BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildButtons() {
        JPanel bar = new JPanel(new BorderLayout());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        saveButton = new JButton(existing == null ? "Create" : "Save");
        saveButton.addActionListener(e -> onSave());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(cancel);
        right.add(saveButton);
        bar.add(right, BorderLayout.EAST);

        if (existing != null) {
            JButton delete = new JButton("Delete");
            delete.setForeground(new Color(0xD93025));
            delete.addActionListener(e -> onDelete());
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.add(delete);
            bar.add(left, BorderLayout.WEST);
        }
        return bar;
    }

    private void onSave() {
        try {
            String title = titleField.getText().trim();
            LocalDate date = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
            LocalTime start = LocalTime.parse(startField.getText().trim().toUpperCase(), TIME_FMT);
            LocalTime end = LocalTime.parse(endField.getText().trim().toUpperCase(), TIME_FMT);

            Event built = new Event(title, date.atTime(start), date.atTime(end));
            if (existing == null) {
                service.addEvent(built);
            } else {
                service.updateEvent(existing, built);
            }
            changed = true;
            dispose();
        } catch (DateTimeParseException ex) {
            error("Invalid date or time. Use yyyy-MM-dd and h:mm AM/PM.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            error(ex.getMessage());
        }
    }

    private void onDelete() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete this event?", "Confirm delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                service.removeEvent(existing);
                changed = true;
                dispose();
            } catch (IllegalStateException ex) {
                error(ex.getMessage());
            }
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** True if an event was created, edited, or deleted. */
    boolean wasChanged() {
        return changed;
    }
}
