package simulation;

/**
 * Portable data record for a team's seasonal history.
 */
public record TeamHistoryRecord(
    int year,
    int wins,
    int losses,
    int confWins,
    int confLosses,
    int rank,
    int pointsFor,
    int pointsAgainst,
    int yardsFor,
    int yardsAgainst,
    int turnovers,
    int prestige,
    int preseasonPrestige,
    String summary
) {
    public static TeamHistoryRecord fromCsv(String csv) {
        if (csv.contains(">")) {
            // New complex format or summary-based
            String[] parts = csv.split(">");
            String base = parts[0];
            String[] p = base.split(":");
            int yr = Integer.parseInt(p[0].trim());
            return new TeamHistoryRecord(yr, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, csv);
        }
        String[] p = csv.split(",");

        // The legacy format has some redundancy and uses dashes for records
        String[] record = p[1].split("-");
        return new TeamHistoryRecord(
            Integer.parseInt(p[0]),
            Integer.parseInt(record[0]),
            Integer.parseInt(record[1]),
            Integer.parseInt(p[4]),
            Integer.parseInt(p[5]),
            Integer.parseInt(p[3]),
            Integer.parseInt(p[6]),
            Integer.parseInt(p[7]),
            Integer.parseInt(p[8]),
            Integer.parseInt(p[9]),
            Integer.parseInt(p[10]),
            p.length > 11 ? Integer.parseInt(p[11]) : 0,
            p.length > 12 ? Integer.parseInt(p[12]) : 0,
            csv
        );
    }

    public String toCsv() {
        return year + "," + wins + "-" + losses + ",0-0," + rank + "," + confWins + "," + confLosses + "," + 
               pointsFor + "," + pointsAgainst + "," + yardsFor + "," + yardsAgainst + "," + turnovers;
    }
}
