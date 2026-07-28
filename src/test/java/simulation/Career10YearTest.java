package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Runs 10 full career seasons headless to catch any regression bugs
 * in the extraction of Team managers and MainActivity controllers.
 */
public class Career10YearTest {

    private League league;
    private SeasonController controller;
    private int recruitingFlowCount;
    private int seasonsCompleted;

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        FileSystemResourceProvider resources = new FileSystemResourceProvider(projectRoot);

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

        recruitingFlowCount = 0;
        seasonsCompleted = 0;

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
                recruitingFlowCount++;
                league.recruitPlayers();
                if (league.userTeam != null) {
                    league.finishRecruitingSeason("");
                }
                seasonsCompleted++;
            }
        };

        controller = new SeasonController(league, bridge);
    }

    @Test
    public void tenYearCareer_completesWithoutErrors() {
        int startYear = league.getYear();

        while (seasonsCompleted < 10) {
            try {
                controller.advanceWeek();
            } catch (Exception e) {
                fail("Season " + (seasonsCompleted + 1) + " crashed at week " + league.currentWeek
                        + " (" + league.getYear() + "): " + e.getMessage());
            }
        }

        // Verify league is still healthy after 10 seasons
        assertTrue("League should have teams after 10 seasons", league.getTeamList().size() > 0);
        assertTrue("Recruiting should have been triggered multiple times", recruitingFlowCount >= 10);
        assertTrue("League history should have entries", league.getLeagueHistory().size() >= 1);
        assertTrue("Year should advance across ten seasons",
                league.getYear() >= startYear + 10);

        // Check no team has negative stats (indicates corruption)
        for (Team t : league.getTeamList()) {
            assertTrue("Team " + t.name + " should not have negative wins", t.wins >= 0);
            assertTrue("Team " + t.name + " should not have negative losses", t.losses >= 0);
        }

        assertNotNull("League should not be null", league);
        assertNotNull("User team should exist", league.userTeam);
    }
}
