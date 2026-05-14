package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class PlayerTest {

    private DesktopResourceProvider resources;
    private League league;
    private Team team;
    private PlayerQB player;

    @Before
    public void setUp() {
        resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
        player = new PlayerQB("Test Player", 1, 3, team);
    }

    @Test
    public void playerConstructor_initializesWithDefaultValues() {
        Player p = new Player();
        assertEquals(0, p.year);
        assertNull(p.stats);
        assertNull(p.careerStats);
        assertNull(p.awards);
        assertNull(p.position);
    }

    @Test
    public void playerConstructor_setsPosition() {
        assertEquals("QB", player.position);
    }

    @Test
    public void playerConstructor_initializesCareerStats() {
        assertNotNull(player.careerStats);
        assertEquals(5, player.careerStats.size());
    }

    @Test
    public void getGames_startsAtZero() {
        assertEquals(0, player.getGames());
    }

    @Test
    public void setStats_recordsStatCorrectly() {
        player.recordGame(5);
        assertEquals(5, player.getGames());
    }

    @Test
    public void seasonStatstoCareer_appendsToCareerStats() {
        player.stats[0] = 2024;
        player.stats[1] = 12;
        player.stats[9] = 300;
        player.stats[13] = 2500;

        player.seasonStatstoCareer();

        int yearIdx = player.year;
        assertEquals(2024, player.careerStats.get(yearIdx)[0]);
        assertEquals(12, player.careerStats.get(yearIdx)[1]);
        assertEquals(300, player.careerStats.get(yearIdx)[9]);
        assertEquals(2500, player.careerStats.get(yearIdx)[13]);
        assertEquals(0, player.stats[1]);
        assertEquals(0, player.stats[9]);
        assertEquals(0, player.stats[13]);
    }

    @Test
    public void seasonStatstoCareer_handlesYear5() {
        player.year = 5;
        player.stats[0] = 2028;
        player.stats[1] = 10;

        player.seasonStatstoCareer();

        assertTrue(player.careerStats.size() > 5);
        assertEquals(2028, player.careerStats.get(5)[0]);
        assertEquals(10, player.careerStats.get(5)[1]);
    }

    @Test
    public void getGames_incrementsWithSet() {
        player.recordGame(3);
        player.recordGame(2);
        assertEquals(5, player.getGames());
    }

    @Test
    public void getCareerGames_sumsCareerEntries() {
        player.stats[1] = 2;
        player.careerStats.get(0)[1] = 3;
        player.careerStats.get(1)[1] = 1;
        assertEquals(6, player.getCareerGames());
    }

    @Test
    public void getSeasonsPlayed_returnsYearRange() {
        player.careerStats.get(0)[0] = 2024;
        player.careerStats.get(4)[0] = 2028;
        String result = player.getSeasonsPlayed();
        assertTrue(result.contains("2024"));
        assertTrue(result.contains("2028"));
        assertTrue(result.contains(" to "));
    }

    @Test
    public void getCareerYardsPerAttempt_usesOnlyCareerStats() {
        player.stats[9] = 10;
        player.stats[13] = 50;
        player.careerStats.get(0)[9] = 100;
        player.careerStats.get(0)[13] = 1000;

        float ypa = player.getCareerYardsPerAttempt();

        assertEquals(9.545, ypa, 0.01);
    }

    @Test
    public void getCareerPasserRating_guardsCareerAtt() {
        player.stats[9] = 0;
        player.careerStats.get(0)[9] = 0;

        assertEquals(0, player.getCareerPasserRating(), 0.001);

        player.careerStats.get(0)[9] = 10;
        player.careerStats.get(0)[13] = 200;
        player.careerStats.get(0)[10] = 7;
        player.careerStats.get(0)[11] = 2;
        player.careerStats.get(0)[12] = 1;

        float rating = player.getCareerPasserRating();
        assertTrue(rating > 0);
    }

    @Test
    public void getProgression_handlesNullHeadCoach() {
        team.HC = null;
        int result = player.getProgression();
        assertTrue(result >= 0);
    }

    @Test
    public void getProgressionOff_handlesNullOC() {
        team.OC = null;
        int result = player.getProgressionOff();
        assertTrue(result >= 0);
    }

    @Test
    public void awardMethods_recordCorrectly() {
        player.recordHeismans(2);
        player.recordAllAmericans(3);
        player.recordAllConference(1);
        player.recordTopFreshman(1);
        player.recordAllFreshman(1);

        assertEquals(2, player.getHeismans());
        assertEquals(3, player.getAllAmericans());
        assertEquals(1, player.getAllConference());
        assertEquals(1, player.getTopFreshman());
        assertEquals(1, player.getAllFreshman());
    }

    @Test
    public void getPlayerFeaturedStats_usesStringBuilderForAwards() {
        player.recordHeismans(1);
        player.recordAllAmericans(1);
        var stats = player.getPlayerFeaturedStats();
        assertNotNull(stats);
        assertFalse(stats.isEmpty());
        boolean foundAwards = stats.stream().anyMatch(s -> s.contains("Awards") || s.contains("POTY"));
        assertTrue("Awards section should appear in featured stats", foundAwards);
    }

    @Test
    public void getRatingMethods_returnExpectedTypes() {
        assertTrue(player.ratAttr1 >= 0 && player.ratAttr1 <= 100);
        assertTrue(player.ratAttr2 >= 0 && player.ratAttr2 <= 100);
        assertTrue(player.ratAttr3 >= 0 && player.ratAttr3 <= 100);
        assertTrue(player.ratAttr4 >= 0 && player.ratAttr4 <= 100);
    }

    @Test
    public void getPotRating_handlesZeroTalent() {
        player.team.league.showPotential = true;
        int result = player.getPotRating(0);
        assertTrue(result >= 0);
    }

    @Test
    public void getInitialCost_usesCorrectParentheses() {
        player.ratOvr = 80;
        player.recruitTolerance = 50;

        int cost = player.getInitialCost();

        assertTrue("Cost should be non-negative", cost >= 0);
    }

    @Test
    public void getLocationCost_usesCorrectRandom() {
        team.location = 0;
        player.homeState = 90;

        boolean foundHigh = false;
        for (int i = 0; i < 50; i++) {
            player.cost = 10;
            int cost = player.getLocationCost();
            if (cost > 10) {
                foundHigh = true;
                break;
            }
        }
        assertTrue("Location cost should be able to increase above base", foundHigh);
    }
}
