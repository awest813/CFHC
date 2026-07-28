package desktop;

import org.junit.Before;
import org.junit.Test;
import simulation.FileSystemResourceProvider;
import simulation.Game;
import simulation.GameUiBridge;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;
import simulation.Team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Desktop gameplay polish: week-result indexing and recent-outcome helpers.
 */
public class DesktopWeekResultTest {

    private League league;
    private Team user;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
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
        user = league.getTeamList().get(0);
        user.setUserControlled(true);
        league.userTeam = user;
    }

    @Test
    public void findPlayedGame_matchesEngineIndex_afterWeekOne() {
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        // Preseason -> week 1
        controller.advanceWeek();
        assertEquals(1, league.currentWeek);

        int weekBefore = league.currentWeek;
        controller.advanceWeek();

        Game expected = user.getGameSchedule().get(weekBefore - 1);
        assertTrue("Week 1 game should be played", expected.hasPlayed);

        Game found = DesktopWeekResult.findPlayedGame(user, weekBefore, league.regSeasonWeeks);
        assertNotNull(found);
        assertEquals(expected, found);

        // Old buggy index (weekBefore) would point at the next unplayed game
        Game wrongIndex = user.getGameSchedule().get(weekBefore);
        assertTrue("Next week's game should still be unplayed", !wrongIndex.hasPlayed);
    }

    @Test
    public void findMostRecentPlayed_skipsFutureUnplayedGames() {
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        controller.advanceWeek(); // to week 1
        controller.advanceWeek(); // play week 1

        Game recent = DesktopWeekResult.findMostRecentPlayed(user);
        assertNotNull(recent);
        assertTrue(recent.hasPlayed);

        Game lastSlot = user.getGameSchedule().get(user.getGameSchedule().size() - 1);
        if (lastSlot != recent) {
            assertTrue("Future slate games remain unplayed", !lastSlot.hasPlayed);
        }
    }

    @Test
    public void opponentHelpers_nullSafeWhenSideMissing() {
        Game g = new Game(user, null, "OOC");
        g.hasPlayed = true;
        g.homeScore = 21;
        g.awayScore = 14;
        assertEquals("Opponent", DesktopWeekResult.opponentAbbr(g, user));
        assertEquals("Opponent", DesktopWeekResult.opponentName(g, user));
        assertEquals(21, DesktopWeekResult.userScore(g, user));
        assertEquals(14, DesktopWeekResult.opponentScore(g, user));
    }
}
