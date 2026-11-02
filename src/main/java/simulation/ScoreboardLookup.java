package simulation;

import java.util.List;

/**
 * Maps a line of the league scoreboard ({@link League#getWeeklyScores()}) back to
 * the {@link Game} it describes, so a shell can open that game's box score.
 *
 * <p>Played-game lines look like {@code "<header>>#12 Alabama 35\n#4 Georgia 31"}
 * (away team first). Lines for games not yet played carry records instead of
 * scores and resolve to {@code null}.
 */
public final class ScoreboardLookup {

    private ScoreboardLookup() {
    }

    /**
     * @param weekIndex the scoreboard week the line came from (index into
     *                  {@link League#getWeeklyScores()})
     * @return the played game, or {@code null} if the line is not a played game
     */
    public static Game findPlayedGame(League league, int weekIndex, String scoreLine) {
        if (league == null || scoreLine == null) {
            return null;
        }
        int gt = scoreLine.indexOf('>');
        String body = gt >= 0 ? scoreLine.substring(gt + 1) : scoreLine;
        String[] sides = body.split("\n");
        if (sides.length < 2) {
            return null;
        }
        Team away = league.findTeam(teamName(sides[0]));
        Team home = league.findTeam(teamName(sides[1]));
        if (away == null || home == null || away == home) {
            return null;
        }

        Game byIndex = null;
        Game byWeekField = null;
        Game latest = null;
        List<Game> schedule = away.getGameSchedule();
        for (int i = 0; i < schedule.size(); i++) {
            Game g = schedule.get(i);
            if (g == null || !g.hasPlayed) {
                continue;
            }
            boolean sameMatchup = (g.homeTeam == home && g.awayTeam == away)
                    || (g.homeTeam == away && g.awayTeam == home);
            if (!sameMatchup) {
                continue;
            }
            latest = g;
            // A game played in week w is posted to scoreboard index w + 1,
            // which is also its 1-based slot in the team schedule.
            if (byIndex == null && i + 1 == weekIndex) {
                byIndex = g;
            }
            if (byWeekField == null && g.week == weekIndex) {
                byWeekField = g;
            }
        }
        if (byIndex != null) {
            return byIndex;
        }
        if (byWeekField != null) {
            return byWeekField;
        }
        return latest;
    }

    /** "#12 Texas A&M 35" → "Texas A&M". */
    static String teamName(String side) {
        String s = side.trim();
        s = s.replaceFirst("^#\\d+\\s+", "");
        s = s.replaceFirst("\\s+-?\\d+$", "");
        return s.trim();
    }
}
