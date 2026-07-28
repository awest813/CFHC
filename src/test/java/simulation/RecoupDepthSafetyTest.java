package simulation;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;

/** Fatigue recovery must tolerate depleted starter depth. */
public class RecoupDepthSafetyTest {

    private League league;
    private Team home;
    private Team away;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
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
    public void recoup_withClearedSkillGroups_doesNotThrow() throws Exception {
        home.teamRBs.clear();
        home.teamWRs.clear();
        home.teamOLs.clear();
        away.teamLBs.clear();
        away.teamSs.clear();
        away.teamDLs.clear();

        Game game = new Game(home, away, "Recoup Safety");
        Method m = Game.class.getDeclaredMethod("recoup", boolean.class, int.class);
        m.setAccessible(true);
        m.invoke(game, true, 1);
        m.invoke(game, true, 2);
        assertNotNull(game);
    }
}
