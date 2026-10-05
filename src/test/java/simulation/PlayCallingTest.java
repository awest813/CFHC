package simulation;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Run/pass calls by down and distance, and the pace that sets how many plays a game has. */
public class PlayCallingTest {

    private static final Pattern SNAP = Pattern.compile("(?m)^\\S+ \\S+ \\S+ (\\d) and (\\S+) at (-?\\d+) yard line\\.\\n(.*)$");

    @Test
    public void shortYardageCallsFollowTheDistanceAndTheBook() {
        PlaybookOffense balanced = PlaybookOffense.forIndex(0);
        assertEquals(0.75, Game.shortYardageRunChance(1, balanced), 1e-9);
        assertEquals(0.50, Game.shortYardageRunChance(3, balanced), 1e-9);
        assertEquals(0.06, Game.shortYardageRunChance(15, balanced), 1e-9);
        double previous = 1;
        for (int yards = 1; yards <= 12; yards++) {
            double p = Game.shortYardageRunChance(yards, balanced);
            assertTrue("runs fall off with distance at " + yards, p <= previous);
            previous = p;
        }
        PlaybookOffense zoneRead = PlaybookOffense.forIndex(4);
        PlaybookOffense airRaid = PlaybookOffense.forIndex(3);
        assertTrue(Game.shortYardageRunChance(2, zoneRead) > Game.shortYardageRunChance(2, airRaid));
    }

    @Test
    public void callsAndPaceLookLikeCollegeFootball() {
        SimRandom.pinNextSeed(424242L);
        FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(res);
        league.fullGameLog = true;
        List<Team> teams = league.getTeamList();

        int[] all = new int[2], firstAndTen = new int[2], secondShort = new int[2], thirdShort = new int[2], thirdLong = new int[2];
        int games = 0;
        for (int i = 0; i < 120; i++) {
            Team home = teams.get((i * 13) % teams.size());
            Team away = teams.get((i * 13 + 7) % teams.size());
            Game g = new Game(home, away, "Calls");
            g.playGame();
            games++;
            Matcher m = SNAP.matcher(g.gameEventLog);
            while (m.find()) {
                String play = m.group(4);
                Boolean run = play.contains(" rushed for ") || play.contains(" scrambled for ") ? Boolean.TRUE
                        : play.contains("caught the pass") || play.contains("incomplete pass") || play.contains("dropped the catch") ? Boolean.FALSE
                        : null;
                if (run == null) continue;
                int down = Integer.parseInt(m.group(1));
                int toGo = m.group(2).equals("Goal") ? Math.max(1, 100 - Integer.parseInt(m.group(3))) : Integer.parseInt(m.group(2));
                tally(all, run);
                if (down == 1 && toGo == 10) tally(firstAndTen, run);
                if (down == 2 && toGo <= 2) tally(secondShort, run);
                if (down == 3 && toGo <= 2) tally(thirdShort, run);
                if (down == 3 && toGo >= 7) tally(thirdLong, run);
            }
        }
        // FBS: ~53% runs, ~56% on 1st and 10, ~70% on 2nd or 3rd and short, ~10-15% on 3rd and long.
        // Runs were 45% of plays, every 3rd and 1-2 was a run and no 3rd and long was.
        assertBetween("run share", share(all), 0.47, 0.60);
        assertBetween("1st and 10", share(firstAndTen), 0.50, 0.65);
        assertBetween("2nd and short", share(secondShort), 0.60, 0.85);
        assertBetween("3rd and short", share(thirdShort), 0.55, 0.85);
        assertBetween("3rd and long", share(thirdLong), 0.04, 0.20);
        // ~70 snaps a team (incompletions, sacks and picks are not in the run/pass tally above).
        double snaps = (all[0] + all[1]) / (2.0 * games);
        assertTrue("snaps per team " + snaps, snaps > 55 && snaps < 75);
    }

    private static void tally(int[] t, boolean run) {
        if (run) t[0]++; else t[1]++;
    }

    private static double share(int[] t) {
        return t[0] / (double) (t[0] + t[1]);
    }

    private static void assertBetween(String what, double v, double lo, double hi) {
        assertTrue(what + " " + v, v >= lo && v <= hi);
    }
}
