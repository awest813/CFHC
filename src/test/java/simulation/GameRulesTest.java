package simulation;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import positions.Player;
import positions.PlayerK;
import positions.PlayerOL;
import positions.PlayerQB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Rules of the play-by-play engine: overtime (no punts, two-point rules),
 * neutral sites, injured players leaving the game, substitutions and the
 * field-goal attempt bookkeeping.
 */
public class GameRulesTest {

    private League league;
    private Team homeTeam;
    private Team awayTeam;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(20261120L);
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
        league.fullGameLog = true;
        homeTeam = league.getTeamList().get(0);
        awayTeam = league.getTeamList().get(1);
    }

    // ---- reflection helpers (the engine's play methods are private) ----

    private static Object call(Game g, String name, Object... args) throws Exception {
        for (Method m : Game.class.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                m.setAccessible(true);
                return m.invoke(g, args);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void set(Game g, String field, Object value) throws Exception {
        Field f = Game.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(g, value);
    }

    private static boolean getBool(Game g, String field) throws Exception {
        Field f = Game.class.getDeclaredField(field);
        f.setAccessible(true);
        return f.getBoolean(g);
    }

    /** A played game (so weather, lineups and returners are set up), rewound to the top of overtime period {@code ot}, tied. */
    private Game overtimeGame(int ot) throws Exception {
        Game g = new Game(homeTeam, awayTeam, "OOC");
        g.playGame();
        g.homeScore = 21;
        g.awayScore = 21;
        g.playingOT = true;
        g.numOT = ot;
        set(g, "bottomOT", false);
        g.gamePoss = (ot % 2) == 0; // away has the ball first in odd overtimes
        g.gameTime = -1;
        g.gameDown = 1;
        g.gameYardsNeed = 10;
        g.gameYardLine = 75;
        g.tdInfo = "test touchdown";
        return g;
    }

    @Test
    public void overtime_fourthDownNeverPunts() throws Exception {
        for (int trial = 0; trial < 300; trial++) {
            Game g = overtimeGame(1);
            // 4th and 15 from their own 20: far out of field goal range, a sure punt in regulation.
            g.gameYardLine = 20;
            g.gameDown = 4;
            g.gameYardsNeed = 15;
            int logStart = g.gameEventLog.length();
            call(g, "runPlay", awayTeam, homeTeam);
            String log = g.gameEventLog.substring(logStart);
            assertFalse("No punts in overtime:\n" + log, log.contains("Punt!") || log.contains("punts from"));
            boolean bottom = getBool(g, "bottomOT");
            if (g.playingOT && !bottom) {
                // Still the away team's series (converted, or a pre-snap flag).
                assertFalse("The ball cannot change hands mid-series", g.gamePoss);
            } else if (g.playingOT) {
                // The series ended: the home team starts its own from the 25.
                assertTrue(g.gamePoss);
                assertEquals(75, g.gameYardLine);
                assertEquals(1, g.gameDown);
            }
        }
    }

    @Test
    public void secondOvertime_touchdownMustGoForTwo() throws Exception {
        int ones = 0;
        for (int trial = 0; trial < 200; trial++) {
            Game g = overtimeGame(2);
            g.gamePoss = true;
            g.homeScore = 27; // the home side just scored a touchdown at the top of 2OT
            int logStart = g.gameEventLog.length();
            call(g, "kickXP", homeTeam, awayTeam);
            assertTrue("2OT tries are worth 0 or 2: " + g.homeScore, g.homeScore == 27 || g.homeScore == 29);
            assertFalse(g.gameEventLog.substring(logStart).contains("XP"));
        }
        for (int trial = 0; trial < 50; trial++) {
            Game g = overtimeGame(1);
            g.awayScore = 27;
            call(g, "kickXP", awayTeam, homeTeam);
            if (g.awayScore == 28) ones++;
        }
        assertTrue("First overtime still kicks extra points", ones > 0);
    }

    @Test
    public void thirdOvertime_isTwoPointPlaysOnly() throws Exception {
        for (int trial = 0; trial < 200; trial++) {
            Game g = overtimeGame(3);
            int snapsBefore = g.gameEventLog.toString().split("yard line", -1).length;
            call(g, "overtimeConversionRound");
            assertTrue(getBool(g, "bottomOT"));
            assertTrue("Home answers after the away try", g.gamePoss);
            int awayAfterTop = g.awayScore;
            assertTrue(awayAfterTop == 21 || awayAfterTop == 23);
            call(g, "overtimeConversionRound");
            assertTrue(g.homeScore == 21 || g.homeScore == 23);
            if (g.homeScore == g.awayScore) {
                assertTrue(g.playingOT);
                assertEquals(4, g.numOT);
            } else {
                assertFalse(g.playingOT);
            }
            String log = g.gameEventLog.toString();
            assertTrue(log.contains("OVERTIME TWO-POINT TRY"));
            assertEquals("No drive plays in a conversion round", snapsBefore, log.split("yard line", -1).length);
        }
    }

    @Test
    public void fullGames_overtimeHasNoPuntsAndScoresAddUp() {
        int overtimes = 0;
        List<Team> teams = league.getTeamList();
        for (int i = 0; i < 400; i++) {
            Team h = teams.get((i * 7) % teams.size());
            Team a = teams.get((i * 7 + 3) % teams.size());
            Game g = new Game(h, a, "OOC");
            g.playGame();
            assertNotEquals("No ties", g.homeScore, g.awayScore);
            int hq = 0, aq = 0;
            for (int k = 0; k < 10; k++) {
                hq += g.homeQScore[k];
                aq += g.awayQScore[k];
            }
            assertEquals(g.homeScore, hq);
            assertEquals(g.awayScore, aq);
            String log = g.gameEventLog.toString();
            int ot = log.indexOf("OVERTIME!");
            if (ot >= 0) {
                overtimes++;
                String otLog = log.substring(ot);
                assertFalse(otLog, otLog.contains("Punt!") || otLog.contains("BLOCKED PUNT") || otLog.contains("FAKE PUNT"));
                int ot2 = otLog.indexOf("OT2");
                if (ot2 >= 0) {
                    assertFalse("No kicked extra points from 2OT on", otLog.substring(ot2).contains("made the XP"));
                }
            }
        }
        assertTrue("sample should include overtime games", overtimes > 0);
    }

    @Test
    public void playoffGames_areNeutralSites() throws Exception {
        for (String name : new String[]{"Quarterfinal", "Semifinal", "Semis, 1v4", "NCG", "Championship", "Rose Bowl"}) {
            Game g = new Game(homeTeam, awayTeam, name);
            assertTrue(name, g.isNeutralSite());
            g.gamePoss = true;
            assertEquals(name, 0, call(g, "getHFadv"));
            g.gamePoss = false;
            assertEquals(name, 0, call(g, "getHFadv"));
            assertEquals(name, 0, g.crowdAdv());
        }
        Game reg = new Game(homeTeam, awayTeam, "OOC");
        assertFalse(reg.isNeutralSite());
        reg.gamePoss = true;
        int homeEdge = (Integer) call(reg, "getHFadv");
        reg.gamePoss = false;
        int awayEdge = (Integer) call(reg, "getHFadv");
        assertEquals("Home field is worth 3 at a campus game", 3, homeEdge + awayEdge);
    }

    @Test
    public void firstHealthy_skipsInjuredPlayers() {
        List<PlayerQB> qbs = homeTeam.getTeamQBs();
        PlayerQB starter = qbs.get(0);
        assertSame(starter, Game.firstHealthy(qbs, 0));
        starter.injury = new Injury(starter);
        assertSame(qbs.get(1), Game.firstHealthy(qbs, 0));
        // Garbage time starts from QB2; still skips anyone hurt.
        qbs.get(1).injury = new Injury(qbs.get(1));
        PlayerQB pick = Game.firstHealthy(qbs, 1);
        assertNotNull(pick);
        assertTrue(pick.injury == null);
    }

    @Test
    public void lineup_mapsEachTiredOrHurtStarterToADistinctBackup() {
        List<PlayerOL> ol = homeTeam.getTeamOLs();
        int starters = homeTeam.startersOL;
        assertTrue("need two backups", ol.size() >= starters + 2);
        for (PlayerOL p : ol) {
            p.gameFatigue = 100;
            p.injury = null;
        }
        ol.get(1).injury = new Injury(ol.get(1));
        ol.get(3).gameFatigue = 0;
        List<PlayerOL> line = Game.lineup(ol, starters, 0);
        assertEquals(starters, line.size());
        assertEquals("no player twice", starters, new HashSet<>(line).size());
        assertFalse(line.contains(ol.get(1)));
        assertFalse(line.contains(ol.get(3)));
        // The first two backups, in order (the old code skipped the first backup).
        assertSame(ol.get(starters), line.get(1));
        assertSame(ol.get(starters + 1), line.get(3));
    }

    @Test
    public void injuredQuarterback_takesNoSnaps() {
        PlayerQB starter = homeTeam.getTeamQBs().get(0);
        starter.injury = new Injury(starter);
        Game g = new Game(homeTeam, awayTeam, "OOC");
        g.playGame();
        assertEquals(0, starter.gameSnaps);
    }

    @Test
    public void suspendedPlayers_takeNoSnaps() {
        // Suspended starters left at the top of a hand-set depth chart.
        PlayerQB qb = homeTeam.getTeamQBs().get(0);
        qb.isSuspended = true;
        qb.weeksSuspended = 2;
        Player rb = homeTeam.getTeamRBs().get(0);
        rb.isSuspended = true;
        rb.weeksSuspended = 2;
        Game g = new Game(homeTeam, awayTeam, "OOC");
        g.playGame();
        assertEquals(0, qb.gameSnaps);
        assertEquals(0, rb.gameSnaps);
        assertTrue("backup QB should play", homeTeam.getTeamQBs().get(1).gameSnaps > 0);
    }

    @Test
    public void benchPlayers_recoverFatigue() throws Exception {
        Game g = new Game(homeTeam, awayTeam, "OOC");
        List<PlayerOL> ol = homeTeam.getTeamOLs();
        PlayerOL bench = ol.get(ol.size() - 1);
        bench.gameFatigue = 40;
        call(g, "recoup", false, 0);
        assertTrue("bench lineman should catch his breath: " + bench.gameFatigue, bench.gameFatigue > 40);
    }

    @Test
    public void neutralSite_isDecidedByGameType_notName() {
        for (String name : new String[]{"Citrus Classic", "Liberty Invitational", "Semifinal"}) {
            assertTrue(name, new Game(homeTeam, awayTeam, name).isNeutralSite());
        }
        for (String name : new String[]{"OOC", "Conference", "Division", "First Round", "ACC CCG"}) {
            assertFalse(name, new Game(homeTeam, awayTeam, name).isNeutralSite());
        }
    }

    /** A game in progress, regulation, home team with the ball. */
    private Game liveGame() {
        Game g = new Game(homeTeam, awayTeam, "OOC");
        g.playGame();
        g.playingOT = false;
        g.gamePoss = true;
        g.gameDown = 1;
        g.gameYardsNeed = 10;
        g.gameYardLine = 25;
        g.gameEventLog.setLength(0);
        return g;
    }

    @Test
    public void penaltyLog_showsTheDownItWasThrownOn() throws Exception {
        Game g = liveGame();
        g.gameDown = 2;
        g.gameYardsNeed = 7;
        g.gameYardLine = 40;
        Class<?> pc = Class.forName("simulation.Game$PenaltyCall");
        java.lang.reflect.Constructor<?> ctor = pc.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object call = ctor.newInstance(true, "False Start", 5, false);
        Method apply = Game.class.getDeclaredMethod("applyPreSnapPenalty", pc, Team.class, Team.class);
        apply.setAccessible(true);
        apply.invoke(g, call, homeTeam, awayTeam);
        String log = g.gameEventLog.toString();
        assertTrue(log, log.contains(" 2 and 7 at "));
        assertTrue(log, log.contains("False Start"));
    }

    @Test
    public void tiedOnOwnSide_withSecondsLeft_kneelsToOvertime() throws Exception {
        Game g = liveGame();
        int kneels = 0;
        for (int attempt = 0; attempt < 20; attempt++) {
            g.homeScore = 17;
            g.awayScore = 17;
            g.gameTime = 15;
            g.gameDown = 1;
            g.gameYardsNeed = 10;
            g.gameYardLine = 25;
            g.gamePoss = true;
            g.gameEventLog.setLength(0);
            call(g, "runPlay", homeTeam, awayTeam);
            String log = g.gameEventLog.toString();
            if (log.contains("PENALTY")) continue; // a pre-snap flag replaced the snap
            assertTrue(log, log.contains("takes its chances in overtime"));
            assertTrue("no turnover risk taken", g.gamePoss);
            kneels++;
        }
        assertTrue(kneels > 0);
    }

    @Test
    public void passPlays_leaveDepthChartPositionsAlone() {
        List<Integer> before = new ArrayList<>();
        for (Player p : homeTeam.getTeamWRs()) before.add(p.posDepth);
        new Game(homeTeam, awayTeam, "OOC").playGame();
        List<Integer> after = new ArrayList<>();
        for (Player p : homeTeam.getTeamWRs()) after.add(p.posDepth);
        assertEquals(before, after);
    }

    @Test
    public void blowout_benchesEveryStarterAtAPosition() throws Exception {
        Game g = liveGame();
        g.homeScore = 63;
        g.awayScore = 0;
        g.gameTime = 600; // 10:00 left in the fourth, up 63
        List<PlayerOL> ol = homeTeam.getTeamOLs();
        assertTrue("need backups", ol.size() > homeTeam.startersOL);
        @SuppressWarnings("unchecked")
        List<PlayerOL> onField = (List<PlayerOL>) call(null, "lineup", ol, homeTeam.startersOL,
                homeTeam.startersOL);
        for (int i = 0; i < homeTeam.startersOL; i++) {
            assertFalse("starter " + i + " still in", onField.contains(ol.get(i)));
        }
        assertTrue((Boolean) call(g, "benchStarters"));

        // And the play itself uses the backups: starting WRs take no snap.
        List<Integer> snaps = new ArrayList<>();
        for (int i = 0; i < homeTeam.startersWR; i++) snaps.add(homeTeam.getTeamWRs().get(i).gameSnaps);
        call(g, "passingPlay", homeTeam, awayTeam);
        for (int i = 0; i < homeTeam.startersWR; i++) {
            assertEquals("starting WR " + i + " played in garbage time",
                    (int) snaps.get(i), homeTeam.getTeamWRs().get(i).gameSnaps);
        }
    }

    @Test
    public void twoPointChart_secondHalfOnly_onScoreChangingMargins() {
        Game g = liveGame();
        g.gamePoss = true;
        int[][] yes = {{13, 12}, {12, 14}, {20, 15}, {10, 15}, {26, 14}, {10, 20}};
        int[][] no = {{14, 7}, {7, 7}, {21, 14}, {10, 13}, {3, 10}};
        g.gameTime = 600;
        for (int[] sc : yes) {
            g.homeScore = sc[0]; g.awayScore = sc[1];
            assertTrue("go for two at " + sc[0] + "-" + sc[1], g.wantsTwoPoint());
        }
        for (int[] sc : no) {
            g.homeScore = sc[0]; g.awayScore = sc[1];
            assertFalse("kick at " + sc[0] + "-" + sc[1], g.wantsTwoPoint());
        }
        g.gameTime = 2400; // first half: always kick
        g.homeScore = 13; g.awayScore = 12;
        assertFalse(g.wantsTwoPoint());
    }

    @Test
    public void onsideKick_downOneScoreLate_orTwoScoresInLastFive() {
        Game g = liveGame();
        g.gamePoss = true; // home kicking
        g.homeScore = 20; g.awayScore = 27; g.gameTime = 150;
        assertTrue(g.wantsOnsideKick());
        g.gameTime = 250;
        assertFalse("one score with four minutes left: kick deep", g.wantsOnsideKick());
        g.homeScore = 14; g.awayScore = 28;
        assertTrue("two scores down inside five minutes", g.wantsOnsideKick());
        g.homeScore = 7; g.awayScore = 35;
        assertFalse("game is out of reach", g.wantsOnsideKick());
        g.homeScore = 28; g.awayScore = 21;
        assertFalse("leading teams kick deep", g.wantsOnsideKick());
    }

    @Test
    public void blowouts_staySane() {
        List<Team> teams = league.getTeamList();
        int over70 = 0;
        int n = 600;
        for (int i = 0; i < n; i++) {
            Game g = new Game(teams.get(i % teams.size()), teams.get((i * 7 + 3) % teams.size()), "OOC");
            if (g.homeTeam == g.awayTeam) continue;
            g.playGame();
            if (Math.max(g.homeScore, g.awayScore) >= 70) over70++;
            assertTrue("absurd score " + g.homeScore + "-" + g.awayScore, Math.max(g.homeScore, g.awayScore) < 110);
        }
        assertTrue("70-point games should be rare: " + over70, over70 <= n / 50);
    }

    @Test
    public void injuriesDuringAGame_benchThePlayer() {
        // Once the log says a player is out of the game, he never shows up in it
        // again (he used to keep catching, running and throwing).
        List<Team> teams = league.getTeamList();
        int injuries = 0;
        for (int i = 0; i < 300; i++) {
            Team h = teams.get(i % teams.size());
            Team a = teams.get((i + 5) % teams.size());
            Game g = new Game(h, a, "OOC");
            g.playGame();
            String log = g.gameEventLog.toString();
            int at = 0;
            while ((at = log.indexOf(" is out of the game with an injury.", at + 1)) >= 0) {
                int lineStart = log.lastIndexOf('\n', at) + 1;
                String who = log.substring(lineStart, at); // "ABBR POS Name"
                String rest = log.substring(at);
                if (who.contains(" QB ") || who.contains(" RB ") || who.contains(" WR ") || who.contains(" TE ")) {
                    injuries++;
                    assertFalse(who + " played after getting hurt", rest.contains(who + " "));
                }
            }
        }
        assertTrue("sample should include in-game injuries", injuries > 0);
    }

    @Test
    public void fieldGoalAttempts_gameAndSeasonAgree() {
        List<Team> teams = league.getTeamList();
        long game = 0, season = 0;
        for (int i = 0; i < 200; i++) {
            Team h = teams.get((i * 3) % teams.size());
            Team a = teams.get((i * 3 + 1) % teams.size());
            List<PlayerK> ks = new ArrayList<>(h.getTeamKs());
            ks.addAll(a.getTeamKs());
            int[] att = new int[ks.size()];
            for (int k = 0; k < ks.size(); k++) att[k] = ks.get(k).getFGAtt();
            new Game(h, a, "OOC").playGame();
            for (int k = 0; k < ks.size(); k++) {
                season += ks.get(k).getFGAtt() - att[k];
                game += ks.get(k).gameFGAttempts;
            }
        }
        assertTrue(game > 0);
        assertEquals(season, game);
    }

    @Test
    public void coinToss_eitherSideCanReceive() {
        boolean homeFirst = false, awayFirst = false;
        for (int i = 0; i < 40 && !(homeFirst && awayFirst); i++) {
            Game g = new Game(homeTeam, awayTeam, "OOC");
            g.playGame();
            String log = g.gameEventLog.toString();
            if (log.contains(homeTeam.getAbbr() + " wins the coin toss")) homeFirst = true;
            if (log.contains(awayTeam.getAbbr() + " wins the coin toss")) awayFirst = true;
        }
        assertTrue(homeFirst);
        assertTrue(awayFirst);
        assertNotSame(homeTeam, awayTeam);
    }
}
