package simulation;

/**
 * Portable data object for a team's seasonal history.
 *
 * <p>The public fields keep legacy Android adapters and chart code source
 * compatible, while the accessor methods preserve record-style call sites used
 * by the newer desktop/save code.</p>
 */
public final class TeamHistoryRecord {
    public final int year;
    public final int wins;
    public final int losses;
    public final int confWins;
    public final int confLosses;
    public final int rank;
    public final int pointsFor;
    public final int pointsAgainst;
    public final int yardsFor;
    public final int yardsAgainst;
    public final int turnovers;
    public final int prestige;
    public final int preseasonPrestige;
    public final String summary;

    public TeamHistoryRecord(
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
            String summary) {
        this.year = year;
        this.wins = wins;
        this.losses = losses;
        this.confWins = confWins;
        this.confLosses = confLosses;
        this.rank = rank;
        this.pointsFor = pointsFor;
        this.pointsAgainst = pointsAgainst;
        this.yardsFor = yardsFor;
        this.yardsAgainst = yardsAgainst;
        this.turnovers = turnovers;
        this.prestige = prestige;
        this.preseasonPrestige = preseasonPrestige;
        this.summary = summary;
    }

    public int year() { return year; }
    public int wins() { return wins; }
    public int losses() { return losses; }
    public int confWins() { return confWins; }
    public int confLosses() { return confLosses; }
    public int rank() { return rank; }
    public int pointsFor() { return pointsFor; }
    public int pointsAgainst() { return pointsAgainst; }
    public int yardsFor() { return yardsFor; }
    public int yardsAgainst() { return yardsAgainst; }
    public int turnovers() { return turnovers; }
    public int prestige() { return prestige; }
    public int preseasonPrestige() { return preseasonPrestige; }
    public String summary() { return summary; }

    public static TeamHistoryRecord fromCsv(String csv) {
        if (csv.contains(">")) {
            String[] parts = csv.split(">");
            String base = parts[0];
            String[] p = base.split(":");
            int yr = Integer.parseInt(p[0].trim());
            return new TeamHistoryRecord(yr, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, csv);
        }
        String[] p = csv.split(",");

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
        return year + "," + wins + "-" + losses + ",0-0," + rank + "," + confWins + "," + confLosses + ","
                + pointsFor + "," + pointsAgainst + "," + yardsFor + "," + yardsAgainst + "," + turnovers;
    }
}
