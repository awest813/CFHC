package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerDLTest {
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
        PlayerDL dl = new PlayerDL("Test DL", 1, 3, team);
        dl.ratAttr1 = 80;
        dl.ratAttr2 = 70;
        dl.ratAttr3 = 60;
        dl.ratAttr4 = 50;
        int ovr = dl.getOverall();
        assertEquals(65, ovr);
        assertTrue("DL overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getHeismanScore_includesSacks() {
        PlayerDL dl = new PlayerDL("Test DL", 1, 3, team);
        dl.stats[9] = 40;
        dl.stats[10] = 12;
        dl.stats[11] = 3;
        dl.ratOvr = 85;
        int score = dl.getHeismanScore();
        assertTrue("Heisman score should be positive for good stats", score > 0);
    }

    @Test
    public void getHeismanScore_handlesZeroStats() {
        PlayerDL dl = new PlayerDL("Test DL", 1, 3, team);
        dl.ratOvr = 1;
        int score = dl.getHeismanScore();
        assertTrue("Heisman score should be at least ratOvr*10", score >= 10);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerDL dl = new PlayerDL("Test DL", 1, 3, team);
        String ratings = dl.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Tackle"));
        assertTrue(ratings.contains("Strength"));
        assertTrue(ratings.contains("Run Stop"));
        assertTrue(ratings.contains("Pass Rush"));
    }

    @Test
    public void getRatMethods_returnCorrectAttributes() {
        PlayerDL dl = new PlayerDL("Test DL", 1, 3, team);
        dl.ratAttr1 = 80;
        dl.ratAttr2 = 70;
        dl.ratAttr3 = 90;
        dl.ratAttr4 = 60;
        assertEquals(80, dl.getRatRunStop());
        assertEquals(70, dl.getRatTackle());
        assertEquals(90, dl.getRatPassRush());
        assertEquals(60, dl.getRatStrength());
    }
}
