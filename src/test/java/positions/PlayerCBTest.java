package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerCBTest {
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
        PlayerCB cb = new PlayerCB("Test CB", 1, 3, team);
        cb.ratAttr1 = 80;
        cb.ratAttr2 = 70;
        cb.ratAttr3 = 60;
        cb.ratAttr4 = 50;
        int ovr = cb.getOverall();
        assertEquals(67, ovr);
        assertTrue("CB overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getHeismanScore_includesInts() {
        PlayerCB cb = new PlayerCB("Test CB", 1, 3, team);
        cb.stats[9] = 50;
        cb.stats[10] = 2;
        cb.stats[12] = 6;
        cb.stats[15] = 15;
        cb.ratOvr = 85;
        int score = cb.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_handlesZeroStats() {
        PlayerCB cb = new PlayerCB("Test CB", 1, 3, team);
        cb.ratOvr = 1;
        int score = cb.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerCB cb = new PlayerCB("Test CB", 1, 3, team);
        String ratings = cb.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Tackle"));
        assertTrue(ratings.contains("Coverage"));
        assertTrue(ratings.contains("Jump"));
        assertTrue(ratings.contains("Speed"));
    }

    @Test
    public void getRatMethods_returnCorrectAttributes() {
        PlayerCB cb = new PlayerCB("Test CB", 1, 3, team);
        cb.ratAttr1 = 85;
        cb.ratAttr2 = 90;
        cb.ratAttr3 = 70;
        cb.ratAttr4 = 80;
        assertEquals(85, cb.getRatCoverage());
        assertEquals(90, cb.getRatSpeed());
        assertEquals(70, cb.getRatTackle());
        assertEquals(80, cb.getRatJump());
    }
}
