package com.juttiga.calendar.storage;

import com.juttiga.calendar.model.Event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain text file persistence for calendar events.
 * Each event occupies one line in the format: title|startISO|endISO
 */
public class FileStorage {

    private final Path filePath;

    public FileStorage(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    /**
     * Loads events from disk. Returns an empty list if the file does not exist.
     * Malformed lines are skipped with a warning to stderr.
     */
    public List<Event> load() {
        List<Event> events = new ArrayList<>();
        if (!Files.exists(filePath)) {
            return events;
        }
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                try {
                    events.add(Event.fromStorageLine(trimmed));
                } catch (Exception ex) {
                    System.err.println("Skipping malformed line: " + line);
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to read events file: " + ex.getMessage());
        }
        return events;
    }

    /**
     * Writes all events to disk, replacing any prior contents.
     */
    public void save(List<Event> events) {
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            List<String> lines = new ArrayList<>();
            lines.add("# Calendar events  (title|startISO|endISO)");
            for (Event event : events) {
                lines.add(event.toStorageLine());
            }
            Files.write(filePath, lines);
        } catch (IOException ex) {
            System.err.println("Failed to write events file: " + ex.getMessage());
        }
    }

    public Path getFilePath() {
        return filePath;
    }
}
