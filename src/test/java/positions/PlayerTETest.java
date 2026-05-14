package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerTETest {
    private League league;
    private Team team;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString("league_player_names"),
                resources.getString("league_last_names"),
                resources.getString("conferences"),
                resources.getString("teams"),
                resources.getString("bowls"),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void getOverall_usesCorrectWeights() {
        PlayerTE te = new PlayerTE("Test TE", 1, 3, team);
        te.ratAttr1 = 80;
        te.ratAttr2 = 70;
        te.ratAttr3 = 60;
        te.ratAttr4 = 50;
        int ovr = te.getOverall();
        assertEquals(70, ovr);
        assertTrue("TE overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getHeismanScore_includesRecStats() {
        PlayerTE te = new PlayerTE("Test TE", 1, 3, team);
        te.stats[21] = 800;
        te.stats[22] = 8;
        te.stats[20] = 50;
        te.ratOvr = 80;
        int score = te.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_handlesMinimumStats() {
        PlayerTE te = new PlayerTE("Test TE", 1, 3, team);
        te.ratOvr = 1;
        int score = te.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerTE te = new PlayerTE("Test TE", 1, 3, team);
        String ratings = te.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Catch"));
        assertTrue(ratings.contains("Speed"));
        assertTrue(ratings.contains("Evasion"));
        assertTrue(ratings.contains("Blocking"));
    }

    @Test
    public void getRatMethods_returnCorrectAttributes() {
        PlayerTE te = new PlayerTE("Test TE", 1, 3, team);
        te.ratAttr1 = 85;
        te.ratAttr2 = 75;
        te.ratAttr3 = 65;
        te.ratAttr4 = 55;
        assertEquals(85, te.getRatRunBlock());
        assertEquals(75, te.getRatCatch());
        assertEquals(65, te.getRatEvasion());
        assertEquals(55, te.getRatSpeed());
    }
}
