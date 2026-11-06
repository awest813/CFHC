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
                        int week = (j + r + k) % League.REGULAR_SEASON_GAMES;
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
        league.rebuildFcsNamePool();

        // OOC pairing (skipped under universal promotion/relegation). Each pairing
        // happens inside its OOC week; remember that week so the game can be placed there.
        java.util.IdentityHashMap<Game, Integer> oocWeekOf = new java.util.IdentityHashMap<>();
        if (!league.enableUnivProRel) {
            for (int week = 0; week < League.REGULAR_SEASON_GAMES; week++) {

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
                        oocWeekOf.put(scheduleOocGame(league, a, rival), week);
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

                    oocWeekOf.put(scheduleOocGame(league, a, b), week);

                    a.addOocTeam(b);
                    b.addOocTeam(a);

                    availTeams.remove(a);
                    availTeams.remove(b);
                }
            }

            // Put every game in its week, so both teams hold it at the same schedule
            // index (Conference.playWeek plays index currentWeek - 1 for each team).
            alignToWeeks(league, oocWeekOf);

            // Rivalry guarantee: an unmet cross-conference pair trades its FCS
            // filler games for a head-to-head matchup.
            scheduleRivalryGuarantees(league);

            // Marquee tagging: rivalry games, each team's senior day (final home
            // game) and homecoming (home game nearest mid-season).
            tagMarqueeGames(league);

            // Pad every schedule to the full regular-season slot count (byes)
            Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
            bye.setRankTeamPollScore(league.teamList.size());
            int targetGames = League.REGULAR_SEASON_GAMES;
            for (Team t : league.teamList) {
                while (t.getGameSchedule().size() < targetGames) {
                    t.addGameToSchedule(new Game(t, bye, "BYE WEEK"));
                }
            }
        }
    }

    private static Conference conferenceOf(League league, Team t) {
        for (Conference c : league.conferences) {
            if (c.confTeams.contains(t)) return c;
        }
        return null;
    }

    /** Creates and attaches one OOC game; {@link #alignToWeeks} moves it to its week. */
    private static Game scheduleOocGame(League league, Team a, Team b) {
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
        return oocGame;
    }

    /**
     * Rebuilds each team's regular-season schedule week by week: OOC games at the
     * week they were paired in, conference games filling the conference's other
     * weeks in round order, byes in any gap. OOC games used to be appended after
     * the conference games, so conferences with different conference-game counts
     * held the same OOC game at different indexes. Since a game plays once, at
     * the first team's index, teams played two games some weeks and none in others
     * (and the schedule screen's week labels did not match).
     */
    private static void alignToWeeks(League league, java.util.IdentityHashMap<Game, Integer> oocWeekOf) {
        int weeks = League.REGULAR_SEASON_GAMES;
        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
        bye.setRankTeamPollScore(league.teamList.size());
        for (Team t : league.teamList) {
            Game[] slots = new Game[weeks];
            ArrayList<Game> conf = new ArrayList<>();
            ArrayList<Game> extra = new ArrayList<>();
            for (Game g : t.gameSchedule) {
                if (g == null) continue;
                Integer w = oocWeekOf.get(g);
                if (w == null) {
                    conf.add(g);
                } else if (w >= 0 && w < weeks && slots[w] == null) {
                    slots[w] = g;
                } else {
                    extra.add(g);
                }
            }
            // Conference round r goes to the conference's r-th non-OOC week (all teams in a
            // conference share those weeks). A team idle in a round gets a bye that week.
            java.util.Set<Integer> oocWeeks = new java.util.HashSet<>(t.getOocWeeks());
            ArrayList<Integer> confWeeks = new ArrayList<>();
            for (int w = 0; w < weeks; w++) {
                if (!oocWeeks.contains(w)) confWeeks.add(w);
            }
            Conference c = conferenceOf(league, t);
            ArrayList<Game> unplaced = new ArrayList<>();
            for (Game g : conf) {
                Integer round = c != null ? c.confRoundOf.get(g) : null;
                if (round != null && round < confWeeks.size() && slots[confWeeks.get(round)] == null) {
                    slots[confWeeks.get(round)] = g;
                } else {
                    unplaced.add(g);
                }
            }
            // Games without a recorded round (other schedule styles) fill free weeks in order.
            for (int w = 0, next = 0; w < weeks && next < unplaced.size(); w++) {
                if (slots[w] == null && !oocWeeks.contains(w)) {
                    slots[w] = unplaced.get(next);
                    unplaced.set(next++, null);
                }
            }
            for (Game g : unplaced) {
                if (g != null) extra.add(g);
            }
            t.gameSchedule.clear();
            for (int w = 0; w < weeks; w++) {
                Game g = slots[w];
                if (g == null) {
                    g = extra.isEmpty() ? new Game(t, bye, "BYE WEEK") : extra.remove(0);
                }
                t.gameSchedule.add(g);
            }
            t.gameSchedule.addAll(extra); // only if a team somehow has more games than weeks
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


    private static void removeGame(Team t, Game g) {
        t.gameSchedule.remove(g);
    }

    /**
     * Cross-conference rivalries without a meeting swap their FCS fillers for a
     * head-to-head game, in a week where both have one (so the game sits at the
     * same schedule index for both teams).
     */
    private static void scheduleRivalryGuarantees(League league) {
        for (Team a : league.teamList) {
            if (!isRivalryCandidate(a) || a.getRivalName().isEmpty()) continue;
            Team b = league.findTeam(a.getRivalName());
            if (b == null || b == a || alreadyScheduled(a, b)) continue;

            int week = -1;
            int n = Math.min(a.gameSchedule.size(), b.gameSchedule.size());
            for (int w = 0; w < n && week < 0; w++) {
                if (isFcsFiller(a, a.gameSchedule.get(w)) && isFcsFiller(b, b.gameSchedule.get(w))) {
                    week = w;
                }
            }
            if (week < 0) continue;

            Game aFiller = a.gameSchedule.get(week);
            Game bFiller = b.gameSchedule.get(week);
            removeGame(opponentOf(aFiller, a), aFiller);
            removeGame(opponentOf(bFiller, b), bFiller);
            Game rivalry = new Game(a, b, "OOC");
            a.gameSchedule.set(week, rivalry);
            b.gameSchedule.set(week, rivalry);
        }
    }

    private static boolean isFcsFiller(Team t, Game g) {
        if (g == null || g.isByeWeek()) return false;
        Team opp = g.homeTeam == t ? g.awayTeam : g.homeTeam;
        return opp != null && opp.getConference() != null && opp.getConference().contains("FCS");
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
     * mid-season). Runs on every schedule build; the flags are also persisted
     * per game in saves (GameRecord) so mid-season load keeps them intact.
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
