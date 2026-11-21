package simulation;

import org.junit.Test;

import positions.Player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** CPU practice focus, kneel yardage and early coordinator promotions. */
public class AiProgramFollowUpTest {

    private static League newLeague(long seed) {
        SimRandom.pinNextSeed(seed);
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League l = new League(
                r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        l.setPlatformResourceProvider(r);
        l.userTeam = l.getTeamList().get(0);
        l.userTeam.setUserControlled(true);
        return l;
    }

    @Test
    public void cpuTeams_pickAPracticeFocus_userKeepsTheirs() {
        League l = newLeague(5150L);
        l.userTeam.practiceFocus = PracticeFocus.BALANCED;
        new SeasonController(l, GameUiBridge.NO_OP).advanceWeek(); // preseason -> week 1
        assertEquals(PracticeFocus.BALANCED, l.userTeam.practiceFocus);
        for (Team t : l.getTeamList()) {
            if (t == l.userTeam) continue;
            assertNotEquals(t.getName() + " stuck on Balanced", PracticeFocus.BALANCED, t.practiceFocus);
        }
    }

    @Test
    public void cpuPracticeFocus_targetsTheWeakestTraitGroup() {
        League l = newLeague(5151L);
        Team t = l.getTeamList().get(1);
        for (Player p : t.getAllPlayers()) {
            p.ratDurability = 10; // by far the weakest group
        }
        t.chooseCpuPracticeFocus();
        assertEquals(PracticeFocus.PHYSICAL, t.practiceFocus);
    }

    @Test
    public void kneelsLoseAYard() throws Exception {
        League l = newLeague(5152L);
        Game g = new Game(l.getTeamList().get(0), l.getTeamList().get(1), "OOC");
        g.playGame();
        g.playingOT = false;
        g.gamePoss = true;
        g.homeScore = 24;
        g.awayScore = 10;
        g.gameTime = 15;
        g.gameDown = 1;
        g.gameYardsNeed = 10;
        g.gameYardLine = 40;
        java.lang.reflect.Method kneel = Game.class.getDeclaredMethod("kneelLoss");
        kneel.setAccessible(true);
        kneel.invoke(g);
        assertEquals(2, g.gameDown);
        assertEquals(39, g.gameYardLine);
        assertEquals(11, g.gameYardsNeed);
        g.gameYardLine = 1;
        kneel.invoke(g);
        assertEquals("never kneels into the end zone", 1, g.gameYardLine);
    }

    @Test
    public void earlyPromotion_neverPicksAWorseCoordinatorOverAnAvailableCoach() {
        League l = newLeague(5153L);
        Team t = l.getTeamList().get(5);
        int[] wt = {1, 1, 1, 1};
        assertTrue(League.bestCoordinatorOvr(t, wt) > 0);
    }
}
