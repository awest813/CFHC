package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Runs 30 full career seasons headless to catch regression bugs in
 * realignment, coaching carousel, recruiting, and gameplay.
 */
public class Career30YearStressTest {

    private League league;
    private SeasonController controller;
    private int advanceErrors;

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        DesktopResourceProvider resources = new DesktopResourceProvider(projectRoot);

        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);

        assertFalse(league.getTeamList().isEmpty());
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.userControlled = true;

        advanceErrors = 0;

        GameUiBridge bridge = new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(File f, Team t) {}
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
                league.recruitPlayers();
                // Match real game flow: reset currentWeek to 0 so next
                // advanceWeek() starts the preseason of the new season.
                league.currentWeek = 0;
            }
        };

        controller = new SeasonController(league, bridge);
    }

    @Test
    public void thirtyYearCareer_completesWithoutErrors() {
        int startYear = league.getYear();
        int startHistorySize = league.getLeagueHistory().size();
        int targetSeasons = 30;

        // Track completed seasons by league year changes
        int lastYear = league.getYear();
        int seasonsCompleted = 0;

        int maxAdvanceCalls = 10000;
        int calls = 0;

        while (seasonsCompleted < targetSeasons && calls < maxAdvanceCalls) {
            try {
                controller.advanceWeek();
            } catch (Exception e) {
                advanceErrors++;
                fail("Crash at week " + league.currentWeek
                        + " (" + league.getYear() + "): " + e.getMessage()
                        + "\n" + getStackTrace(e));
            }
            int currentYear = league.getYear();
            if (currentYear > lastYear) {
                seasonsCompleted++;
                lastYear = currentYear;
            }
            calls++;
        }

        assertEquals("No advance errors should occur", 0, advanceErrors);
        assertEquals("Should complete " + targetSeasons + " seasons", targetSeasons, seasonsCompleted);
        assertTrue("Team count should be reasonable (>=80 after realignment)", league.getTeamList().size() >= 80);

        for (Team t : league.getTeamList()) {
            assertTrue("Team " + t.name + " should not have negative wins", t.wins >= 0);
            assertTrue("Team " + t.name + " should not have negative losses", t.losses >= 0);
        }

        assertTrue("Year should advance by " + targetSeasons + " (" + league.getYear() + " >= " + (startYear + targetSeasons) + ")",
                league.getYear() >= startYear + targetSeasons);
        assertTrue("League history should have gained entries (" + league.getLeagueHistory().size() + " > " + startHistorySize + ")",
                league.getLeagueHistory().size() > startHistorySize);
        assertNotNull("User team should exist after " + targetSeasons + " seasons", league.userTeam);
    }

    private static String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append("  at ").append(el.toString()).append("\n");
        }
        if (e.getCause() != null) {
            sb.append("Caused by: ").append(e.getCause().getMessage()).append("\n");
            for (StackTraceElement el : e.getCause().getStackTrace()) {
                sb.append("  at ").append(el.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}
