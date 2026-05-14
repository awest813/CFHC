package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerOLTest {
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
        PlayerOL ol = new PlayerOL("Test OL", 1, 3, team);
        ol.ratAttr1 = 80;
        ol.ratAttr2 = 70;
        ol.ratAttr3 = 60;
        ol.ratAttr4 = 50;
        int ovr = ol.getOverall();
        assertEquals(68, ovr);
        assertTrue("OL overall should be in 0-99 range", ovr >= 0 && ovr <= 99);
    }

    @Test
    public void getHeismanScore_returnsRatOvrBased() {
        PlayerOL ol = new PlayerOL("Test OL", 1, 3, team);
        ol.stats[1] = 12;
        ol.stats[2] = 12;
        ol.stats[5] = 500;
        ol.stats[6] = 400;
        ol.stats[7] = 3;
        ol.ratOvr = 85;
        int score = ol.getHeismanScore();
        assertTrue("OL Heisman score should be positive", score > 0);
    }

    @Test
    public void getHeismanScore_returnsMinimumForZeroGames() {
        PlayerOL ol = new PlayerOL("Test OL", 1, 3, team);
        ol.ratOvr = 50;
        int score = ol.getHeismanScore();
        assertTrue("OL Heisman score should be >= ratOvr*100", score >= 5000);
    }

    @Test
    public void getPlayerRatings_returnsNonEmpty() {
        PlayerOL ol = new PlayerOL("Test OL", 1, 3, team);
        String ratings = ol.getPlayerRatings();
        assertNotNull(ratings);
        assertFalse(ratings.isEmpty());
        assertTrue(ratings.contains("Run Block"));
        assertTrue(ratings.contains("Pass Block"));
        assertTrue(ratings.contains("Strength"));
        assertTrue(ratings.contains("Vision"));
    }

    @Test
    public void getRatMethods_returnCorrectAttributes() {
        PlayerOL ol = new PlayerOL("Test OL", 1, 3, team);
        ol.ratAttr1 = 90;
        ol.ratAttr2 = 80;
        ol.ratAttr3 = 70;
        ol.ratAttr4 = 60;
        assertEquals(90, ol.getRatRunBlock());
        assertEquals(80, ol.getRatPassBlock());
        assertEquals(70, ol.getRatStrength());
        assertEquals(60, ol.getRatVision());
    }
}
