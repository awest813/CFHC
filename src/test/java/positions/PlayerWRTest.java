package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerWRTest {
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
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        wr.ratAttr1 = 80;
        wr.ratAttr2 = 70;
        wr.ratAttr3 = 60;
        wr.ratAttr4 = 50;
        int ovr = wr.getOverall();
        assertEquals(67, ovr);
        assertTrue("WR overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getSlotOverall_averagesCatchAndEvasion() {
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        wr.ratAttr2 = 80;
        wr.ratAttr3 = 70;
        assertEquals(75, wr.getSlotOverall());
    }

    @Test
    public void getHeismanScore_includesRecStats() {
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        wr.stats[21] = 1200;
        wr.stats[22] = 12;
        wr.stats[20] = 80;
        wr.stats[23] = 4;
        wr.ratOvr = 85;
        int score = wr.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_handlesZeroStats() {
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        wr.ratOvr = 1;
        int score = wr.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        String ratings = wr.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Catch"));
        assertTrue(ratings.contains("Speed"));
        assertTrue(ratings.contains("Evasion"));
        assertTrue(ratings.contains("Jump"));
    }
}
