package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.PlayerReturner;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * Regression for kickoff returner selection when WR/RB/CB depth is depleted
 * (Career30YearStressTest crash: null returner.ratSpeed).
 */
public class KickReturnerSafetyTest {

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
    public void getReturner_withEmptySkillPositions_stillAssignsReturner() throws Exception {
        home.teamWRs.clear();
        home.teamRBs.clear();
        home.teamCBs.clear();
        // Leave other positions so getAllPlayers() fallback works.
        assertFalse(home.getAllPlayers().isEmpty());

        Game g = new Game(home, away, "Returner Safety");
        Method m = Game.class.getDeclaredMethod("getReturner", Team.class);
        m.setAccessible(true);
        m.invoke(g, home);

        PlayerReturner homeRet = (PlayerReturner) Game.class.getDeclaredField("homeKickReturner").get(g);
        assertNotNull("Depleted WR/RB/CB roster must still produce a kick returner", homeRet);
        assertNotNull(homeRet.name);
        assertTrue(homeRet.ratSpeed > 0);
    }

    @Test
    public void getReturner_withSingleCandidate_assignsThatReturner() throws Exception {
        while (home.teamWRs.size() > 1) {
            home.teamWRs.remove(home.teamWRs.size() - 1);
        }
        home.teamRBs.clear();
        home.teamCBs.clear();
        assertEquals(1, home.teamWRs.size());

        Game g = new Game(home, away, "Single Returner");
        Method m = Game.class.getDeclaredMethod("getReturner", Team.class);
        m.setAccessible(true);
        m.invoke(g, home);

        PlayerReturner homeRet = (PlayerReturner) Game.class.getDeclaredField("homeKickReturner").get(g);
        assertNotNull(homeRet);
        assertEquals(home.teamWRs.get(0).name, homeRet.name);
    }
}
