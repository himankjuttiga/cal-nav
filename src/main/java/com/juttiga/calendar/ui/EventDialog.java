package com.juttiga.calendar.ui;

import com.juttiga.calendar.model.Event;
import com.juttiga.calendar.service.CalendarService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
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
 *   - All-day toggle (hides time fields)
 *   - Category selector (built-in + user-created custom categories)
 *   - Custom color picker per event
 *   - Optional weekly recurrence
 *   - Deleting a single instance or entire recurring series
 */
class EventDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US);

    private final CalendarService service;
    private final Event existing;
    private boolean changed = false;

    // Core fields
    private final JTextField titleField    = new JTextField(24);
    private final JTextField dateField     = new JTextField(10);
    private final JTextField startField    = new JTextField(8);
    private final JTextField endField      = new JTextField(8);
    private final JTextField locationField = new JTextField(24);
    private final JTextArea  descArea      = new JTextArea(3, 24);

    // All-day toggle
    private final JCheckBox allDayCheck = new JCheckBox("All-day event");
    private JLabel startLabel, endLabel;

    // Category + color
    private JComboBox<Category> categoryCombo;
    private Color chosenColor = null;   // null = use category default
    private JButton colorSwatch;

    // Repeat section
    private final JCheckBox repeatCheck = new JCheckBox("Repeat weekly");
    private final Map<DayOfWeek, JCheckBox> dayBoxes = new EnumMap<>(DayOfWeek.class);
    private final JTextField untilField = new JTextField(10);
    private JPanel repeatPanel;

    private JButton saveButton;

    EventDialog(Frame owner, CalendarService service, Event existing, LocalDateTime defaultStart) {
        super(owner, existing == null ? "New event" : "Edit event", true);
        this.service  = service;
        this.existing = existing;

        populateFields(defaultStart);
        setContentPane(buildContent());
        pack();
        setMinimumSize(new Dimension(460, 0));
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
            allDayCheck.setSelected(existing.isAllDay());
            if (!existing.isAllDay()) {
                startField.setText(existing.getStart().toLocalTime().format(TIME_FMT));
                endField.setText(existing.getEnd().toLocalTime().format(TIME_FMT));
            }
            if (existing.getLocation()    != null) locationField.setText(existing.getLocation());
            if (existing.getDescription() != null) descArea.setText(existing.getDescription());
            chosenColor = existing.getCategoryColor();
        } else {
            LocalDateTime s = defaultStart != null ? defaultStart : LocalDateTime.now();
            dateField.setText(s.toLocalDate().format(DATE_FMT));
            startField.setText(s.toLocalTime().format(TIME_FMT));
            endField.setText(s.plusHours(1).toLocalTime().format(TIME_FMT));
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

        // All-day toggle
        c.gridx = 0; c.gridy = row; c.gridwidth = 4; c.fill = GridBagConstraints.NONE;
        allDayCheck.setOpaque(false);
        allDayCheck.addActionListener(e -> updateAllDayState());
        form.add(allDayCheck, c);
        row++;

        // Date
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(label("Date *"), c);
        c.gridx = 1; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(dateField, c);
        row++;

        // Start / End
        startLabel = label("Start *");
        endLabel   = label("End *");
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(startLabel, c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(startField, c);
        c.gridx = 2; c.fill = GridBagConstraints.NONE;
        form.add(endLabel, c);
        c.gridx = 3; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(endField, c);
        row++;

        // Category + color swatch
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(label("Category"), c);

        categoryCombo = new JComboBox<>();
        refreshCategoryCombo();
        categoryCombo.setRenderer(new CategoryRenderer());

        // "New category..." sentinel item handled in action
        categoryCombo.addActionListener(this::onCategoryChange);

        c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(categoryCombo, c);

        // Color swatch button
        colorSwatch = new JButton();
        colorSwatch.setPreferredSize(new Dimension(32, 22));
        colorSwatch.setFocusPainted(false);
        colorSwatch.setToolTipText("Choose a custom color for this event");
        colorSwatch.addActionListener(e -> pickColor());
        updateSwatchDisplay();
        c.gridx = 3; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(colorSwatch, c);
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

        // Repeat section (new events only)
        if (existing == null) {
            c.gridx = 0; c.gridy = row; c.gridwidth = 4; c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 0, 0, 0);
            form.add(buildRepeatSection(), c);
        }

        updateAllDayState(); // sync initial state
        return form;
    }

    /** Repopulates the category combo from the current global category list. */
    private void refreshCategoryCombo() {
        Object selected = categoryCombo.getSelectedItem();
        categoryCombo.removeAllItems();
        for (Category cat : Category.editable()) categoryCombo.addItem(cat);
        categoryCombo.addItem(null); // sentinel for "Add new category..."
        // Restore selection
        if (selected instanceof Category s) categoryCombo.setSelectedItem(s);
        else categoryCombo.setSelectedIndex(0);
    }

    private void onCategoryChange(ActionEvent e) {
        Object sel = categoryCombo.getSelectedItem();
        if (sel == null) {
            // Sentinel selected — open "new category" dialog
            categoryCombo.hidePopup();
            SwingUtilities.invokeLater(this::createNewCategory);
        }
    }

    private void createNewCategory() {
        JTextField nameField = new JTextField(16);
        Color[] picked = { new Color(0x1A73E8) };

        JButton pickBtn = new JButton("Pick Color");
        JPanel colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(28, 22));
        colorPreview.setBackground(picked[0]);
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        pickBtn.addActionListener(ev -> {
            Color c = JColorChooser.showDialog(this, "Category Color", picked[0]);
            if (c != null) { picked[0] = c; colorPreview.setBackground(c); }
        });

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4); gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0; gc.gridy = 0; panel.add(new JLabel("Category name:"), gc);
        gc.gridx = 1; gc.gridwidth = 2; gc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(nameField, gc);
        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 1; gc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Color:"), gc);
        gc.gridx = 1; panel.add(colorPreview, gc);
        gc.gridx = 2; panel.add(pickBtn, gc);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "New Category", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                Category existing = Category.byLabel(name);
                Category cat = (existing != null) ? existing : Category.createUserCategory(name, picked[0]);
                refreshCategoryCombo();
                categoryCombo.setSelectedItem(cat);
                return;
            }
        }
        // Cancelled — revert to first item
        if (categoryCombo.getItemCount() > 0) categoryCombo.setSelectedIndex(0);
    }

    private void pickColor() {
        Color initial = chosenColor != null ? chosenColor
                : (categoryCombo.getSelectedItem() instanceof Category c ? c.getSwatch() : CalendarTheme.ACCENT);
        Color picked = JColorChooser.showDialog(this, "Event Color", initial);
        if (picked != null) {
            chosenColor = picked;
            updateSwatchDisplay();
        }
    }

    private void updateSwatchDisplay() {
        if (chosenColor != null) {
            colorSwatch.setBackground(chosenColor);
            colorSwatch.setForeground(chosenColor);
            colorSwatch.setText("");
            colorSwatch.setToolTipText("Custom color set — click to change");
        } else {
            colorSwatch.setBackground(null);
            colorSwatch.setForeground(null);
            colorSwatch.setText("…");
            colorSwatch.setToolTipText("Choose a custom color for this event");
        }
    }

    private void updateAllDayState() {
        boolean ad = allDayCheck.isSelected();
        startField.setEnabled(!ad);
        endField.setEnabled(!ad);
        if (startLabel != null) startLabel.setEnabled(!ad);
        if (endLabel   != null) endLabel.setEnabled(!ad);
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

        c.gridx = 0; c.gridy = 0; c.gridwidth = 7;
        repeatCheck.setOpaque(false);
        repeatCheck.setFont(repeatCheck.getFont().deriveFont(Font.BOLD));
        repeatCheck.addActionListener(e -> updateRepeatEnabled());
        repeatPanel.add(repeatCheck, c);

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
        for (Component comp : repeatPanel.getComponents())
            if ("untilLabel".equals(comp.getName())) comp.setEnabled(on);
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
            boolean   ad    = allDayCheck.isSelected();
            String    desc  = descArea.getText();
            String    loc   = locationField.getText();

            // Determine category
            Object catSel = categoryCombo.getSelectedItem();
            String catLabel = (catSel instanceof Category cat) ? cat.getLabel() : null;
            Color  catColor = chosenColor;
            // If user-defined category, use its color as default when no override
            if (catColor == null && catSel instanceof Category cat && !cat.isBuiltin()) {
                catColor = cat.getSwatch();
            }

            if (ad) {
                // All-day event: use midnight start, end = next midnight
                LocalDateTime s = date.atStartOfDay();
                LocalDateTime e = date.plusDays(1).atStartOfDay();
                Event built = new Event(title, s, e, desc, loc,
                        existing != null ? existing.getSeriesId() : null,
                        true, catLabel, catColor);
                if (existing == null) service.addEvent(built);
                else                  service.updateEvent(existing, built);
                changed = true;
                dispose();
            } else {
                LocalTime start = LocalTime.parse(startField.getText().trim().toUpperCase(), TIME_FMT);
                LocalTime end   = LocalTime.parse(endField.getText().trim().toUpperCase(), TIME_FMT);

                if (existing == null && repeatCheck.isSelected()) {
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
                    Event built = new Event(title, date.atTime(start), date.atTime(end),
                            desc, loc,
                            existing != null ? existing.getSeriesId() : null,
                            false, catLabel, catColor);
                    if (existing == null) service.addEvent(built);
                    else                  service.updateEvent(existing, built);
                    changed = true;
                    dispose();
                }
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
            if (wholeSeries && existing.isRecurring()) service.removeSeries(existing.getSeriesId());
            else                                        service.removeEvent(existing);
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

    // ----------------------------------------------------------------
    // Category combo renderer
    // ----------------------------------------------------------------

    private static class CategoryRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText("+ Add new category…");
                setForeground(CalendarTheme.ACCENT);
            } else if (value instanceof Category cat) {
                setText(cat.getLabel());
                // Show a small color dot
                setIcon(colorDot(cat.getSwatch()));
            }
            return this;
        }

        private Icon colorDot(Color c) {
            return new Icon() {
                public void paintIcon(Component comp, Graphics g, int x, int y) {
                    g.setColor(c);
                    g.fillOval(x, y + 1, 12, 12);
                    g.setColor(c.darker());
                    g.drawOval(x, y + 1, 12, 12);
                }
                public int getIconWidth()  { return 14; }
                public int getIconHeight() { return 14; }
            };
        }
    }
}
