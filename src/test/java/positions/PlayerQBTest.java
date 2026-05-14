package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.assertEquals;

public class PlayerQBTest {

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
        player = new PlayerQB("Test QB", 1, 3, team);
    }

    @Test
    public void getCareerScore_usesOnlyCareerStats() {
        player.stats[11] = 5;
        player.stats[12] = 2;
        player.stats[13] = 500;
        player.stats[17] = 3;
        player.stats[16] = 200;

        player.careerStats.get(0)[11] = 10;
        player.careerStats.get(0)[12] = 5;
        player.careerStats.get(0)[13] = 2000;
        player.careerStats.get(0)[17] = 5;
        player.careerStats.get(0)[16] = 800;

        int score = player.getCareerScore();

        int careerPassTD = player.getCareerPassTD();
        int careerPassInt = player.getCareerPassInt();
        int careerPassYards = player.getCareerPassYards();
        int careerRushTDs = player.getCareerRushTDs();
        int careerRushYards = player.getCareerRushYards();

        int expected = careerPassTD * 150 - careerPassInt * 200 + careerPassYards
                + careerRushTDs * 150 + 3 * careerRushYards + player.ratOvr * 10 * player.year;

        assertEquals(expected, score);
    }

    @Test
    public void getCareerScore_matchesHandCalculation() {
        player.stats[11] = 5;
        player.stats[12] = 2;
        player.stats[13] = 500;
        player.stats[17] = 3;
        player.stats[16] = 200;

        player.careerStats.get(0)[11] = 10;
        player.careerStats.get(0)[12] = 5;
        player.careerStats.get(0)[13] = 2000;
        player.careerStats.get(0)[17] = 5;
        player.careerStats.get(0)[16] = 800;

        int score = player.getCareerScore();
        int expected = 15 * 150 - 7 * 200 + 2500 + 8 * 150 + 3 * 1000 + player.ratOvr * 10 * player.year;

        assertEquals(expected, score);
    }
}
