package simulation;

import java.util.List;

/**
 * A complete, portable snapshot of a League's state.
 * This can be serialized to any format (JSON, XML, Custom)
 * and is entirely platform-agnostic.
 */
public record LeagueRecord(
    String leagueName,
    int year,
    int currentWeek,
    List<ConferenceRecord> conferences,
    List<PlayerRecord> leagueHoF,
    List<DataRecord> leagueRecords,
    String heismanWinnerName,
    String nationalChampName,
    List<GameRecord> scheduledGames
) {
    // Nested records for structured hierarchy
    public record ConferenceRecord(
        String name,
        List<Integer> oocWeeks,
        List<TeamRecord> teams
    ) {}

    public record TeamRecord(
        String name,
        String abbr,
        int prestige,
        int wins,
        int losses,
        List<Integer> oocWeeks,
        List<String> oocOpponentNames,
        float teamPollScore,
        int rankTeamPollScore,
        StaffRecord headCoach,
        StaffRecord offenseCoach,
        StaffRecord defenseCoach,
        List<PlayerRecord> roster,
        List<TeamHistoryRecord> history,
        List<DataRecord> records
    ) {}

    /**
     * One row per unique {@link Game} (deduped by identity across team schedules).
     * {@link #slot()} is the restore order (append order for {@link Team#addGameToSchedule(Game)}).
     */
    public record GameRecord(
            int slot,
            String homeName,
            String awayName,
            String gameName,
            int week,
            boolean played,
            int homeScore,
            int awayScore
    ) {
        private static String tabSafe(String s) {
            return s == null ? "" : s.replace("\t", " ");
        }

        public String toSaveLine() {
            return slot + "\t" + tabSafe(homeName) + "\t" + tabSafe(awayName) + "\t" + tabSafe(gameName) + "\t"
                    + week + "\t" + (played ? 1 : 0) + "\t" + homeScore + "\t" + awayScore;
        }

        public static GameRecord fromSaveLine(String line) {
            String[] p = line.split("\t", -1);
            if (p.length < 8) {
                throw new IllegalArgumentException("Bad GM line (expected 8 tab fields): " + line);
            }
            return new GameRecord(
                    Integer.parseInt(p[0]),
                    p[1], p[2], p[3],
                    Integer.parseInt(p[4]),
                    "1".equals(p[5]),
                    Integer.parseInt(p[6]),
                    Integer.parseInt(p[7])
            );
        }

        public static GameRecord fromGame(Game g, int slot) {
            String gn = g.gameName == null ? "" : g.gameName.replace("\t", " ");
            return new GameRecord(
                    slot,
                    Team.normalizeRecordText(g.homeTeam.getName()),
                    Team.normalizeRecordText(g.awayTeam.getName()),
                    gn,
                    g.week,
                    g.hasPlayed,
                    g.homeScore,
                    g.awayScore
            );
        }
    }
}
