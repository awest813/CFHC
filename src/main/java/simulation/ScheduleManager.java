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
                    k = k + (int) (Math.random() * 4);
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
                    int selTeamA = (int) (availTeams.size() * Math.random());
                    Team a = availTeams.get(selTeamA);

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
                                    (int) (Math.random() * 40), "FCS1", 0, league, false);
                        } else {
                            b = new Team(
                                    league.teamsFCSList.get(
                                            (int) (league.teamsFCSList.size() * Math.random())),
                                    "FCS", "FCS Division",
                                    (int) (Math.random() * 40), "FCS1", 0, league, false);
                        }
                    } else {
                        int selTeamB = (int) (availTeamsB.size() * Math.random());
                        b = availTeamsB.get(selTeamB);
                    }

                    Game gm = new Game(a, b, "OOC");

                    if (a.getGameSchedule().size() != b.getGameSchedule().size()) {
                        PlatformLog.d("league", "setupSeason: week " + week + " " + a.getName()
                                + " size" + a.getGameSchedule().size() + " vs " + b.getName()
                                + " size" + b.getGameSchedule().size());
                    }

                    // Append OOC games (don't insert by week — insertion fails when
                    // the schedule is too small for the week index, silently dropping
                    // games for teams with fewer conf games).
                    Game oocGame = gm;
                    if (!a.getConference().contains("Independent")
                            && !a.getConference().contains("FCS")) {
                        a.addGameToSchedule(oocGame);
                    }
                    if (!b.getConference().contains("Independent")
                            && !b.getConference().contains("FCS")) {
                        b.addGameToSchedule(oocGame);
                    }

                    if (a.getConference().contains("Independent")) {
                        a.addGameToSchedule(oocGame);
                    }
                    if (b.getConference().contains("Independent")) {
                        b.addGameToSchedule(oocGame);
                    }

                    a.addOocTeam(b);
                    b.addOocTeam(a);

                    availTeams.remove(a);
                    availTeams.remove(b);
                }
            }

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
}
