package simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One line of {@link League#userTransfers} or {@link League#sumTransfers}.
 * Section labels stay sections. Player lines become position, name, year, overall, and destination.
 */
public final class TransferSummaryLine {
    private static final Pattern OUTGOING = Pattern.compile(
            "^(\\S+)\\s+(.+),\\s+(\\S+)\\s+Ovr:\\s*(\\d+)\\s*\\((.+)\\)\\s*$");
    private static final Pattern INCOMING = Pattern.compile(
            "^(\\S+)\\s+(.+?)\\s+(RS|Fr|So|Jr|Sr|5Y-Sr)\\s+Ovr:\\s*(\\d+)\\s*\\((.+)\\)\\s*$");
    private static final Pattern LEAGUE = Pattern.compile("^(\\d+)\\s+(\\S+)\\s+(.+)$");

    public final boolean section;
    public final String sectionTitle;
    public final String position;
    public final String name;
    public final String year;
    public final String overall;
    public final String destination;

    private TransferSummaryLine(boolean section, String sectionTitle, String position, String name,
                                String year, String overall, String destination) {
        this.section = section;
        this.sectionTitle = sectionTitle == null ? "" : sectionTitle;
        this.position = position == null ? "" : position;
        this.name = name == null ? "" : name;
        this.year = year == null ? "" : year;
        this.overall = overall == null ? "" : overall;
        this.destination = destination == null ? "" : destination;
    }

    public static List<TransferSummaryLine> parse(String text) {
        List<TransferSummaryLine> rows = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return rows;
        }
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.endsWith(":") && !line.contains("Ovr:")) {
                rows.add(section(line.substring(0, line.length() - 1).trim()));
                continue;
            }
            TransferSummaryLine player = parsePlayer(line);
            rows.add(player != null ? player : section(line));
        }
        return rows;
    }

    public static int playerCount(List<TransferSummaryLine> rows) {
        int count = 0;
        if (rows == null) {
            return 0;
        }
        for (TransferSummaryLine row : rows) {
            if (row != null && !row.section) {
                count++;
            }
        }
        return count;
    }

    private static TransferSummaryLine parsePlayer(String line) {
        Matcher outgoing = OUTGOING.matcher(line);
        if (outgoing.matches()) {
            return player(outgoing.group(1), outgoing.group(2), outgoing.group(3),
                    outgoing.group(4), outgoing.group(5));
        }
        Matcher incoming = INCOMING.matcher(line);
        if (incoming.matches()) {
            return player(incoming.group(1), incoming.group(2), incoming.group(3),
                    incoming.group(4), incoming.group(5));
        }
        Matcher league = LEAGUE.matcher(line);
        if (!league.matches()) {
            return null;
        }
        String rest = league.group(3).trim();
        String name = rest;
        String destination = "";
        int bracket = rest.indexOf(" [");
        if (bracket >= 0) {
            name = rest.substring(0, bracket).trim();
            String tail = rest.substring(bracket).trim();
            int close = tail.indexOf(']');
            destination = close >= 0 ? tail.substring(close + 1).trim() : tail;
        } else {
            int paren = rest.lastIndexOf(" (");
            if (paren > 0 && rest.endsWith(")")) {
                name = rest.substring(0, paren).trim();
                destination = rest.substring(paren + 2, rest.length() - 1).trim();
            }
        }
        return player(league.group(2), name, "", league.group(1), destination);
    }

    private static TransferSummaryLine section(String title) {
        return new TransferSummaryLine(true, title, "", "", "", "", "");
    }

    private static TransferSummaryLine player(String position, String name, String year,
                                              String overall, String destination) {
        return new TransferSummaryLine(false, "", position, name.trim(), year, overall, destination.trim());
    }
}
