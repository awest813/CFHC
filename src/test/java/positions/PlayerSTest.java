package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerSTest {
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
        PlayerS s = new PlayerS("Test S", 1, 3, team);
        s.ratAttr1 = 80;
        s.ratAttr2 = 70;
        s.ratAttr3 = 60;
        s.ratAttr4 = 50;
        int ovr = s.getOverall();
        assertEquals(70, ovr);
        assertTrue("S overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getHeismanScore_includesTackles() {
        PlayerS s = new PlayerS("Test S", 1, 3, team);
        s.stats[9] = 80;
        s.stats[10] = 4;
        s.stats[11] = 2;
        s.stats[12] = 5;
        s.ratOvr = 85;
        int score = s.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_handlesZeroStats() {
        PlayerS s = new PlayerS("Test S", 1, 3, team);
        s.ratOvr = 1;
        int score = s.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerS s = new PlayerS("Test S", 1, 3, team);
        String ratings = s.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Tackle"));
        assertTrue(ratings.contains("Coverage"));
        assertTrue(ratings.contains("Run Stop"));
        assertTrue(ratings.contains("Speed"));
    }

    @Test
    public void getRatMethods_returnCorrectAttributes() {
        PlayerS s = new PlayerS("Test S", 1, 3, team);
        s.ratAttr1 = 85;
        s.ratAttr2 = 80;
        s.ratAttr3 = 75;
        s.ratAttr4 = 70;
        assertEquals(85, s.getRatTackle());
        assertEquals(80, s.getRatCoverage());
        assertEquals(75, s.getRatSpeed());
        assertEquals(70, s.getRatRunStop());
    }
}
