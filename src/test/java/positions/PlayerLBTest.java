package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerLBTest {
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
        PlayerLB lb = new PlayerLB("Test LB", 1, 3, team);
        lb.ratAttr1 = 80;
        lb.ratAttr2 = 70;
        lb.ratAttr3 = 60;
        lb.ratAttr4 = 50;
        int ovr = lb.getOverall();
        assertEquals(70, ovr);
        assertTrue("LB overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getHeismanScore_includesTacklesAndSacks() {
        PlayerLB lb = new PlayerLB("Test LB", 1, 3, team);
        lb.stats[9] = 100;
        lb.stats[10] = 8;
        lb.stats[11] = 2;
        lb.stats[12] = 3;
        lb.ratOvr = 85;
        int score = lb.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_handlesZeroStats() {
        PlayerLB lb = new PlayerLB("Test LB", 1, 3, team);
        lb.ratOvr = 1;
        int score = lb.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerLB lb = new PlayerLB("Test LB", 1, 3, team);
        String ratings = lb.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Tackle"));
        assertTrue(ratings.contains("Coverage"));
        assertTrue(ratings.contains("Run Stop"));
        assertTrue(ratings.contains("Speed"));
    }

    @Test
    public void getRatMethods_returnCorrectAttributes() {
        PlayerLB lb = new PlayerLB("Test LB", 1, 3, team);
        lb.ratAttr1 = 90;
        lb.ratAttr2 = 70;
        lb.ratAttr3 = 80;
        lb.ratAttr4 = 75;
        assertEquals(90, lb.getRatTackle());
        assertEquals(70, lb.getRatRunStop());
        assertEquals(80, lb.getRatCoverage());
        assertEquals(75, lb.getRatSpeed());
    }
}
