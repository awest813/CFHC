package desktop;

import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the desktop docked-recruiting gate: NLI week pauses bulk advance,
 * {@link DesktopUiBridge#completeDockedRecruiting(String)} signs the class,
 * and the league can roll into the next season.
 */
public class DesktopDockedRecruitingPlayabilityTest {

    private League league;
    private DesktopUiBridge bridge;
    private SeasonController controller;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        bridge = new DesktopUiBridge(null, league);
        controller = new SeasonController(league, bridge);
    }

    @Test
    public void nliWeek_setsAwaitingDockedRecruitingForUserTeam() {
        league.currentWeek = league.regSeasonWeeks + 13;

        controller.advanceWeek();

        assertTrue(bridge.isAwaitingDockedRecruiting());
        assertFalse(bridge.isNewSeasonPending());
    }

    @Test
    public void completeDockedRecruiting_rollsIntoNextSeason() {
        league.currentWeek = league.regSeasonWeeks + 13;
        controller.advanceWeek();
        assertTrue(bridge.isAwaitingDockedRecruiting());

        bridge.completeDockedRecruiting("");

        assertFalse(bridge.isAwaitingDockedRecruiting());
        assertTrue(bridge.isNewSeasonPending());

        league.startNextSeason();
        assertEquals(0, league.currentWeek);
    }
}
