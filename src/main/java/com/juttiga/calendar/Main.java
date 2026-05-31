package com.juttiga.calendar;

import com.formdev.flatlaf.FlatLightLaf;
import com.juttiga.calendar.service.CalendarService;
import com.juttiga.calendar.storage.FileStorage;
import com.juttiga.calendar.ui.CalendarFrame;

import javax.swing.*;

/**
 * Application entry point. Wires the storage, service, and UI together.
 */
public class Main {

    // Stored per user so the app works the same whether run from source or
    // installed as a native app. Override by passing a path as the first arg.
    private static final String DEFAULT_DATA_FILE =
            System.getProperty("user.home") + java.io.File.separator
                    + ".cal-nav" + java.io.File.separator + "events.txt";

    public static void main(String[] args) {
        String dataFile = args.length > 0 ? args[0] : DEFAULT_DATA_FILE;

        FileStorage storage = new FileStorage(dataFile);
        CalendarService service = new CalendarService();
        service.loadEvents(storage.load());

        // Save events to disk on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                storage.save(service.getAllEvents())));

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new FlatLightLaf());
                // Customize a few UI defaults for extra polish
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 6);
                UIManager.put("Component.focusWidth", 1);
                UIManager.put("Button.hoverBackground", new java.awt.Color(0xE8F0FE));
            } catch (Exception ignored) {
                // Fall back to default look and feel
            }
            new CalendarFrame(service, storage).setVisible(true);
        });
    }
}