package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerRBTest {
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
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        rb.ratAttr1 = 80;
        rb.ratAttr2 = 70;
        rb.ratAttr3 = 60;
        rb.ratAttr4 = 50;
        int ovr = rb.getOverall();
        assertEquals(68, ovr);
        assertTrue("RB overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void get3DRBOverall_returnsCorrectValue() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        rb.ratAttr1 = 80;
        rb.ratAttr2 = 70;
        rb.ratAttr4 = 60;
        assertEquals(70, rb.get3DRBOverall());
    }

    @Test
    public void getHeismanScore_includesRushStats() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        rb.stats[16] = 1000;
        rb.stats[17] = 10;
        rb.stats[18] = 2;
        rb.ratOvr = 80;
        int score = rb.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_doesNotDoubleCount() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        rb.stats[16] = 0;
        rb.stats[17] = 0;
        rb.stats[20] = 0;
        rb.stats[21] = 0;
        rb.stats[22] = 0;
        rb.ratOvr = 1;
        int score = rb.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        String ratings = rb.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Speed"));
        assertTrue(ratings.contains("Evasion"));
        assertTrue(ratings.contains("Power"));
        assertTrue(ratings.contains("Catch"));
    }

    @Test
    public void getInfoForLineup_returnsFormatted() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        rb.ratOvr = 85;
        rb.ratAttr1 = 80;
        rb.ratAttr2 = 75;
        rb.ratAttr3 = 70;
        rb.ratAttr4 = 65;
        String info = rb.getInfoForLineup();
        assertNotNull(info);
        assertTrue(info.contains("T. RB"));
        assertTrue(info.contains("85"));
    }
}
