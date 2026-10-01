package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.PlayerQB;
import positions.PlayerRB;
import positions.PlayerTE;
import positions.PlayerWR;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Passing and rushing: the play curves, and the stats and rules every game must keep. */
public class PassRushTest {

    private static final Pattern SNAP = Pattern.compile("\\S+ \\d and \\S+ at (-?\\d+) yard line\\.");
    /** A snap header ("time poss down and dist at yard line.") and the team named on the play line under it. */
    private static final Pattern SNAP_AND_PLAY = Pattern.compile("(?m)^\\S+ \\S+ (\\S+) \\d and \\S+ at -?\\d+ yard line\\.\\n(\\S+) (RB|QB|WR|TE) ");
    private static final Pattern LONG_TD_RUN = Pattern.compile("rushed [4-9]\\d yards for a TD");

    private final FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    private League league;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(31337L);
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.fullGameLog = true;
    }

    @Test
    public void interceptionsFollowTheQuarterback() {
        int p = Game.LEAGUE_PRESSURE;
        assertEquals(Game.INT_BASE_CHANCE, Game.interceptionChance(Game.INT_QB_PIVOT, p, Game.INT_SAFETY_PIVOT, 0), 1e-9);
        double accurate = Game.interceptionChance(95, p, 78, 0);
        double erratic = Game.interceptionChance(62, p, 78, 0);
        assertTrue("95-skill QB " + accurate, accurate > 0.009 && accurate < 0.015);
        assertTrue("62-skill QB " + erratic, erratic > 0.03 && erratic < 0.04);
        assertTrue("pressure adds picks", Game.interceptionChance(84, 100, 78, 0) > Game.interceptionChance(84, 60, 78, 0));
        assertTrue("a better safety adds picks", Game.interceptionChance(84, p, 90, 0) > Game.interceptionChance(84, p, 70, 0));
        assertEquals(Game.INT_MIN_CHANCE, Game.interceptionChance(200, 0, 0, -10), 1e-9);
        assertEquals(Game.INT_MAX_CHANCE, Game.interceptionChance(0, 200, 200, 10), 1e-9);
    }

    @Test
    public void sacksFollowThePassRushAgainstTheLine() {
        assertEquals(Game.SACK_RATE, Game.sackChance(Game.LEAGUE_PRESSURE), 1e-9);
        assertTrue(Game.sackChance(100) > 1.8 * Game.sackChance(80));
        assertTrue(Game.sackChance(60) < 0.6 * Game.sackChance(80));
        assertEquals(0.01, Game.sackChance(-100), 1e-9);
        assertEquals(0.16, Game.sackChance(300), 1e-9);
    }

    @Test
    public void runsAreStuffedLessBehindBetterBlocking() {
        assertEquals(Game.RUN_STUFF_CHANCE, Game.runStuffChance(0), 1e-9);
        assertTrue(Game.runStuffChance(10) < Game.runStuffChance(0));
        assertTrue(Game.runStuffChance(-10) > Game.runStuffChance(0));
        assertEquals(0.04, Game.runStuffChance(100), 1e-9);
        assertEquals(0.30, Game.runStuffChance(-100), 1e-9);
    }

    @Test
    public void fasterCarriersBreakAwayMoreAndFurther() {
        assertEquals(0.0, Game.breakawayChance(50), 1e-9);
        assertTrue(Game.breakawayChance(95) > Game.breakawayChance(75));
        int max = 0;
        for (int i = 0; i < 2000; i++) {
            int y = Game.openFieldYards(90);
            assertTrue("open field " + y, y >= 18 && y < 18 + 62);
            max = Math.max(max, y);
        }
        assertTrue("fast runners can go a long way: " + max, max >= 70);
    }

    @Test
    public void everyGameKeepsItsStatsAndRules() {
        List<Team> teams = league.getTeamList();
        int safeties = 0;
        int longRuns = 0;
        for (int i = 0; i < 160; i++) {
            Team home = teams.get((i * 7) % teams.size());
            Team away = teams.get((i * 7 + 3 + i / teams.size()) % teams.size());
            if (home == away) continue;
            Game g = new Game(home, away, "Pass/rush invariants");
            g.playGame();
            String log = g.gameEventLog.toString();

            checkSide(g, home, g.homeScore, g.homeQScore, g.homePassYards, g.homeRushYards);
            checkSide(g, away, g.awayScore, g.awayQScore, g.awayPassYards, g.awayRushYards);

            Matcher m = SNAP.matcher(log);
            while (m.find()) {
                int yardLine = Integer.parseInt(m.group(1));
                assertTrue("snap from the " + yardLine + " yard line", yardLine > 0 && yardLine < 100);
            }
            Matcher play = SNAP_AND_PLAY.matcher(log);
            while (play.find()) {
                // The first snap after halftime used to be run by the team that ended the half.
                assertEquals("the team with the ball runs the play: " + play.group(), play.group(1), play.group(2));
            }
            safeties += count(log, "SAFETY!");
            Matcher run = LONG_TD_RUN.matcher(log);
            while (run.find()) longRuns++;
        }
        assertTrue("safeties happen (" + safeties + ")", safeties > 0);
        assertTrue("long touchdown runs happen (" + longRuns + ")", longRuns > 0);
    }

    private static void checkSide(Game g, Team t, int score, int[] quarters, int passYards, int rushYards) {
        int quarterSum = 0;
        for (int q : quarters) quarterSum += q;
        assertEquals(t.getAbbr() + " quarter scores add up to the final", score, quarterSum);

        int qbComp = 0, qbPassYds = 0, rec = 0, recYds = 0, rushYds = 0;
        for (PlayerQB q : t.getTeamQBs()) {
            qbComp += q.gamePassComplete;
            qbPassYds += q.gamePassYards;
            rushYds += q.gameRushYards;
            assertTrue("sacks are rushing attempts", q.gameRushAttempts >= q.gameSacks);
        }
        for (PlayerWR p : t.getTeamWRs()) { rec += p.gameReceptions; recYds += p.gameRecYards; }
        for (PlayerTE p : t.getTeamTEs()) { rec += p.gameReceptions; recYds += p.gameRecYards; }
        for (PlayerRB p : t.getTeamRBs()) { rec += p.gameReceptions; recYds += p.gameRecYards; rushYds += p.gameRushYards; }
        // A TD pass used to credit the QB's completion and yards twice.
        assertEquals(t.getAbbr() + " QB completions = catches", rec, qbComp);
        assertEquals(t.getAbbr() + " QB yards = receiving yards", recYds, qbPassYds);
        assertEquals(t.getAbbr() + " box score pass yards", qbPassYds, passYards);
        assertEquals(t.getAbbr() + " box score rush yards", rushYds, rushYards);
    }

    @Test
    public void teamSeasonTotalsMatchThePlayers() {
        Team home = league.getTeamList().get(0);
        Team away = league.getTeamList().get(1);
        for (int i = 0; i < 6; i++) {
            Game g = i % 2 == 0 ? new Game(home, away, "Totals") : new Game(away, home, "Totals");
            g.playGame();
        }
        for (Team t : new Team[]{home, away}) {
            int pass = 0, rush = 0;
            for (PlayerQB q : t.getTeamQBs()) {
                pass += q.getPassYards();
                rush += q.getRushYards();
            }
            for (PlayerRB r : t.getTeamRBs()) rush += r.getRushYards();
            assertEquals(t.getAbbr() + " season passing yards", pass, t.teamPassYards);
            // Sack losses are rushing yards for the QB; the offense's total left them out.
            assertEquals(t.getAbbr() + " season rushing yards", rush, t.teamRushYards);
        }
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int i = s.indexOf(needle); i >= 0; i = s.indexOf(needle, i + 1)) n++;
        return n;
    }
}
