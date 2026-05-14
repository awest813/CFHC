package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlayerKTest {

    private DesktopResourceProvider resources;
    private League league;
    private Team team;
    private PlayerK kicker;

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
        kicker = new PlayerK("Test Kicker", 1, 3, team);
    }

    @Test
    public void getCareerScore_usesOnlyCareerStats() {
        kicker.stats[6] = 10;
        kicker.stats[5] = 12;
        kicker.stats[4] = 20;
        kicker.stats[3] = 22;

        kicker.careerStats.get(0)[6] = 30;
        kicker.careerStats.get(0)[5] = 35;
        kicker.careerStats.get(0)[4] = 50;
        kicker.careerStats.get(0)[3] = 55;

        int score = kicker.getCareerScore();

        int careerFGMade = kicker.getCareerFGMade();
        int careerFGAtt = kicker.getCareerFGAtt();
        int careerXPMade = kicker.getCareerXPMade();
        float careerFGpct = kicker.getCareerFGpct();

        int expected = (careerFGMade * 25 + careerXPMade * 5) * (int) careerFGpct / 100
                + kicker.ratOvr * 10 * kicker.year;

        assertEquals(expected, score);
    }

    @Test
    public void getHeismanScore_fgPercentIsNotTruncated() {
        kicker.stats[6] = 2;
        kicker.stats[5] = 2;
        kicker.stats[4] = 3;
        kicker.stats[3] = 3;

        float fgPct = kicker.getFGpct();
        assertTrue("FG% should be 100.0", fgPct > 50);

        int heisman = kicker.getHeismanScore();
        assertTrue("Heisman score should include FG term, not just ratOvr*10",
                heisman > kicker.ratOvr * 10);
    }
}
