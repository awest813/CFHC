package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NullUserTeamPlayWeekTest {

    private League league;
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
        league.userTeam = null;
        controller = new SeasonController(league, GameUiBridge.NO_OP);
    }

    @Test
    public void regularSeasonWeeks_doNotCrashWithoutUserTeam() {
        controller.advanceWeek();
        assertEquals(1, league.currentWeek);

        for (int w = 1; w < league.regSeasonWeeks; w++) {
            controller.advanceWeek();
        }
        assertEquals(league.regSeasonWeeks, league.currentWeek);
    }
}
