package com.juttiga.calendar.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A calendar layer. Built-in system layers (PERSONAL, RELIGION, MIAMI) are
 * created at startup; users can add custom layers at runtime with any color.
 *
 * Replaces the old enum so that the set of categories is mutable.
 */
public final class Category {

    // ----------------------------------------------------------------
    // Built-in system categories (act like the old enum constants)
    // ----------------------------------------------------------------

    public static final Category PERSONAL = new Category(
            "My Calendar", new Color(0x1A73E8), false, true, true, true);
    public static final Category RELIGION = new Category(
            "Religion", new Color(0x8E24AA), true, false, false, true);
    public static final Category MIAMI = new Category(
            "Miami University", new Color(0xC3142D), true, false, false, true);

    // The mutable list of ALL categories (system + user-created).
    private static final List<Category> ALL = new ArrayList<>(List.of(PERSONAL, RELIGION, MIAMI));

    // ----------------------------------------------------------------
    // Instance fields
    // ----------------------------------------------------------------

    final String  label;
    Color         swatch;       // mutable so user can recolor
    final boolean allDay;
    final boolean editable;
    final boolean titleColored;
    final boolean builtin;      // true = cannot be deleted

    // ----------------------------------------------------------------
    // Construction
    // ----------------------------------------------------------------

    private Category(String label, Color swatch, boolean allDay,
                     boolean editable, boolean titleColored, boolean builtin) {
        this.label        = label;
        this.swatch       = swatch;
        this.allDay       = allDay;
        this.editable     = editable;
        this.titleColored = titleColored;
        this.builtin      = builtin;
    }

    /** Creates a new user-defined category and registers it globally. */
    public static Category createUserCategory(String label, Color color) {
        Category c = new Category(label, color, false, true, false, false);
        ALL.add(c);
        return c;
    }

    /** Removes a user-defined category (builtin categories are protected). */
    public static boolean removeUserCategory(Category c) {
        if (c.builtin) return false;
        return ALL.remove(c);
    }

    public static List<Category> all()      { return new ArrayList<>(ALL); }
    public static List<Category> editable() {
        List<Category> out = new ArrayList<>();
        for (Category c : ALL) if (c.editable) out.add(c);
        return out;
    }

    /** Finds a category by label (case-insensitive). Returns null if not found. */
    public static Category byLabel(String label) {
        if (label == null) return null;
        for (Category c : ALL) if (c.label.equalsIgnoreCase(label)) return c;
        return null;
    }

    public String  getLabel()  { return label; }
    public Color   getSwatch() { return swatch; }
    public void    setSwatch(Color c) { this.swatch = c; }
    public boolean isBuiltin() { return builtin; }

    @Override public String toString() { return label; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category cat)) return false;
        return Objects.equals(label, cat.label);
    }

    @Override public int hashCode() { return Objects.hashCode(label); }
}
