package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration test that boots a brand-new league and advances through every
 * phase of a single season: preseason → regular season → bowls/playoffs →
 * offseason (up to just before recruiting).
 *
 * <p>The test runs entirely headless using {@link GameUiBridge#NO_OP} and a
 * stub {@link GameFlowManager}, so no Android framework is required.
 */
public class FullSeasonTest {

    private League league;
    private SeasonController controller;

    /** Counts how many times each bridge/flow callback fires. */
    private int recruitingFlowCount;

    @Before
    public void setUp() {
        // Use the same resource-loading path as the desktop prototype.
        String projectRoot = System.getProperty("user.dir");
        DesktopResourceProvider resources = new DesktopResourceProvider(projectRoot);

        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,  // randomize
                false   // equalize
        );
        league.setPlatformResourceProvider(resources);

        // Assign the first team as the user-controlled team.
        assertFalse("League must have at least one team", league.teamList.isEmpty());
        league.userTeam = league.teamList.get(0);
        league.userTeam.userControlled = true;

        recruitingFlowCount = 0;

        // Build a recording NO_OP bridge so we can count terminal callbacks.
        GameUiBridge bridge = new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File f, Team t) {}
            @Override public void transferPlayer(positions.Player p) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player p, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {
                recruitingFlowCount++;
            }
        };

        GameFlowManager flowManager = new GameFlowManager() {
            @Override public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode p, String u) {}
            @Override public void loadGame(String s) {}
            @Override public void importSave(String u) {}
            @Override public void finishRecruiting(String r) {}
            @Override public void startRecruiting(String u) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void returnToMainHub() {}
        };

        controller = new SeasonController(league, bridge, flowManager);
    }

    @Test
    public void fullSeason_completesWithoutErrors() {
        int initialTeamCount = league.teamList.size();
        int initialYear = league.getYear();

        // --- Phase 1: Preseason (week 0 → walk-ons + preseason news) ---
        assertEquals("Season should start at week 0", 0, league.currentWeek);
        controller.advanceWeek();  // handles preseason transition

        // --- Phase 2: Regular season + conference championships ---
        int regWeeks = league.regSeasonWeeks;
        // Play through the regular season weeks (1 through regWeeks-1)
        // and then conference championships (regWeeks-1)
        // and then postseason weeks (regWeeks through regWeeks+3)
        // Total playWeek calls = regWeeks + 4 (including bowl/playoff weeks)
        for (int w = 0; w < regWeeks + 4; w++) {
            controller.advanceWeek();
        }

        // After all games, currentWeek should be regWeeks + 4
        assertEquals("After all games currentWeek should be regWeeks+4",
                regWeeks + 4, league.currentWeek);

        // --- Phase 3: Offseason ---
        // Offseason has stages: season summary, contracts, job offers,
        // coaching carousel, hire assistants, graduation, transfer logic,
        // transfer list, realignment, then recruiting trigger.
        // That's 10 advanceWeek calls (regWeeks+4 through regWeeks+13).
        for (int w = 0; w < 10; w++) {
            controller.advanceWeek();
        }

        // At this point the recruiting flow should have been triggered.
        assertEquals("Recruiting flow should have been triggered exactly once",
                1, recruitingFlowCount);

        // --- Basic invariant checks ---
        // Team count should be unchanged (no teams lost during season).
        assertEquals("Team count should remain the same",
                initialTeamCount, league.teamList.size());

        // Every team should have played their regular-season schedule.
        // Not all teams reach the conference championship or bowls, so the
        // minimum is regWeeks - 1 (12 games in a 13-week season).
        for (Team t : league.teamList) {
            int totalGames = t.wins + t.losses;
            assertTrue("Team " + t.name + " should have played at least " + (regWeeks - 1)
                            + " games but played " + totalGames,
                    totalGames >= regWeeks - 1);
        }

        // Year should have advanced by 1 after updateLeagueHistory added an entry.
        assertEquals("Year should advance by 1 after season completes",
                initialYear + 1, league.getYear());

        // League history should have gained one entry from updateLeagueHistory().
        assertTrue("League history should have at least one entry",
                league.leagueHistory.size() >= 1);

        // The national championship game was played — every season has a champion.
        // Check that at least one team has a national championship win.
        boolean hasChampion = false;
        for (Team t : league.teamList) {
            if (t.totalNCs > 0) {
                hasChampion = true;
                break;
            }
        }
        assertTrue("A national champion should have been crowned", hasChampion);
    }
}
