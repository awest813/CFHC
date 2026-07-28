package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * weekSummaryStr must tolerate empty/short schedules and out-of-range weeks.
 */
public class WeekSummarySafetyTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        DesktopResourceProvider resources =
                new DesktopResourceProvider(System.getProperty("user.dir"));
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
        team = league.getTeamList().get(0);
    }

    @Test
    public void weekSummary_emptySchedule_doesNotThrow() {
        team.gameSchedule.clear();
        String summary = team.weekSummaryStr(1);
        assertTrue(summary.contains(team.name));
        assertTrue(summary.toLowerCase().contains("no games") || summary.contains("poll"));
    }

    @Test
    public void weekSummary_outOfRangeWeek_clampsSafely() {
        assertFalse(team.gameSchedule.isEmpty());
        String summary = team.weekSummaryStr(999);
        assertTrue(summary.contains(team.name));
        assertFalse(summary.trim().isEmpty());
    }

    @Test
    public void weekSummary_byeWeek_usesCanonicalLabel() {
        Team bye = new Team("BYE", "BYE", "BYE", 0, "BYE", 0, league);
        team.gameSchedule.clear();
        team.addGameToSchedule(new Game(team, bye, Game.BYE_WEEK_NAME));
        String summary = team.weekSummaryStr(1);
        assertTrue(summary.contains(Game.BYE_WEEK_NAME));
    }
}
