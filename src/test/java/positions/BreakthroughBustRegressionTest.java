package positions;

import simulation.FileSystemResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import static org.junit.Assert.*;

public class BreakthroughBustRegressionTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
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
    public void breakthrough_canTriggerForHighProgressionPlayer() {
        PlayerQB qb = new PlayerQB("BT QB", 1, 3, team);
        qb.ratAttr1 = 50; qb.ratAttr2 = 50; qb.ratAttr3 = 50; qb.ratAttr4 = 50;
        qb.ratOvr = qb.getOverall();
        qb.ratPot = 99;
        qb.stats[1] = 12;
        qb.stats[2] = 12;

        boolean bigGrowth = false;
        for (int i = 0; i < 200; i++) {
            qb.ratAttr1 = 50; qb.ratAttr2 = 50; qb.ratAttr3 = 50; qb.ratAttr4 = 50;
            qb.ratOvr = qb.getOverall();
            int before = qb.ratAttr1 + qb.ratAttr2 + qb.ratAttr3 + qb.ratAttr4;
            qb.genericAdvanceSeason();
            int after = qb.ratAttr1 + qb.ratAttr2 + qb.ratAttr3 + qb.ratAttr4;
            if (after - before > 15) {
                bigGrowth = true;
                break;
            }
        }
        assertTrue("High-progression player should occasionally get a big breakthrough", bigGrowth);
    }

    @Test
    public void bust_canTriggerForLowCharLowPlaytime() {
        // Bust is only 3% and applies after random growth. Keep facilities / pot low so
        // a bust penalty is visible as a net decline below the starting attributes.
        int savedFacilities = team.getTeamFacilities();
        team.setTeamFacilities(0);
        try {
            PlayerRB rb = new PlayerRB("Bust RB", 1, 2, team);
            rb.ratAttr1 = 50; rb.ratAttr2 = 50; rb.ratAttr3 = 50; rb.ratAttr4 = 50;
            rb.ratOvr = rb.getOverall();
            rb.ratPot = 5;
            rb.character = 15;
            rb.year = 1;
            rb.stats[1] = 2;
            rb.stats[2] = 0;

            boolean sawDecline = false;
            for (int i = 0; i < 8000; i++) {
                rb.ratAttr1 = 50; rb.ratAttr2 = 50; rb.ratAttr3 = 50; rb.ratAttr4 = 50;
                rb.ratPot = 5;
                rb.character = 15;
                rb.ratOvr = rb.getOverall();
                rb.genericAdvanceSeason();
                if (rb.ratAttr1 < 50 || rb.ratAttr2 < 50 || rb.ratAttr3 < 50 || rb.ratAttr4 < 50) {
                    sawDecline = true;
                    break;
                }
            }
            assertTrue("Low-character low-playtime player should occasionally bust (decline)", sawDecline);
        } finally {
            team.setTeamFacilities(savedFacilities);
        }
    }

    @Test
    public void bust_doesNotTriggerForHighCharacter() {
        PlayerWR wr = new PlayerWR("Safe WR", 1, 3, team);
        wr.ratAttr1 = 50; wr.ratAttr2 = 50; wr.ratAttr3 = 50; wr.ratAttr4 = 50;
        wr.ratOvr = wr.getOverall();
        wr.ratPot = 80;
        wr.character = 80;
        wr.stats[1] = 12;
        wr.stats[2] = 10;

        boolean sawDecline = false;
        for (int i = 0; i < 200; i++) {
            wr.ratAttr1 = 50; wr.ratAttr2 = 50; wr.ratAttr3 = 50; wr.ratAttr4 = 50;
            wr.ratOvr = wr.getOverall();
            wr.genericAdvanceSeason();
            if (wr.ratAttr1 < 50 || wr.ratAttr2 < 50 || wr.ratAttr3 < 50 || wr.ratAttr4 < 50) {
                sawDecline = true;
                break;
            }
        }
        assertFalse("High-character high-playtime player should not bust", sawDecline);
    }

    @Test
    public void regression_canAffectYear4Players() {
        PlayerCB cb = new PlayerCB("Reg CB", 4, 4, team);
        cb.ratAttr1 = 70; cb.ratAttr2 = 80; cb.ratAttr3 = 60; cb.ratAttr4 = 70;
        cb.ratOvr = cb.getOverall();
        cb.ratPot = 85;
        cb.year = 4;
        cb.stats[1] = 12;
        cb.stats[2] = 10;

        boolean sawRegression = false;
        // Regression is 10%/5% per season and pre-applied growth often masks
        // the 1-3 point penalty, so the observable rate per trial is well
        // under 1% — 500 trials once produced a false negative in CI. Same
        // high-trial pattern as the 3% bust test above.
        for (int i = 0; i < 8000; i++) {
            int beforeSpeed = cb.ratAttr2;
            cb.ratAttr1 = 70; cb.ratAttr2 = 80; cb.ratAttr3 = 60; cb.ratAttr4 = 70;
            cb.ratOvr = cb.getOverall();
            cb.genericAdvanceSeason();
            if (cb.ratAttr1 < 70 || cb.ratAttr2 < 80 || cb.ratAttr3 < 60 || cb.ratAttr4 < 70) {
                if (cb.ratAttr2 < beforeSpeed || cb.ratAttr1 < 70 || cb.ratAttr3 < 60 || cb.ratAttr4 < 70) {
                    sawRegression = true;
                    break;
                }
            }
        }
        assertTrue("Year 4+ player should occasionally regress physically", sawRegression);
    }

    @Test
    public void regression_doesNotAffectYoungPlayers() {
        PlayerLB lb = new PlayerLB("Young LB", 1, 3, team);
        lb.ratAttr1 = 50; lb.ratAttr2 = 50; lb.ratAttr3 = 50; lb.ratAttr4 = 50;
        lb.ratOvr = lb.getOverall();
        lb.ratPot = 80;
        lb.year = 1;
        lb.stats[1] = 12;
        lb.stats[2] = 10;

        boolean sawRegression = false;
        for (int i = 0; i < 200; i++) {
            lb.ratAttr1 = 50; lb.ratAttr2 = 50; lb.ratAttr3 = 50; lb.ratAttr4 = 50;
            lb.ratOvr = lb.getOverall();
            lb.genericAdvanceSeason();
            if (lb.ratAttr1 < 48 || lb.ratAttr2 < 48 || lb.ratAttr3 < 48 || lb.ratAttr4 < 48) {
                sawRegression = true;
                break;
            }
        }
        assertFalse("Year 1 player should not regress", sawRegression);
    }

    @Test
    public void lateBloomer_canTriggerForHighPotUnderperformer() {
        PlayerS s = new PlayerS("Bloom S", 3, 3, team);
        s.ratAttr1 = 45; s.ratAttr2 = 45; s.ratAttr3 = 45; s.ratAttr4 = 45;
        s.ratOvr = s.getOverall();
        s.ratPot = 90;
        s.year = 3;
        s.stats[1] = 8;
        s.stats[2] = 6;

        boolean sawBigGrowth = false;
        for (int i = 0; i < 300; i++) {
            s.ratAttr1 = 45; s.ratAttr2 = 45; s.ratAttr3 = 45; s.ratAttr4 = 45;
            s.ratOvr = s.getOverall();
            int before = s.ratAttr1 + s.ratAttr2 + s.ratAttr3 + s.ratAttr4;
            s.genericAdvanceSeason();
            int after = s.ratAttr1 + s.ratAttr2 + s.ratAttr3 + s.ratAttr4;
            if (after - before > 12) {
                sawBigGrowth = true;
                break;
            }
        }
        assertTrue("High-pot underperforming year 3+ player should occasionally late-bloom", sawBigGrowth);
    }

    @Test
    public void lateBloomer_doesNotTriggerForLowPot() {
        PlayerDL dl = new PlayerDL("No Bloom DL", 3, 3, team);
        dl.ratAttr1 = 45; dl.ratAttr2 = 45; dl.ratAttr3 = 45; dl.ratAttr4 = 45;
        dl.ratOvr = dl.getOverall();
        dl.ratPot = 50;
        dl.year = 3;
        dl.stats[1] = 8;
        dl.stats[2] = 6;

        PlayerDL dlHigh = new PlayerDL("Bloom DL", 3, 3, team);
        dlHigh.ratAttr1 = 45; dlHigh.ratAttr2 = 45; dlHigh.ratAttr3 = 45; dlHigh.ratAttr4 = 45;
        dlHigh.ratOvr = dlHigh.getOverall();
        dlHigh.ratPot = 95;
        dlHigh.year = 3;
        dlHigh.stats[1] = 8;
        dlHigh.stats[2] = 6;

        int lowPotMaxGrowth = 0;
        int highPotMaxGrowth = 0;
        for (int i = 0; i < 200; i++) {
            dl.ratAttr1 = 45; dl.ratAttr2 = 45; dl.ratAttr3 = 45; dl.ratAttr4 = 45;
            dl.ratOvr = dl.getOverall();
            int before = dl.ratAttr1 + dl.ratAttr2 + dl.ratAttr3 + dl.ratAttr4;
            dl.genericAdvanceSeason();
            int after = dl.ratAttr1 + dl.ratAttr2 + dl.ratAttr3 + dl.ratAttr4;
            if (after - before > lowPotMaxGrowth) lowPotMaxGrowth = after - before;

            dlHigh.ratAttr1 = 45; dlHigh.ratAttr2 = 45; dlHigh.ratAttr3 = 45; dlHigh.ratAttr4 = 45;
            dlHigh.ratOvr = dlHigh.getOverall();
            int beforeH = dlHigh.ratAttr1 + dlHigh.ratAttr2 + dlHigh.ratAttr3 + dlHigh.ratAttr4;
            dlHigh.genericAdvanceSeason();
            int afterH = dlHigh.ratAttr1 + dlHigh.ratAttr2 + dlHigh.ratAttr3 + dlHigh.ratAttr4;
            if (afterH - beforeH > highPotMaxGrowth) highPotMaxGrowth = afterH - beforeH;
        }
        assertTrue("High-pot player should have bigger max growth than low-pot (low=" + lowPotMaxGrowth
                + " high=" + highPotMaxGrowth + ")", highPotMaxGrowth > lowPotMaxGrowth);
    }

    @Test
    public void regression_moreLikelyForYear5() {
        PlayerOL ol = new PlayerOL("Old OL", 5, 4, team);
        ol.ratAttr1 = 90; ol.ratAttr2 = 90; ol.ratAttr3 = 90; ol.ratAttr4 = 90;
        ol.ratOvr = ol.getOverall();
        ol.ratPot = 50;
        ol.year = 5;
        ol.stats[1] = 12;
        ol.stats[2] = 10;

        int regressions = 0;
        for (int i = 0; i < 500; i++) {
            ol.ratAttr1 = 90; ol.ratAttr2 = 90; ol.ratAttr3 = 90; ol.ratAttr4 = 90;
            ol.ratOvr = ol.getOverall();
            ol.genericAdvanceSeason();
            int[] caps = positions.Archetypes.getCaps("OL", ol.archetypeTag);
            int minAttr = Math.min(Math.min(ol.ratAttr1, ol.ratAttr2), Math.min(ol.ratAttr3, ol.ratAttr4));
            if (ol.ratAttr3 < 90 || ol.ratAttr4 < 90 || minAttr < 90) {
                regressions++;
            }
        }
        assertTrue("Year 5 player should regress at least occasionally (got " + (regressions * 100 / 500) + "%)",
                regressions > 25);
    }
}
