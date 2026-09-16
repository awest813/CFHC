package simulation;

import java.util.ArrayList;

/**
 * Builds regular-season conference and OOC schedules for a {@link League}.
 *
 * <p>Extracted from {@link League#setupSeason()} so scheduling can evolve without
 * growing the League god object further. Same-package access to League fields
 * ({@code conferences}, {@code teamList}, etc.) is intentional.
 */
public final class ScheduleManager {

    private ScheduleManager() {}

    /**
     * Builds conference schedules, assigns OOC weeks, pairs OOC opponents
     * (with FCS fill-ins), and pads short schedules with BYE weeks.
     *
     * <p>Mutates {@code league} in place. Caller remains responsible for
     * prestige averages, news lists, and other post-schedule season setup.
     */
    public static void scheduleRegularSeason(League league) {
        // Rivalries exist before scheduling so the OOC slate can honor them.
        league.generateRivalries();

        for (int i = 0; i < league.conferences.size(); ++i) {
            league.conferences.get(i).setUpSchedule();
        }

        // Decide OOC schedule weeks per conference / team
        for (int r = 0; r < league.regSeasonWeeks; r++) {
            int j = 0;
            int k = 0;

            for (int c = 0; c < league.conferences.size(); c++) {
                if (r < league.conferences.get(c).oocGames
                        && league.conferences.get(c).confTeams.size()
                        >= league.conferences.get(c).minConfTeams) {
                    boolean scheduled = false;
                    k = k + (int) (SimRandom.nextDouble() * 4);
                    while (!scheduled) {
                        int week = (j + r + k) % (league.regSeasonWeeks - 1);
                        if (!league.conferences.get(c).oocWeeks.contains(week)) {
                            league.conferences.get(c).oocWeeks.add(week);
                            for (int t = 0; t < league.conferences.get(c).confTeams.size(); t++) {
                                league.conferences.get(c).confTeams.get(t).addOocWeek(week);
                            }
                            scheduled = true;
                        } else {
                            k = k + 2;
                        }
                    }
                    j++;
                } else if (league.conferences.get(c).confTeams.size()
                        < league.conferences.get(c).minConfTeams
                        && r < league.conferences.get(c).oocGames) {
                    for (int t = 0; t < league.conferences.get(c).confTeams.size(); t++) {
                        league.conferences.get(c).confTeams.get(t).addOocWeek(r);
                    }
                }
            }
        }

        // FCS name pool (exclude names already used by FBS teams)
        ArrayList<String> leagueTeams = new ArrayList<>();
        for (int i = 0; i < league.teamList.size(); i++) {
            leagueTeams.add(league.teamList.get(i).getName());
        }

        league.teamsFCSList = new ArrayList<>();
        for (int i = 0; i < league.teamsFCS.length; i++) {
            if (!leagueTeams.contains(league.teamsFCS[i])) {
                league.teamsFCSList.add(league.teamsFCS[i]);
            }
        }

        // OOC pairing (skipped under universal promotion/relegation)
        if (!league.enableUnivProRel) {
            for (int week = 0; week < (league.regSeasonWeeks - 1); week++) {

                ArrayList<Team> availTeams = new ArrayList<>();
                for (int t = 0; t < league.teamList.size(); t++) {
                    if (league.teamList.get(t).getOocWeeks().contains(week)) {
                        availTeams.add(league.teamList.get(t));
                    }
                }

                while (availTeams.size() > 0) {
                    int selTeamA = (int) (availTeams.size() * SimRandom.nextDouble());
                    Team a = availTeams.get(selTeamA);

                    // Rivalry week: when the declared rival shares this OOC window, play it.
                    Team rival = a.getRivalName().isEmpty() ? null : league.findTeam(a.getRivalName());
                    if (rival != null && availTeams.contains(rival) && !a.getOocTeams().contains(rival)) {
                        scheduleOocGame(league, a, rival);
                        a.addOocTeam(rival);
                        rival.addOocTeam(a);
                        availTeams.remove(a);
                        availTeams.remove(rival);
                        continue;
                    }

                    ArrayList<Team> availTeamsB = new ArrayList<>();
                    for (int k = 0; k < availTeams.size(); k++) {
                        if (!availTeams.get(k).getConference().equals(a.getConference())
                                && !a.getOocTeams().contains(availTeams.get(k))) {
                            availTeamsB.add(availTeams.get(k));
                        }
                    }
                    Team b;

                    if (availTeamsB.isEmpty()) {
                        if (league.teamsFCSList.isEmpty()) {
                            b = new Team("Antdroid Tech", "FCS", "FCS Division",
                                    (int) (SimRandom.nextDouble() * 40), "FCS1", 0, league, false);
                        } else {
                            b = new Team(
                                    league.teamsFCSList.get(
                                            (int) (league.teamsFCSList.size() * SimRandom.nextDouble())),
                                    "FCS", "FCS Division",
                                    (int) (SimRandom.nextDouble() * 40), "FCS1", 0, league, false);
                        }
                    } else {
                        int selTeamB = (int) (availTeamsB.size() * SimRandom.nextDouble());
                        b = availTeamsB.get(selTeamB);
                    }

                    if (a.getGameSchedule().size() != b.getGameSchedule().size()) {
                        PlatformLog.d("league", "setupSeason: week " + week + " " + a.getName()
                                + " size" + a.getGameSchedule().size() + " vs " + b.getName()
                                + " size" + b.getGameSchedule().size());
                    }

                    scheduleOocGame(league, a, b);

                    a.addOocTeam(b);
                    b.addOocTeam(a);

                    availTeams.remove(a);
                    availTeams.remove(b);
                }
            }

            // Rivalry guarantee: an unmet cross-conference pair trades its FCS
            // filler games for a head-to-head matchup.
            scheduleRivalryGuarantees(league);

            // Marquee tagging: rivalry games, each team's senior day (final home
            // game) and homecoming (home game nearest mid-season).
            tagMarqueeGames(league);

            // Ensure every team has at least regSeasonWeeks-1 games
            Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
            bye.setRankTeamPollScore(league.teamList.size());
            int targetGames = league.regSeasonWeeks - 1;
            for (Team t : league.teamList) {
                while (t.getGameSchedule().size() < targetGames) {
                    t.addGameToSchedule(new Game(t, bye, "BYE WEEK"));
                }
            }
        }
    }

    /** Creates and attaches one OOC game (schedule-append semantics preserved). */
    private static void scheduleOocGame(League league, Team a, Team b) {
        Game oocGame = new Game(a, b, "OOC");

        // Append OOC games (don't insert by week — insertion fails when
        // the schedule is too small for the week index, silently dropping
        // games for teams with fewer conf games).
        if (!a.getConference().contains("Independent") && !a.getConference().contains("FCS")
                || a.getConference().contains("Independent")) {
            a.addGameToSchedule(oocGame);
        }
        if (!b.getConference().contains("Independent") && !b.getConference().contains("FCS")
                || b.getConference().contains("Independent")) {
            b.addGameToSchedule(oocGame);
        }
    }

    /** True when the two teams already meet on the current schedule. */
    private static boolean alreadyScheduled(Team a, Team b) {
        for (Game g : a.gameSchedule) {
            if (g != null && (g.homeTeam == b || g.awayTeam == b)) {
                return true;
            }
        }
        return false;
    }

    private static Game findFcsFillerGame(Team t) {
        for (Game g : t.gameSchedule) {
            if (g == null || g.isByeWeek()) continue;
            Team opp = g.homeTeam == t ? g.awayTeam : g.homeTeam;
            if (opp != null && opp.getConference() != null && opp.getConference().contains("FCS")) {
                return g;
            }
        }
        return null;
    }

    private static void removeGame(Team t, Game g) {
        t.gameSchedule.remove(g);
    }

    /** Cross-conference rivalries without a meeting swap their FCS fillers for a head-to-head game. */
    private static void scheduleRivalryGuarantees(League league) {
        for (Team a : league.teamList) {
            if (!isRivalryCandidate(a) || a.getRivalName().isEmpty()) continue;
            Team b = league.findTeam(a.getRivalName());
            if (b == null || b == a || alreadyScheduled(a, b)) continue;

            Game aFiller = findFcsFillerGame(a);
            Game bFiller = findFcsFillerGame(b);
            if (aFiller == null || bFiller == null) continue;

            removeGame(a, aFiller);
            removeGame(opponentOf(aFiller, a), aFiller);
            removeGame(b, bFiller);
            removeGame(opponentOf(bFiller, b), bFiller);

            Game rivalry = new Game(a, b, "OOC");
            a.addGameToSchedule(rivalry);
            b.addGameToSchedule(rivalry);
        }
    }

    /** The other participant of a scheduled game. */
    private static Team opponentOf(Game g, Team t) {
        return g.homeTeam == t ? g.awayTeam : g.homeTeam;
    }

    private static boolean isRivalryCandidate(Team t) {
        String conf = t.getConference();
        return conf != null && !conf.contains("FCS") && !conf.equals("BYE");
    }

    /**
     * Tags marquee games on every team's schedule: games between declared rivals,
     * each team's senior day (final home game) and homecoming (home game nearest
     * mid-season). Flags are session-only and re-tagged on every schedule build.
     */
    private static void tagMarqueeGames(League league) {
        for (Team t : league.teamList) {
            if (!isRivalryCandidate(t)) continue;

            ArrayList<Integer> homeGameIndices = new ArrayList<>();
            for (int i = 0; i < t.gameSchedule.size(); i++) {
                Game g = t.gameSchedule.get(i);
                if (g == null || g.isByeWeek()) continue;
                if (g.homeTeam == t) homeGameIndices.add(i);
            }
            if (!homeGameIndices.isEmpty()) {
                t.gameSchedule.get(homeGameIndices.get(homeGameIndices.size() - 1)).seniorDay = true;
                int midSeason = t.gameSchedule.size() / 2;
                int homecomingIdx = homeGameIndices.get(0);
                int bestDistance = Integer.MAX_VALUE;
                for (int idx : homeGameIndices) {
                    int distance = Math.abs(idx - midSeason);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        homecomingIdx = idx;
                    }
                }
                t.gameSchedule.get(homecomingIdx).homecomingGame = true;
            }
        }

        // Rivalry flags: any scheduled game between declared rivals.
        for (Team a : league.teamList) {
            if (a.getRivalName().isEmpty()) continue;
            Team b = league.findTeam(a.getRivalName());
            if (b == null) continue;
            for (Game g : a.gameSchedule) {
                if (g != null && !g.isByeWeek() && (g.homeTeam == b || g.awayTeam == b)) {
                    g.rivalryGame = true;
                }
            }
        }
    }
}
