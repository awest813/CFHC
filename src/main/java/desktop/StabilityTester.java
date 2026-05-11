package desktop;

import simulation.League;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;
import simulation.Team;
import java.io.File;

/**
 * Headless stability tester that runs three full consecutive seasons
 * to verify long-term engine stability and roster health.
 */
public class StabilityTester {

    private static final String TAG = "StabilityTester";
    private static final int SEASONS_TO_RUN = 3;
    private static final int MAX_STEPS_PER_SEASON = 200;
    private static final int MIN_HEALTHY_ROSTER_SIZE = 40;

    public static void runTest() {
        try {
            DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));

            League league = new League(
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                    resources.getString(PlatformResourceProvider.KEY_TEAMS),
                    resources.getString(PlatformResourceProvider.KEY_BOWLS),
                    false, false
            );
            league.setPlatformResourceProvider(resources);

            // Assign a user team
            Team userTeam = league.getTeamList().get(0);
            userTeam.setUserControlled(true);
            league.userTeam = userTeam;
            league.careerMode = false; // Stay with the same team for stability test

            int startYear = league.getYear();

            int expectedTeamCount = league.getTeamList().size();

            for (int season = 1; season <= SEASONS_TO_RUN; season++) {
                int seasonYear = league.getYear();
                int historyBefore = league.getLeagueHistory().size();
                int championsBefore = countNationalChampionships(league);

                validateNewSeasonState(league, expectedTeamCount, seasonYear);

                System.out.println("\n------------------------------------------------");
                System.out.println("STABILITY TEST: Starting Season " + seasonYear);
                System.out.println("------------------------------------------------");

                class HeadlessBridge extends DesktopUiBridge {
                    private boolean recruitingComplete = false;

                    HeadlessBridge() {
                        super(null, league);
                    }

                    @Override public void crash() { throw new RuntimeException("BRIDGE CRASH TRIGGERED"); }
                    @Override public void showNotification(String t, String m) { System.out.println("NOTIFY: " + t + " - " + m); }
                    @Override public void refreshCurrentPage() {}
                    @Override public void updateSimStatus(String s, String b, boolean m) {}
                    @Override public void showAwardsSummary(String s) {}
                    @Override public void showMidseasonSummary() {}
                    @Override public void showSeasonSummary() {}
                    @Override public void showContractDialog() {}
                    @Override public void showJobOffersDialog() {}
                    @Override public void showPromotionsDialog() {}
                    @Override public void showRedshirtList() {}
                    @Override public void showTransferList() {}
                    @Override public void showRealignmentSummary() {}
                    @Override public void transferPlayer(positions.Player player) {}
                    @Override public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {}
                    @Override public void startRecruitingFlow() {
                        league.recruitPlayers();
                        if (league.userTeam != null) {
                            league.userTeam.recruitPlayersFromStr("");
                            league.updateTeamTalentRatings();
                        }
                        recruitingComplete = true;
                    }
                }

                HeadlessBridge bridge = new HeadlessBridge();

                SeasonController controller = new SeasonController(league, bridge);

                int steps = 0;
                while (!bridge.recruitingComplete && steps < MAX_STEPS_PER_SEASON) {
                    controller.advanceWeek();
                    steps++;
                }
                if (!bridge.recruitingComplete) {
                    throw new RuntimeException("CRITICAL: Recruiting was not reached within "
                            + MAX_STEPS_PER_SEASON + " steps.");
                }

                validateCompletedSeason(league, expectedTeamCount, seasonYear,
                        historyBefore, championsBefore, steps);

                System.out.println("Season " + seasonYear + " complete in " + steps + " steps.");
                System.out.println("User Team Record: " + userTeam.getWins() + "-" + userTeam.getLosses());

                // Boundary transition
                league.startNextSeason();
                System.out.println("Transitioned to Year " + league.getYear());

                validateNewSeasonState(league, expectedTeamCount, league.getYear());
            }

            System.out.println("\nSUCCESS: Stability test completed " + SEASONS_TO_RUN
                    + " full seasons with schedule, history, champion, roster, and reset checks.");

        } catch (Exception e) {
            System.err.println("\nFAILURE: Stability test failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        runTest();
    }

    private static void validateNewSeasonState(League league, int expectedTeamCount, int expectedYear) {
        require(league.getTeamList().size() == expectedTeamCount,
                "Team count changed at new-season boundary: expected " + expectedTeamCount
                        + " but got " + league.getTeamList().size());
        require(league.getYear() == expectedYear,
                "Unexpected league year at new-season boundary: expected " + expectedYear
                        + " but got " + league.getYear());
        require(league.currentWeek == 0,
                "New season should start at week 0 but currentWeek is " + league.currentWeek);
        require(league.userTeam != null, "User team was lost during season transition.");
        validateRosterHealth(league);
        for (Team team : league.getTeamList()) {
            int scheduleSize = team.getGameSchedule().size();
            require(scheduleSize == league.regSeasonWeeks - 1,
                    "Team " + team.getName() + " should start with "
                            + (league.regSeasonWeeks - 1) + " scheduled games but has " + scheduleSize);
            require(team.getWins() == 0 && team.getLosses() == 0,
                    "Team " + team.getName() + " did not reset W-L record for year "
                            + expectedYear + ": " + team.getWins() + "-" + team.getLosses());
        }
    }

    private static void validateCompletedSeason(League league, int expectedTeamCount, int seasonYear,
                                                int historyBefore, int championsBefore, int steps) {
        require(steps > league.regSeasonWeeks,
                "Season " + seasonYear + " completed suspiciously quickly in " + steps + " steps.");
        require(league.getTeamList().size() == expectedTeamCount,
                "Team count changed during season " + seasonYear);
        require(league.getLeagueHistory().size() > historyBefore,
                "League history did not record season " + seasonYear);
        require(countNationalChampionships(league) > championsBefore,
                "No national champion was crowned for season " + seasonYear);
        require(league.userTeam != null && league.userTeam.isUserControlled(),
                "User-controlled team was lost during season " + seasonYear);
        validateRosterHealth(league);

        int minGames = league.regSeasonWeeks - 2;
        for (Team team : league.getTeamList()) {
            int gamesPlayed = team.getWins() + team.getLosses();
            require(gamesPlayed >= minGames,
                    "Team " + team.getName() + " played only " + gamesPlayed
                            + " games in season " + seasonYear + "; expected at least " + minGames);
        }
    }

    private static void validateRosterHealth(League league) {
        for (Team team : league.getTeamList()) {
            int rosterSize = team.getAllPlayers().size();
            require(rosterSize >= MIN_HEALTHY_ROSTER_SIZE,
                    "Team " + team.getName() + " roster depleted to " + rosterSize);
        }
    }

    private static int countNationalChampionships(League league) {
        int total = 0;
        for (Team team : league.getTeamList()) {
            total += team.totalNCs;
        }
        return total;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException("CRITICAL: " + message);
        }
    }
}
