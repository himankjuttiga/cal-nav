package com.juttiga.calendar.storage;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.juttiga.calendar.model.Event;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileStorage {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int SCHEMA_VERSION = 3;

    private final Path filePath;

    public FileStorage(String filePath) {
        this.filePath = Paths.get(filePath);
    }

    private boolean isJson() {
        return filePath.getFileName().toString().toLowerCase().endsWith(".json");
    }

    public List<Event> load() {
        if (isJson()) {
            if (Files.exists(filePath)) return loadJson(filePath);
            Path legacy = filePath.resolveSibling("events.txt");
            if (Files.exists(legacy)) {
                System.out.println("Migrating events from " + legacy + " to JSON.");
                return loadText(legacy);
            }
            return new ArrayList<>();
        }
        return Files.exists(filePath) ? loadText(filePath) : new ArrayList<>();
    }

    private List<Event> loadJson(Path path) {
        List<Event> events = new ArrayList<>();
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("events");
            if (arr == null) return events;
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    String        title  = o.get("title").getAsString();
                    LocalDateTime start  = LocalDateTime.parse(o.get("start").getAsString(), ISO);
                    LocalDateTime end    = LocalDateTime.parse(o.get("end").getAsString(), ISO);
                    String        desc   = o.has("description") ? o.get("description").getAsString() : null;
                    String        loc    = o.has("location")    ? o.get("location").getAsString()    : null;
                    String        series = o.has("seriesId")    ? o.get("seriesId").getAsString()    : null;
                    boolean       allDay = o.has("allDay")      && o.get("allDay").getAsBoolean();
                    String        catLbl = o.has("categoryLabel") ? o.get("categoryLabel").getAsString() : null;
                    Color         color  = null;
                    if (o.has("categoryColor") && !o.get("categoryColor").getAsString().isBlank()) {
                        try { color = new Color((int) Long.parseLong(o.get("categoryColor").getAsString(), 16)); }
                        catch (NumberFormatException ignored) {}
                    }
                    events.add(new Event(title, start, end, desc, loc, series, allDay, catLbl, color));
                } catch (Exception ex) {
                    System.err.println("Skipping malformed event: " + el);
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to read JSON events file: " + ex.getMessage());
        }
        return events;
    }

    private List<Event> loadText(Path path) {
        List<Event> events = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
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

    public void save(List<Event> events) {
        try {
            if (filePath.getParent() != null) Files.createDirectories(filePath.getParent());
            if (isJson()) saveJson(events); else saveText(events);
        } catch (IOException ex) {
            System.err.println("Failed to write events file: " + ex.getMessage());
        }
    }

    private void saveJson(List<Event> events) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", SCHEMA_VERSION);
        JsonArray arr = new JsonArray();
        for (Event e : events) {
            JsonObject o = new JsonObject();
            o.addProperty("title",  e.getTitle());
            o.addProperty("start",  e.getStart().format(ISO));
            o.addProperty("end",    e.getEnd().format(ISO));
            o.addProperty("allDay", e.isAllDay());
            if (e.getDescription()   != null) o.addProperty("description",   e.getDescription());
            if (e.getLocation()      != null) o.addProperty("location",      e.getLocation());
            if (e.getSeriesId()      != null) o.addProperty("seriesId",      e.getSeriesId());
            if (e.getCategoryLabel() != null) o.addProperty("categoryLabel", e.getCategoryLabel());
            if (e.getCategoryColor() != null)
                o.addProperty("categoryColor",
                        Integer.toHexString(e.getCategoryColor().getRGB() & 0xFFFFFF));
            arr.add(o);
        }
        root.add("events", arr);
        Files.writeString(filePath,
                new GsonBuilder().setPrettyPrinting().create().toJson(root),
                StandardCharsets.UTF_8);
    }

    private void saveText(List<Event> events) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Calendar events  (title|startISO|endISO|allDay|categoryLabel|categoryColorHex)");
        for (Event e : events) lines.add(e.toStorageLine());
        Files.write(filePath, lines);
    }

    public Path getFilePath() { return filePath; }
}
