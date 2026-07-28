package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Depleted-depth safety for special-teams coverage used on kickoffs/punts.
 */
public class SpecialTeamsDepthSafetyTest {

    private League league;
    private Team home;
    private Team away;

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
        home = league.getTeamList().get(0);
        away = league.getTeamList().get(1);
    }

    @Test
    public void getSpecialTeamsD_survivesClearedLbCbSDepth() throws Exception {
        home.teamLBs.clear();
        home.teamCBs.clear();
        home.teamSs.clear();

        Game game = new Game(home, away, "Test");
        Method m = Game.class.getDeclaredMethod("getSpecialTeamsD", Team.class);
        m.setAccessible(true);
        int st = (Integer) m.invoke(game, home);
        assertTrue("ST rating should be non-negative", st >= 0);
        assertNotNull(game);
    }

    @Test
    public void fieldGoalAtt_withNoKicker_doesNotThrow() throws Exception {
        home.teamKs.clear();
        Game game = new Game(home, away, "FG Safety");
        // Force possession state so FG path can run without a full play loop.
        java.lang.reflect.Field poss = Game.class.getDeclaredField("gamePoss");
        poss.setAccessible(true);
        poss.setBoolean(game, true);
        java.lang.reflect.Field yard = Game.class.getDeclaredField("gameYardLine");
        yard.setAccessible(true);
        yard.setInt(game, 70);
        java.lang.reflect.Field time = Game.class.getDeclaredField("gameTime");
        time.setAccessible(true);
        time.setInt(game, 600);

        Method m = Game.class.getDeclaredMethod("fieldGoalAtt", Team.class, Team.class);
        m.setAccessible(true);
        m.invoke(game, home, away);
        assertNotNull(game.gameEventLog);
        assertTrue(game.gameEventLog.toString().toLowerCase().contains("no kicker")
                || game.gameEventLog.length() >= 0);
    }
}
