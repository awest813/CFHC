package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Structured view of the plain-text digest from {@link League#buildWeekDigest(int)}
 * so a shell can render it as cards instead of one message blob. The digest
 * format is: a title line, then blank-line separated blocks. A block whose
 * first line is "Your Result: ..." or "Next Up: ..." is a one-line callout;
 * any other block is a section whose first line is its heading and whose
 * remaining lines are entries (a leading "- " bullet is stripped).
 */
public final class WeekDigestView {

    /** One headed section of the digest (Top 10, Headlines, Injury Report, ...). */
    public static final class Section {
        public final String heading;
        public final List<String> lines;

        Section(String heading, List<String> lines) {
            this.heading = heading;
            this.lines = Collections.unmodifiableList(lines);
        }
    }

    public final String title;
    /** Text after "Your Result: " (e.g. "W 31-17 vs Rival  (Game)"), or "" if absent. */
    public final String result;
    /** "W", "L", "T", "BYE" or "" derived from {@link #result}. */
    public final String outcome;
    /** Text after "Next Up: ", or "" if absent. */
    public final String nextUp;
    public final List<Section> sections;

    private WeekDigestView(String title, String result, String outcome, String nextUp, List<Section> sections) {
        this.title = title;
        this.result = result;
        this.outcome = outcome;
        this.nextUp = nextUp;
        this.sections = Collections.unmodifiableList(sections);
    }

    public static WeekDigestView parse(String digest) {
        String text = digest == null ? "" : digest.replace("\r", "").trim();
        String title = "";
        String result = "";
        String nextUp = "";
        List<Section> sections = new ArrayList<>();
        if (text.isEmpty()) {
            return new WeekDigestView(title, result, "", nextUp, sections);
        }
        String[] blocks = text.split("\n\\s*\n");
        for (int b = 0; b < blocks.length; b++) {
            List<String> lines = new ArrayList<>();
            for (String line : blocks[b].split("\n")) {
                if (!line.trim().isEmpty()) lines.add(line.trim());
            }
            if (lines.isEmpty()) continue;
            if (b == 0 && title.isEmpty() && !isCallout(lines.get(0))) {
                title = lines.remove(0);
                if (lines.isEmpty()) continue;
            }
            List<String> rest = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("Your Result:")) {
                    result = line.substring("Your Result:".length()).trim();
                } else if (line.startsWith("Next Up:")) {
                    nextUp = line.substring("Next Up:".length()).trim();
                } else {
                    rest.add(line);
                }
            }
            if (rest.isEmpty()) continue;
            String heading = rest.remove(0);
            List<String> entries = new ArrayList<>();
            for (String entry : rest) {
                entries.add(entry.startsWith("- ") ? entry.substring(2).trim() : entry);
            }
            sections.add(new Section(heading, entries));
        }
        return new WeekDigestView(title, result, outcomeOf(result), nextUp, sections);
    }

    private static boolean isCallout(String line) {
        return line.startsWith("Your Result:") || line.startsWith("Next Up:");
    }

    private static String outcomeOf(String result) {
        if (result.isEmpty()) return "";
        if (result.toUpperCase().startsWith("BYE")) return "BYE";
        char c = result.charAt(0);
        if ((c == 'W' || c == 'L' || c == 'T') && (result.length() == 1 || result.charAt(1) == ' ')) {
            return String.valueOf(c);
        }
        return "";
    }
}
