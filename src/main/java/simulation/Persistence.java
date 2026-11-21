package simulation;

/**
 * Utility for simulation-wide data persistence and formatting.
 */
public final class Persistence {
    private Persistence() {
    }

    static String escapeAmp(String s) {
        return s == null ? null : s.replace("&", "_AMP_");
    }

    static String unescapeAmp(String s) {
        return s == null ? null : s.replace("_AMP_", "&");
    }

    /**
     * Characters that separate fields somewhere in a save line: CSV commas, the
     * {@code team: POS} colon, {@code TO:} list separators, staff history carets,
     * line breaks, and {@code %} itself (the escape character).
     */
    static final String FIELD_DELIMITERS = "%,:;|^\t\n\r";

    /**
     * Player CSV text fields. {@code &} is written as {@code _AMP_} there (as it
     * always was); a real {@code _} is escaped so a name holding "_AMP_" survives.
     */
    static final String PLAYER_FIELD_CHARS = FIELD_DELIMITERS + "_";

    /** Staff name: {@code &} separates the staff CSV sections. */
    static final String STAFF_NAME_CHARS = FIELD_DELIMITERS + "&";

    /** One staff history item: items are joined with {@code ^} in the last section. */
    static final String STAFF_HISTORY_CHARS = "%^\n\r";

    /**
     * {@code %XX}-encodes every character of {@code s} found in {@code chars}
     * (and nothing else, so ordinary text is written unchanged). Read back with
     * {@link #decodeField}. Names never could contain {@code %} (see
     * {@code League.isNameValid}), so text written before this escape decodes
     * to itself.
     */
    static String encodeField(String s, String chars) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (chars.indexOf(c) >= 0) {
                if (sb == null) {
                    sb = new StringBuilder(s.length() + 8);
                    sb.append(s, 0, i);
                }
                sb.append('%').append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? s : sb.toString();
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** Reverses {@link #encodeField}; a {@code %} not followed by two hex digits stays as it is. */
    static String decodeField(String s) {
        if (s == null || s.indexOf('%') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = hexDigit(s.charAt(i + 1));
                int lo = hexDigit(s.charAt(i + 2));
                if (hi >= 0 && lo >= 0) {
                    sb.append((char) ((hi << 4) | lo));
                    i += 2;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        return -1;
    }

    /** A player CSV text field: delimiters (and {@code _}) escaped, then {@code &} as {@code _AMP_}. */
    static String playerField(String s) {
        return escapeAmp(encodeField(s, PLAYER_FIELD_CHARS));
    }

    /** Reverses {@link #playerField}; older saves (raw text, {@code _AMP_}) read as before. */
    static String readPlayerField(String s) {
        return decodeField(unescapeAmp(s));
    }

    public static String toCsv(PlayerRecord r) {
        StringBuilder sb = new StringBuilder(512); // a player line is ~350-450 chars
        // Basic Part
        String pos = r.position();
        if (r.teamName() != null && !r.teamName().isEmpty()) {
            pos = playerField(r.teamName()) + ": " + pos;
        }
        sb.append(pos).append(",").append(playerField(r.name())).append(",").append(r.year()).append(",")

          .append(r.homeState()).append(",").append(r.character()).append(",").append(r.ratIntelligence()).append(",")
          .append(r.recruitRating()).append(",").append(r.isTransfer()).append(",").append(r.wasRedshirt()).append(",")
          .append(r.ratPot()).append(",").append(r.ratDurability()).append(",").append(r.ratOvr()).append(",")
          .append(r.cost()).append(",")
          .append(r.attributes()[0]).append(",").append(r.attributes()[1]).append(",")
          .append(r.attributes()[2]).append(",").append(r.attributes()[3]).append(",")
          .append(r.height()).append(",").append(r.weight()).append(",")
          .append(r.isSuspended()).append(",").append(r.weeksSuspended()).append(",")
          .append(r.troubledTimes()).append(",").append(r.talentNFL()).append(",")
          .append(r.isRedshirt()).append(",").append(r.isMedicalRS()).append(",")
          .append(r.isGradTransfer()).append(",").append(r.isWalkOn()).append(",")
          .append(r.isInjured()).append(",").append(r.injuryDuration()).append(",")
          .append(r.injuryDescription() == null ? "" : r.injuryDescription().replace(",", ";").replace("&", "_AMP_"))
          .append(",").append(playerField(r.archetypeTag() == null ? "" : r.archetypeTag()))
          .append(",").append(playerField(r.mentorName() == null ? "" : r.mentorName()))
          .append(",").append(r.ratOvrStart()).append(",").append(r.ratImprovement());
        
        sb.append("&");

        // Stats Part
        for (int i = 0; i < r.stats().length; i++) {
            sb.append(r.stats()[i]);
            if (i < r.stats().length - 1) sb.append(",");
        }
        sb.append("&");

        // Career Stats Part
        for (int i = 0; i < r.careerStats().size(); i++) {
            int[] yrStats = r.careerStats().get(i);
            for (int j = 0; j < yrStats.length; j++) {
                sb.append(yrStats[j]);
                if (j < yrStats.length - 1) sb.append(",");
            }
            if (i < r.careerStats().size() - 1) sb.append("#");
        }
        sb.append("&");

        // Awards Part
        for (int i = 0; i < r.awards().length; i++) {
            sb.append(r.awards()[i]);
            if (i < r.awards().length - 1) sb.append(",");
        }

        return sb.toString();
    }


    public static String toCsv(StaffRecord r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.position()).append(",").append(encodeField(r.name(), STAFF_NAME_CHARS)).append(",").append(r.age()).append(",")
          .append(r.year()).append(",").append(r.ratOff()).append(",").append(r.ratDef()).append(",")
          .append(r.ratTalent()).append(",").append(r.ratDiscipline()).append(",").append(r.offStrat())
          .append(",").append(r.defStrat()).append(",").append(r.contractYear()).append(",")
          .append(r.contractLength()).append(",").append(r.baselinePrestige()).append(",")
          .append(r.retired()).append(",").append(r.ratOvr()).append(",").append(r.ratImprovement())
          .append(",").append(r.user()).append(",").append(r.coachSkillXp()).append(",")
          .append(r.coachSkillRanksBits()).append(",")
          .append(r.schemeArchetypes() != null ? r.schemeArchetypes() : "").append(",&");
        
        // Stats
        for (int i = 0; i < r.stats().length; i++) {
            sb.append(r.stats()[i]);
            if (i < r.stats().length - 1) sb.append(",");
        }
        sb.append("&");

        // Awards
        for (int i = 0; i < r.awards().length; i++) {
            sb.append(r.awards()[i]);
            if (i < r.awards().length - 1) sb.append(",");
        }
        sb.append("&");

        // History
        for (int i = 0; i < r.history().size(); i++) {
            if (i > 0) sb.append("^");
            sb.append(encodeField(r.history().get(i), STAFF_HISTORY_CHARS));
        }

        return sb.toString();
    }
}
