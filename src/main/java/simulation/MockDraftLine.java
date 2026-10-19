package simulation;

/**
 * Display fields for one {@link Player#getMockDraftStr(int, int, String)} row.
 * The engine string stays {@code Round R, Pick N : Club\nPOS Name\nYear>\nSchool\nOverall: N}.
 */
public final class MockDraftLine {
    public final boolean parsed;
    public final String round;
    public final String pick;
    public final String pickLabel;
    public final String player;
    public final String year;
    public final String school;
    public final String proTeam;
    public final String overall;
    public final String raw;

    private MockDraftLine(boolean parsed, String round, String pick, String pickLabel,
                          String player, String year, String school, String proTeam,
                          String overall, String raw) {
        this.parsed = parsed;
        this.round = round;
        this.pick = pick;
        this.pickLabel = pickLabel;
        this.player = player;
        this.year = year;
        this.school = school;
        this.proTeam = proTeam;
        this.overall = overall;
        this.raw = raw == null ? "" : raw;
    }

    public static MockDraftLine parse(String raw) {
        if (raw == null || raw.isEmpty() || raw.indexOf('>') < 0) {
            return message(raw);
        }
        String left = raw.substring(0, raw.indexOf('>'));
        String right = raw.substring(raw.indexOf('>') + 1);
        String[] leftLines = left.split("\n", -1);
        String header = leftLines.length > 0 ? leftLines[0].trim() : "";
        String player = leftLines.length > 1 ? leftLines[1].trim() : "";
        String year = leftLines.length > 2 ? leftLines[2].trim() : "";

        String round = "";
        String pick = "";
        String proTeam = "";
        int roundAt = header.indexOf("Round ");
        int pickAt = header.indexOf(", Pick ");
        int clubAt = header.indexOf(" : ");
        if (roundAt == 0 && pickAt > 6 && clubAt > pickAt) {
            round = header.substring(6, pickAt).trim();
            pick = header.substring(pickAt + 7, clubAt).trim();
            proTeam = header.substring(clubAt + 3).trim();
        }
        if (round.isEmpty() || pick.isEmpty()) {
            return message(raw);
        }

        String school = "";
        String overall = "";
        for (String line : right.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("Overall:")) {
                overall = trimmed.substring("Overall:".length()).trim();
            } else if (school.isEmpty()) {
                school = trimmed;
            }
        }
        return new MockDraftLine(true, round, pick, "R" + round + " · " + pick,
                player, year, school, proTeam, overall, raw);
    }

    /** Year, school, and pro club, skipping any blank piece. */
    public String metaLine() {
        StringBuilder sb = new StringBuilder();
        appendPiece(sb, year);
        appendPiece(sb, school);
        appendPiece(sb, proTeam);
        return sb.toString();
    }

    /** One scannable desktop line: pick, player, overall, school, pro club. */
    public String boardLine() {
        if (!parsed) {
            return raw;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(pickLabel);
        if (!player.isEmpty()) {
            sb.append("  ").append(player);
        }
        if (!overall.isEmpty()) {
            sb.append("  ").append(overall);
        }
        if (!school.isEmpty()) {
            sb.append("  ").append(school);
        }
        if (!proTeam.isEmpty()) {
            sb.append("  ").append(proTeam);
        }
        return sb.toString();
    }

    private static void appendPiece(StringBuilder sb, String piece) {
        if (piece == null || piece.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("  ·  ");
        }
        sb.append(piece);
    }

    private static MockDraftLine message(String raw) {
        return new MockDraftLine(false, "", "", "", "", "", "", "", "", raw);
    }
}
