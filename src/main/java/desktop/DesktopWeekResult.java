package desktop;

import simulation.Game;
import simulation.Team;

import java.util.List;

/**
 * Pure helpers for desktop week-result presentation (testable without Swing).
 */
final class DesktopWeekResult {

    private DesktopWeekResult() {}

    /**
     * Finds the user-team game that was played during the advance that started
     * at {@code weekBefore} ({@link simulation.League#currentWeek} before
     * {@link simulation.SeasonController#advanceWeek()}).
     *
     * <p>Regular-season weeks play {@code schedule[weekBefore - 1]}. Conference
     * championship and later games are typically appended after the regular slate.
     *
     * @return the played game, or {@code null} if none should be summarized
     */
    static Game findPlayedGame(Team userTeam, int weekBefore, int regSeasonWeeks) {
        if (userTeam == null || weekBefore < 1 || regSeasonWeeks < 2) {
            return null;
        }
        List<Game> schedule = userTeam.getGameSchedule();
        if (schedule == null || schedule.isEmpty()) {
            return null;
        }

        if (weekBefore < regSeasonWeeks - 1) {
            int idx = weekBefore - 1;
            if (idx >= 0 && idx < schedule.size()) {
                return usableResult(schedule.get(idx));
            }
            return null;
        }

        // CCG / bowls / playoffs: show the newest played non-BYE at or after the
        // championship slot so we do not re-show an earlier regular-season game.
        int minIdx = Math.max(0, regSeasonWeeks - 1);
        for (int i = schedule.size() - 1; i >= minIdx; i--) {
            Game g = usableResult(schedule.get(i));
            if (g != null) {
                return g;
            }
        }
        return null;
    }

    /**
     * Most recently played non-BYE game on the schedule (dashboard "Recent Outcome").
     */
    static Game findMostRecentPlayed(Team userTeam) {
        if (userTeam == null) {
            return null;
        }
        List<Game> schedule = userTeam.getGameSchedule();
        if (schedule == null || schedule.isEmpty()) {
            return null;
        }
        Game last = null;
        for (Game g : schedule) {
            Game usable = usableResult(g);
            if (usable != null) {
                last = usable;
            }
        }
        return last;
    }

    static String opponentAbbr(Game g, Team userTeam) {
        if (g == null || userTeam == null) {
            return "Opponent";
        }
        if (g.homeTeam == userTeam) {
            return g.awayTeam != null ? g.awayTeam.getAbbr() : "Opponent";
        }
        return g.homeTeam != null ? g.homeTeam.getAbbr() : "Opponent";
    }

    static String opponentName(Game g, Team userTeam) {
        if (g == null || userTeam == null) {
            return "Opponent";
        }
        if (g.homeTeam == userTeam) {
            return g.awayTeam != null ? g.awayTeam.getName() : "Opponent";
        }
        return g.homeTeam != null ? g.homeTeam.getName() : "Opponent";
    }

    static boolean userIsHome(Game g, Team userTeam) {
        return g != null && userTeam != null && g.homeTeam == userTeam;
    }

    static int userScore(Game g, Team userTeam) {
        if (g == null || userTeam == null) {
            return 0;
        }
        return userIsHome(g, userTeam) ? g.homeScore : g.awayScore;
    }

    static int opponentScore(Game g, Team userTeam) {
        if (g == null || userTeam == null) {
            return 0;
        }
        return userIsHome(g, userTeam) ? g.awayScore : g.homeScore;
    }

    private static Game usableResult(Game g) {
        if (g == null || !g.hasPlayed || "BYE WEEK".equals(g.gameName)) {
            return null;
        }
        return g;
    }
}
