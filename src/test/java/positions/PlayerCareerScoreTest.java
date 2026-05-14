package positions;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.assertEquals;

public class PlayerCareerScoreTest {

    private DesktopResourceProvider resources;
    private League league;
    private Team team;

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
    }

    @Test
    public void playerRB_getCareerScore_usesOnlyCareerStats() {
        PlayerRB rb = new PlayerRB("Test RB", 1, 3, team);
        rb.stats[17] = 2;
        rb.stats[18] = 1;
        rb.stats[16] = 150;
        rb.stats[20] = 5;
        rb.stats[21] = 60;
        rb.stats[22] = 1;

        rb.careerStats.get(0)[17] = 8;
        rb.careerStats.get(0)[18] = 3;
        rb.careerStats.get(0)[16] = 600;
        rb.careerStats.get(0)[20] = 20;
        rb.careerStats.get(0)[21] = 250;
        rb.careerStats.get(0)[22] = 4;
        rb.careerStats.get(0)[4] = 100;
        rb.careerStats.get(0)[5] = 0;
        rb.careerStats.get(0)[7] = 50;
        rb.careerStats.get(0)[8] = 0;

        int score = rb.getCareerScore();

        int expected = rb.getCareerRushTDs() * 150 - rb.getCareerFumbles() * 75
                + (int) (rb.getCareerRushYards() * 2.65) + 2 * rb.getCareerReceptions()
                + (int) (rb.getCareerRecYards() * 2.5) + rb.getCareerRecTDs() * 150
                + rb.getCareerKOYards() + rb.getCareerKOTDs() * 150
                + rb.getCareerPuntYards() + rb.getCareerPuntTDs() * 150
                + rb.ratOvr * 10 * rb.year;

        assertEquals(expected, score);
    }

    @Test
    public void playerWR_getCareerScore_usesOnlyCareerStats() {
        PlayerWR wr = new PlayerWR("Test WR", 1, 3, team);
        wr.stats[22] = 2;
        wr.stats[18] = 1;
        wr.stats[20] = 8;
        wr.stats[23] = 2;
        wr.stats[21] = 120;
        wr.stats[4] = 50;
        wr.stats[5] = 0;
        wr.stats[7] = 30;
        wr.stats[8] = 0;

        wr.careerStats.get(0)[22] = 6;
        wr.careerStats.get(0)[18] = 4;
        wr.careerStats.get(0)[20] = 25;
        wr.careerStats.get(0)[23] = 5;
        wr.careerStats.get(0)[21] = 400;
        wr.careerStats.get(0)[4] = 200;
        wr.careerStats.get(0)[5] = 1;
        wr.careerStats.get(0)[7] = 100;
        wr.careerStats.get(0)[8] = 0;

        int score = wr.getCareerScore();

        int expected = wr.getCareerRecTDs() * 150 - wr.getCareerFumbles() * 75
                + wr.getCareerReceptions() * 2 - wr.getCareerDrops() * 25
                + (int) (wr.getCareerRecYards() * 2.65)
                + wr.getCareerKOYards() + wr.getCareerKOTDs() * 150
                + wr.getCareerPuntYards() + wr.getCareerPuntTDs() * 150
                + wr.ratOvr * 10 * wr.year;

        assertEquals(expected, score);
    }

    @Test
    public void playerTE_getCareerScore_usesOnlyCareerStats() {
        PlayerTE te = new PlayerTE("Test TE", 1, 3, team);
        te.stats[22] = 1;
        te.stats[18] = 0;
        te.stats[20] = 4;
        te.stats[23] = 1;
        te.stats[21] = 50;

        te.careerStats.get(0)[22] = 5;
        te.careerStats.get(0)[18] = 2;
        te.careerStats.get(0)[20] = 18;
        te.careerStats.get(0)[23] = 4;
        te.careerStats.get(0)[21] = 250;

        int score = te.getCareerScore();

        int expected = te.getCareerRecTDs() * 220 - te.getCareerFumbles() * 75
                + te.getCareerReceptions() * 2 - te.getCareerDrops() * 25
                + te.getCareerRecYards() * 3
                + te.ratOvr * 10 * te.year;

        assertEquals(expected, score);
    }

    @Test
    public void playerOL_getCareerScore_usesOnlyCareerStats() {
        PlayerOL ol = new PlayerOL("Test OL", 1, 3, team);

        int score = ol.getCareerScore();

        int expected = ol.ratOvr * ol.year * 50;
        assertEquals(expected, score);
    }

    @Test
    public void playerCB_getCareerScore_usesOnlyCareerStats() {
        PlayerCB cb = new PlayerCB("Test CB", 1, 3, team);
        cb.stats[15] = 5;
        cb.stats[9] = 20;
        cb.stats[10] = 1;
        cb.stats[11] = 1;
        cb.stats[12] = 2;

        cb.careerStats.get(0)[15] = 15;
        cb.careerStats.get(0)[9] = 60;
        cb.careerStats.get(0)[10] = 3;
        cb.careerStats.get(0)[11] = 2;
        cb.careerStats.get(0)[12] = 5;
        cb.careerStats.get(0)[4] = 150;
        cb.careerStats.get(0)[7] = 80;

        int score = cb.getCareerScore();

        int expected = cb.getCareerTackles() * 25 + cb.getCareerSacks() * 425
                + cb.getCareerFumblesRec() * 425 + cb.getCareerInterceptions() * 425
                + cb.getCareerDefended() * 100
                + cb.getCareerKOYards() + cb.getCareerKOTDs() * 150
                + cb.getCareerPuntYards() + cb.getCareerPuntTDs() * 150
                + cb.ratOvr * 10 * cb.year;

        assertEquals(expected, score);
    }

    @Test
    public void playerDL_getCareerScore_usesOnlyCareerStats() {
        PlayerDL dl = new PlayerDL("Test DL", 1, 3, team);
        dl.stats[9] = 10;
        dl.stats[10] = 2;
        dl.stats[11] = 0;
        dl.stats[12] = 0;

        dl.careerStats.get(0)[9] = 30;
        dl.careerStats.get(0)[10] = 5;
        dl.careerStats.get(0)[11] = 1;
        dl.careerStats.get(0)[12] = 1;

        int score = dl.getCareerScore();

        int expected = dl.getCareerTackles() * 35 + dl.getCareerSacks() * 425
                + dl.getCareerFumblesRec() * 425 + dl.getCareerInterceptions() * 425
                + dl.ratOvr * dl.year * 10;

        assertEquals(expected, score);
    }

    @Test
    public void playerLB_getCareerScore_usesOnlyCareerStats() {
        PlayerLB lb = new PlayerLB("Test LB", 1, 3, team);
        lb.stats[9] = 15;
        lb.stats[10] = 1;
        lb.stats[11] = 0;
        lb.stats[12] = 1;

        lb.careerStats.get(0)[9] = 40;
        lb.careerStats.get(0)[10] = 4;
        lb.careerStats.get(0)[11] = 2;
        lb.careerStats.get(0)[12] = 3;

        int score = lb.getCareerScore();

        int expected = lb.getCareerTackles() * 25 + lb.getCareerSacks() * 425
                + lb.getCareerFumblesRec() * 425 + lb.getCareerInterceptions() * 425
                + lb.ratOvr * lb.year * 10;

        assertEquals(expected, score);
    }

    @Test
    public void playerS_getCareerScore_usesOnlyCareerStats() {
        PlayerS s = new PlayerS("Test S", 1, 3, team);
        s.stats[9] = 12;
        s.stats[10] = 0;
        s.stats[11] = 1;
        s.stats[12] = 1;

        s.careerStats.get(0)[9] = 35;
        s.careerStats.get(0)[10] = 2;
        s.careerStats.get(0)[11] = 2;
        s.careerStats.get(0)[12] = 4;

        int score = s.getCareerScore();

        int expected = s.getCareerTackles() * 35 + s.getCareerSacks() * 425
                + s.getCareerFumblesRec() * 425 + s.getCareerInterceptions() * 425
                + s.ratOvr * s.year * 10;

        assertEquals(expected, score);
    }
}
