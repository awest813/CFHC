package simulation.rows;

/**
 * One line of {@code League.getConfStandings()} / {@code getTeamRankings()}:
 * {@code rank,team,record,stat}. No rank but a team name is a conference
 * header; neither is the spacer between conferences.
 */
public record RankingRow(RowKind kind, String rank, String team, String record, String stat) {

    public static RankingRow parse(String line) {
        Csv c = new Csv(line, ",");
        String rank = c.at(0);
        String team = c.at(1);
        RowKind kind = rank.isEmpty() ? (team.isEmpty() ? RowKind.SPACER : RowKind.SECTION) : RowKind.ENTRY;
        return new RankingRow(kind, rank, team, c.at(2), c.at(3));
    }
}
