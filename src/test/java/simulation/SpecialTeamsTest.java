package simulation;

import org.junit.Test;
import positions.PlayerReturner;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Kickoffs, punts and their returns. */
public class SpecialTeamsTest {

    private static final Pattern PUNT = Pattern.compile(
            "Punt!\\n\\S+ punts from the (\\d+) yard line\\. (?:.*(?:returns the punt to the|ball at the) (\\d+) yard line|.*(touchback))");
    private static final Pattern KICKOFF = Pattern.compile("Kick-off!\\n.*?(touchback|fair catch|returns the kickoff)");

    @Test
    public void kicksTravelCollegeDistances() {
        SimRandom.pinNextSeed(77L);
        int n = 20000;
        double punt85 = 0, punt95 = 0;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, deepKickoffs = 0;
        for (int i = 0; i < n; i++) {
            int d = Game.puntDistance(85);
            min = Math.min(min, d);
            max = Math.max(max, d);
            punt85 += d;
            punt95 += Game.puntDistance(95);
            // From the 35, a kick of more than 68 yards lands deeper than 3 yards into the end zone.
            if (Game.kickoffDistance(85) > 68) deepKickoffs++;
        }
        // FBS gross punting ~43 yards. Punts were power minus 25-44: ~50 for an 85 and ~60 for a 95.
        assertBetween("gross punt at 85 power", punt85 / n, 42, 45);
        assertTrue("punt range " + min + "-" + max, min >= 34 && max <= 53);
        assertEquals("a yard per two points of power", 5, punt95 / n - punt85 / n, 0.5);
        // About half of kickoffs go for touchbacks (it was ~5%).
        assertBetween("kickoffs deep in the end zone", deepKickoffs / (double) n, 0.40, 0.65);
    }

    @Test
    public void onsideKicksAreLongShots() {
        // FBS ~11% when the return team expects it; it was 25-33%.
        assertBetween("onside at 80 form", Game.onsideRecoveryChance(80), 0.10, 0.14);
        double previous = 0;
        for (int form = 40; form <= 100; form += 5) {
            double p = Game.onsideRecoveryChance(form);
            assertTrue("better kickers recover more at " + form, p >= previous);
            assertTrue("onside chance in range at " + form, p >= 0.03 && p <= 0.20);
            previous = p;
        }
    }

    @Test
    public void puntsAndKickoffsLookLikeCollegeFootball() {
        SimRandom.pinNextSeed(31337L);
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

        int punts = 0, puntTouchbacks = 0, deepPunts = 0, deepStart = 0, plusPunts = 0, plusStart = 0;
        int kickoffs = 0, kickoffTouchbacks = 0;
        int kickReturns = 0, kickYards = 0, puntReturns = 0, puntYards = 0;
        for (int i = 0; i < 150; i++) {
            Team home = teams.get((i * 11) % teams.size());
            Team away = teams.get((i * 11 + 5) % teams.size());
            Game g = new Game(home, away, "Special teams");
            g.playGame();
            for (PlayerReturner r : new PlayerReturner[]{g.homeKickReturner, g.awayKickReturner}) {
                kickReturns += r.kReturns;
                kickYards += r.kYards;
                puntReturns += r.pReturns;
                puntYards += r.pYards;
            }
            Matcher p = PUNT.matcher(g.gameEventLog);
            while (p.find()) {
                punts++;
                int from = Integer.parseInt(p.group(1));
                if (p.group(3) != null) {
                    puntTouchbacks++;
                    continue;
                }
                int start = Integer.parseInt(p.group(2));
                if (from <= 20) {
                    deepPunts++;
                    deepStart += start;
                } else if (from >= 55) {
                    plusPunts++;
                    plusStart += start;
                }
            }
            Matcher k = KICKOFF.matcher(g.gameEventLog);
            while (k.find()) {
                kickoffs++;
                if (k.group(1).equals("touchback")) kickoffTouchbacks++;
            }
        }

        // Field position after a punt follows where it was kicked from. The receiver
        // started near its 25 from anywhere: punts were measured from the wrong side of the field.
        assertTrue("punts " + punts, punts > 500);
        assertBetween("receiver after a punt from inside the punter's 20", deepStart / (double) deepPunts, 38, 58);
        assertBetween("receiver after a punt from plus territory", plusStart / (double) plusPunts, 3, 20);
        // FBS: ~8% of punts are touchbacks (it was 77%), ~35-40% are returned for ~8-9 yards.
        assertBetween("punt touchbacks", puntTouchbacks / (double) punts, 0.01, 0.15);
        assertBetween("punts returned", puntReturns / (double) punts, 0.25, 0.50);
        assertBetween("punt return average", puntYards / (double) puntReturns, 5, 12);
        // FBS: about half of kickoffs are touchbacks, returns average ~21 yards.
        assertTrue("kickoffs " + kickoffs, kickoffs > 500);
        assertBetween("kickoff touchbacks", kickoffTouchbacks / (double) kickoffs, 0.35, 0.65);
        assertBetween("kick return average", kickYards / (double) kickReturns, 18, 26);
    }

    private static void assertBetween(String what, double v, double lo, double hi) {
        assertTrue(what + " " + v, v >= lo && v <= hi);
    }
}
