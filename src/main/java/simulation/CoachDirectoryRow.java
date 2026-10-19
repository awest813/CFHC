package simulation;

/**
 * The coach database glues a status tag onto the name ({@code Name (ALA)}, {@code Name [U]},
 * {@code Name [R]}). These helpers split that field for the row.
 */
public final class CoachDirectoryRow {
    private CoachDirectoryRow() {
    }

    public static String bareName(String field) {
        String trimmed = field == null ? "" : field.trim();
        if (trimmed.endsWith(" [R]") || trimmed.endsWith(" [U]")) {
            return trimmed.substring(0, trimmed.length() - 4).trim();
        }
        int open = trimmed.lastIndexOf(" (");
        if (open > 0 && trimmed.endsWith(")")) {
            String inside = trimmed.substring(open + 2, trimmed.length() - 1).trim();
            if (!inside.isEmpty() && inside.indexOf(' ') < 0) {
                return trimmed.substring(0, open).trim();
            }
        }
        return trimmed;
    }

    /** {@code Retired}, {@code Unemployed}, the school abbreviation, or empty. */
    public static String statusLabel(String field) {
        String trimmed = field == null ? "" : field.trim();
        if (trimmed.endsWith(" [R]")) {
            return "Retired";
        }
        if (trimmed.endsWith(" [U]")) {
            return "Unemployed";
        }
        int open = trimmed.lastIndexOf(" (");
        if (open > 0 && trimmed.endsWith(")")) {
            String inside = trimmed.substring(open + 2, trimmed.length() - 1).trim();
            if (!inside.isEmpty() && inside.indexOf(' ') < 0) {
                return inside;
            }
        }
        return "";
    }
}
