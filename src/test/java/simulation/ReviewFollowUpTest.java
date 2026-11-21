package simulation;

import org.junit.Test;

import positions.Player;
import positions.PlayerWR;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression tests for the fresh-eyes review findings. */
public class ReviewFollowUpTest {

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
        l.fullGameLog = true;
        return l;
    }

    @Test
    public void wouldStart_countsPlayersAlreadySignedThisPortal() {
        League l = newLeague(31L);
        Team t = l.getTeamList().get(0);
        List<PlayerWR> depth = new ArrayList<>(t.getTeamWRs());
        int starters = t.startersWR;
        PlayerWR transfer = depth.get(0);
        int oldLast = depth.get(starters - 1).ratOvr;
        transfer.ratOvr = oldLast + 1;
        // Append clearly better signings (unsorted, as the portal does).
        for (int i = 0; i < starters; i++) {
            PlayerWR signed = new PlayerWR(depth.get(i), t);
            signed.ratOvr = 99;
            depth.add(signed);
        }
        assertFalse("starters are now all 99s", League.wouldStart(depth, starters, transfer));
    }

    @Test
    public void conferenceMateRivals_alwaysMeet() {
        for (long seed = 1; seed <= 8; seed++) {
            League l = newLeague(seed);
            for (Team a : l.getTeamList()) {
                if (a.getRivalName().isEmpty()) continue;
                Team b = l.findTeam(a.getRivalName());
                if (b == null || !b.getConference().equals(a.getConference())) continue;
                boolean meet = false;
                for (Game g : a.getGameSchedule()) {
                    if (g.homeTeam == b || g.awayTeam == b) meet = true;
                }
                assertTrue("seed " + seed + ": " + a.getName() + " never plays rival " + b.getName(), meet);
            }
        }
    }

    @Test
    public void playByPlayClock_neverRunsBackwardsWithinAQuarter() {
        League l = newLeague(77L);
        List<Team> teams = l.getTeamList();
        java.util.regex.Pattern stamp = java.util.regex.Pattern.compile("(\\d+):(\\d\\d) Q(\\d)");
        for (int i = 0; i < 150; i++) {
            Game g = new Game(teams.get(i % teams.size()), teams.get((i * 11 + 5) % teams.size()), "OOC");
            if (g.homeTeam == g.awayTeam) continue;
            g.playGame();
            java.util.regex.Matcher m = stamp.matcher(g.gameEventLog);
            int lastQ = 0, lastSec = Integer.MAX_VALUE;
            while (m.find()) {
                int q = Integer.parseInt(m.group(3));
                int sec = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
                if (q != lastQ) { lastQ = q; lastSec = sec; continue; }
                assertTrue("clock went from " + lastSec + " to " + sec + " in Q" + q, sec <= lastSec);
                lastSec = sec;
            }
        }
    }

    @Test
    public void practiceInjuries_areOnTheInjuryList() {
        League l = newLeague(41L);
        Team t = l.getTeamList().get(2);
        t.practiceFocus = PracticeFocus.PHYSICAL;
        t.focusIntensity = PracticeFocus.FocusIntensity.INTENSE;
        l.currentWeek = 3;
        for (int w = 0; w < 200; w++) t.applyWeeklyPractice();
        int hurt = 0;
        for (Player p : t.getAllPlayers()) {
            if (p.injury != null) {
                hurt++;
                assertTrue(p.name + " hurt in practice but not listed", t.getPlayersInjured().contains(p));
            }
        }
        assertTrue("expected some practice injuries", hurt > 0);
    }
}
