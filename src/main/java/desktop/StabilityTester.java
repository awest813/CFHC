package desktop;

import simulation.League;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;
import simulation.Team;
import simulation.GameFlowManager;
import simulation.LeagueLaunchCoordinator;

import java.io.File;

/**
 * Headless stability tester that runs three full consecutive seasons
 * to verify long-term engine stability and roster health.
 */
public class StabilityTester {

    private static final String TAG = "StabilityTester";

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

            for (int season = 1; season <= 3; season++) {
                System.out.println("\n------------------------------------------------");
                System.out.println("STABILITY TEST: Starting Season " + (startYear + season - 1));
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

                SeasonController controller = new SeasonController(league, bridge, new GameFlowManager() {
                    @Override public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode p, String u) {}
                    @Override public void loadGame(String s) {}
                    @Override public void importSave(String u) {}
                    @Override public void finishRecruiting(String r) {}
                    @Override public void startRecruiting(String u) {}
                    @Override public void showNotification(String t, String m) {}
                    @Override public void returnToMainHub() {}
                });

                int steps = 0;
                while (!bridge.recruitingComplete && steps < 200) {
                    controller.advanceWeek();
                    steps++;
                }
                if (!bridge.recruitingComplete) {
                    throw new RuntimeException("CRITICAL: Recruiting was not reached within 200 steps.");
                }

                System.out.println("Season " + (startYear + season - 1) + " complete.");
                System.out.println("User Team Record: " + userTeam.getWins() + "-" + userTeam.getLosses());

                // Boundary transition
                league.startNextSeason();
                System.out.println("Transitioned to Year " + league.getYear());

                // Sanity check
                if (userTeam.getAllPlayers().size() < 40) {
                    throw new RuntimeException("CRITICAL: User team roster depleted to " + userTeam.getAllPlayers().size());
                }
            }

            System.out.println("\nSUCCESS: Stability test completed 3 full seasons without failure.");

        } catch (Exception e) {
            System.err.println("\nFAILURE: Stability test failed!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        runTest();
    }
}
