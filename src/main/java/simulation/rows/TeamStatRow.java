package simulation.rows;

/** One line of {@code Team.getTeamStatsStrCSV()} (split on "%\n"): {@code value,label,rank}. */
public record TeamStatRow(String value, String label, String rank) {

    public static TeamStatRow parse(String line) {
        Csv c = new Csv(line, ",");
        return new TeamStatRow(c.at(0), c.at(1), c.at(2));
    }
}
