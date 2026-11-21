package simulation;

import org.junit.Test;

import positions.Player;
import staff.HeadCoach;

import static org.junit.Assert.assertTrue;

public class NflDeclareAndStaffFloorTest {

    private static League newLeague() {
        SimRandom.pinNextSeed(9191L);
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League l = new League(
                r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        l.setPlatformResourceProvider(r);
        return l;
    }

    @Test
    public void earlyEntryBar_isReachableAtEveryPosition_andNeverAbove93() {
        League l = newLeague();
        l.computeNflDeclareThresholds();
        java.util.Map<String, Integer> best = new java.util.HashMap<>();
        for (Team t : l.getTeamList()) {
            for (Player p : t.getAllPlayers()) best.merge(p.position, p.ratOvr, Math::max);
        }
        for (java.util.Map.Entry<String, Integer> e : best.entrySet()) {
            int bar = l.nflDeclareThreshold(e.getKey());
            assertTrue(e.getKey() + " bar " + bar, bar <= Team.NFL_OVR);
            assertTrue(e.getKey() + " best " + e.getValue() + " can't clear bar " + bar,
                    e.getValue() > bar || bar == Team.NFL_OVR || e.getValue() == bar);
        }
    }

    @Test
    public void agedStaff_neverDecliseBelowRatingFloors() {
        League l = newLeague();
        Team t = l.getTeamList().get(3);
        HeadCoach hc = t.getHeadCoach();
        hc.age = 79;
        hc.ratOff = 20;
        hc.ratDef = 20;
        hc.ratTalent = 20;
        hc.ratDiscipline = 15;
        for (int i = 0; i < 5; i++) {
            hc.advanceSeason(-5, -5);
            assertTrue("off " + hc.ratOff, hc.ratOff >= 20);
            assertTrue("def " + hc.ratDef, hc.ratDef >= 20);
            assertTrue("talent " + hc.ratTalent, hc.ratTalent >= 20);
            assertTrue("discipline " + hc.ratDiscipline, hc.ratDiscipline >= 15);
        }
    }
}
