package simulation;

import org.junit.Test;

import positions.Player;
import staff.HeadCoach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pins the balance calibrations from the multi-season audit so later tweaks
 * notice when they move them.
 */
public class BalanceTuningTest {

    @Test
    public void fieldGoalCurve_matchesCollegeSplitsForAnAverageKicker() {
        // Average (80 accuracy) kicker; FBS: ~95% in the 20s, ~85% in the 30s,
        // ~70% in the 40s, ~50% from 50+.
        assertEquals(0.96, Game.fgMakeChance(25, 80), 0.02);
        assertEquals(0.83, Game.fgMakeChance(35, 80), 0.03);
        assertEquals(0.63, Game.fgMakeChance(44, 80), 0.03);
        assertEquals(0.45, Game.fgMakeChance(50, 80), 0.03);
    }

    @Test
    public void fieldGoalCurve_rewardsAccuracyAndStaysBounded() {
        assertTrue(Game.fgMakeChance(40, 95) > Game.fgMakeChance(40, 80));
        assertTrue(Game.fgMakeChance(40, 80) > Game.fgMakeChance(40, 60));
        assertTrue(Game.fgMakeChance(30, 80) > Game.fgMakeChance(45, 80));
        assertEquals(0.99, Game.fgMakeChance(18, 99), 1e-9);
        assertEquals(0.05, Game.fgMakeChance(70, 40), 1e-9);
    }

    @Test
    public void interceptionCurve_keepsTheLeagueNearFbsRates() {
        // League ~2.2% a throw (FBS ~2.3%); the QB's accuracy and IQ set most of
        // the spread: ~1.3% for the most accurate, ~3.5% for the least.
        int p = Game.LEAGUE_PRESSURE;
        assertEquals(0.0205, Game.interceptionChance(84, p, 78, Game.INT_COVERAGE_PIVOT, 0), 0.001);
        assertEquals(0.0134, Game.interceptionChance(95, p, 78, Game.INT_COVERAGE_PIVOT, 0), 0.001);
        assertEquals(0.0348, Game.interceptionChance(62, p, 78, Game.INT_COVERAGE_PIVOT, 0), 0.001);
        assertTrue("riskier throws are picked more often",
                Game.interceptionChance(84, 95, 78, Game.INT_COVERAGE_PIVOT, 1) > Game.interceptionChance(84, 70, 78, Game.INT_COVERAGE_PIVOT, 0));
        assertEquals(0.06, Game.interceptionChance(0, 300, 300, Game.INT_COVERAGE_PIVOT, 10), 1e-9);
    }

    @Test
    public void generatedPlayers_overallMatchesTheirCappedAttributes() {
        // Generation boosts upperclassmen past the archetype caps; the overall must
        // be recomputed after the caps apply (it used to leave some at 101-105).
        SimRandom.pinNextSeed(1L);
        FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);

        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                assertTrue(p.position + " " + p.name + " OVR " + p.ratOvr, p.ratOvr <= 100);
                assertEquals(p.position + " " + p.name, p.getOverall(), p.ratOvr);
            }
        }
    }

    @Test
    public void prestigeNormalization_keepsLeagueMeanAndStaffBaselinesAligned() {
        FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);

        long startSum = 0;
        for (Team t : league.getTeamList()) {
            startSum += t.teamPrestigeStart;
        }
        // Every program gains 4 (upgrades, bowl season...) except one that gains 12.
        Team standout = league.getTeamList().get(3);
        HeadCoach standoutCoach = standout.getHeadCoach();
        for (Team t : league.getTeamList()) {
            t.teamPrestige = t.teamPrestigeStart + (t == standout ? 12 : 4);
        }
        int coachBaselineBefore = standoutCoach.baselinePrestige;
        int standoutBefore = standout.teamPrestige;
        int standoutEdgeBefore = standout.teamPrestige - league.getTeamList().get(4).teamPrestige;

        league.normalizeLeaguePrestige();

        long nowSum = 0;
        for (Team t : league.getTeamList()) {
            nowSum += t.teamPrestige;
        }
        assertEquals("league total prestige returns exactly to where the season started",
                startSum, nowSum);
        assertEquals("relative gains survive normalization (±1 remainder point)",
                standoutEdgeBefore, standout.teamPrestige - league.getTeamList().get(4).teamPrestige, 1);
        int standoutShift = standoutBefore - standout.teamPrestige;
        assertTrue("every team moves by the common gain, give or take the remainder",
                standoutShift == 4 || standoutShift == 5);
        assertEquals("staff baselines shift with their team",
                coachBaselineBefore - standoutShift, standoutCoach.baselinePrestige);
    }
}
