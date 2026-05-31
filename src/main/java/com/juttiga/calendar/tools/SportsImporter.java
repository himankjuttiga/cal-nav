package com.juttiga.calendar.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Pulls real game schedules from ESPN's public (undocumented, key-free) JSON
 * endpoints and writes them into the Sports layer file that cal-nav reads.
 *
 * ESPN has no official public API, so this uses the same site.api.espn.com
 * endpoints that power espn.com. They need no API key but are unsupported and
 * may change without notice.
 *
 * Run with Maven (from the project root):
 *
 *   # A single team's whole season (find the team id from the league's
 *   # /teams endpoint, e.g. site.api.espn.com/apis/site/v2/sports/basketball/nba/teams):
 *   mvn exec:java -Dexec.mainClass=com.juttiga.calendar.tools.SportsImporter \
 *       -Dexec.args="basketball/nba team 5"
 *
 *   # Every game in a date range across a league:
 *   mvn exec:java -Dexec.mainClass=com.juttiga.calendar.tools.SportsImporter \
 *       -Dexec.args="basketball/nba range 20261001 20270101"
 *
 * League path examples: basketball/nba, baseball/mlb, football/nfl, hockey/nhl,
 * football/college-football, basketball/mens-college-basketball.
 *
 * Output defaults to ~/.cal-nav/sports.txt, which the app prefers over the
 * bundled sample at runtime, so a refresh shows up without rebuilding. Pass a
 * different output path as the final argument to target another file.
 */
public class SportsImporter {

    private static final String BASE = "https://site.api.espn.com/apis/site/v2/sports/";
    private static final int GAME_MINUTES = 150; // ESPN gives no end time; assume 2.5h
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();


    private void run(@org.jetbrains.annotations.NotNull String[] args) throws Exception {
        String league = args[0];
        String mode = args[1];
        List<String[]> games = new ArrayList<>(); // [startIso, line]
        Path output;

        if (mode.equalsIgnoreCase("team")) {
            String teamId = args[2];
            output = outputPath(args, 3);
            String url = BASE + league + "/teams/" + teamId + "/schedule";
            System.out.println("Fetching " + url);
            collect(fetch(url), games);
        } else if (mode.equalsIgnoreCase("range")) {
            LocalDate start = LocalDate.parse(args[2], YYYYMMDD);
            LocalDate end = LocalDate.parse(args[3], YYYYMMDD);
            output = outputPath(args, 4);
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                String url = BASE + league + "/scoreboard?dates=" + d.format(YYYYMMDD);
                String body = fetch(url);
                if (body != null) collect(body, games);
            }
        } else {
            System.out.println("Unknown mode: " + mode + " (use 'team' or 'range')");
            return;
        }

        write(output, games, league, mode);
    }

    private Path outputPath(String[] args, int idx) {
        if (args.length > idx) {
            return Paths.get(args[idx]);
        }
        return Paths.get(System.getProperty("user.home"), ".cal-nav", "sports.txt");
    }

    private String fetch(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (cal-nav SportsImporter)")
                    .timeout(Duration.ofSeconds(20))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.err.println("HTTP " + resp.statusCode() + " for " + url);
                return null;
            }
            return resp.body();
        } catch (Exception ex) {
            System.err.println("Request failed: " + ex.getMessage());
            return null;
        }
    }

    /** Extracts each event's title and start time from an ESPN JSON payload. */
    private void collect(String body, List<String[]> games) {
        if (body == null) return;
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("events")) return;
            JsonArray events = root.getAsJsonArray("events");
            for (JsonElement el : events) {
                try {
                    JsonObject ev = el.getAsJsonObject();
                    String dateStr = ev.get("date").getAsString();
                    String title = ev.has("shortName")
                            ? ev.get("shortName").getAsString()
                            : ev.get("name").getAsString();

                    OffsetDateTime odt = OffsetDateTime.parse(dateStr);
                    LocalDateTime startLocal = odt.atZoneSameInstant(ZoneId.systemDefault())
                            .toLocalDateTime();
                    LocalDateTime endLocal = startLocal.plusMinutes(GAME_MINUTES);

                    String startIso = startLocal.format(ISO);
                    String line = title.replace("|", "/") + "|" + startIso + "|" + endLocal.format(ISO);
                    games.add(new String[]{startIso, line});
                } catch (Exception ignore) {
                    // skip an unparseable event
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not parse response: " + ex.getMessage());
        }
    }

    private void write(Path output, List<String[]> games, String league, String mode) throws Exception {
        games.sort(Comparator.comparing(g -> g[0]));
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("# Sports calendar generated by SportsImporter");
        lines.add("# Source: ESPN public endpoints (" + league + ", " + mode + ")");
        lines.add("# title|startISO|endISO  (local time, end is approximate)");
        for (String[] g : games) lines.add(g[1]);

        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.write(output, lines, StandardCharsets.UTF_8);
        System.out.println("Wrote " + games.size() + " game(s) to " + output);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println("  <leaguePath> team <teamId> [outputFile]");
            System.out.println("  <leaguePath> range <startYYYYMMDD> <endYYYYMMDD> [outputFile]");
            System.out.println("Example: basketball/nba team 5");
            return;
        }
        new SportsImporter().run(args);
    }
}
