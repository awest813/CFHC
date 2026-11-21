package simulation;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import positions.PlayerK;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import simulation.Game.FourthDownCall;

/**
 * AI tests for the in-game coaching calls the sim makes for CPU staffs (and the
 * user's team on auto): fourth down, field-goal range, timeouts, the victory
 * formation, the two-point chart and onside kicks. Decision-table rows are
 * constructed situations with the home team on offense; the last test checks
 * league-wide rates over simulated games against FBS norms.
 */
public class AiGameDecisionTest {

    private static League league;
    private Team home;
    private Team away;
    private Game g;

    @BeforeClass
    public static void buildLeague() {
        PlatformLog.setDebugEnabled(false);
        SimRandom.pinNextSeed(0xA1A1L);
        FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(res);
    }

    @Before
    public void setUp() {
        home = league.getTeamList().get(3);
        away = league.getTeamList().get(9);
        // A bowl name makes the site neutral, so home field does not move the kicker's range.
        g = new Game(home, away, "AI Test Bowl");
        g.gamePoss = true;
        kicker(80, 80);
    }

    /** Sets the home kicker's leg and accuracy. */
    private void kicker(int power, int accuracy) {
        PlayerK k = Game.firstHealthy(home.getTeamKs(), 0);
        k.ratAttr1 = power;
        k.ratAttr2 = accuracy;
    }

    /** Fourth down with {@code need} to go at {@code yardLine} (own goal = 0), {@code secondsLeft} in the game. */
    private FourthDownCall fourth(int need, int yardLine, int secondsLeft, int ourScore, int theirScore) {
        return fourth(need, yardLine, secondsLeft, ourScore, theirScore, 0);
    }

    private FourthDownCall fourth(int need, int yardLine, int secondsLeft, int ourScore, int theirScore, int bias) {
        g.gameDown = 4;
        g.gameYardsNeed = need;
        g.gameYardLine = yardLine;
        g.gameTime = secondsLeft;
        g.homeScore = ourScore;
        g.awayScore = theirScore;
        return g.fourthDownCall(home, bias);
    }

    // ---- fourth down ------------------------------------------------------
    @Test
    public void lastSnapOfHalf_ownTerritory_puntsInsteadOfHandingOverPoints() {
        // 0:12 left in the half, 4th-and-12 at our own 15: going for it handed
        // the opponent a short field and a free field goal.
        assertEquals(FourthDownCall.PUNT, fourth(12, 15, 1812, 7, 7));
        assertEquals("past midfield it's points or nothing",
                FourthDownCall.GO, fourth(12, 55, 1812, 7, 7));
    }


    @Test
    public void downTenLate_goesForFourthAndOneAtTheOpponent35() {
        assertEquals(FourthDownCall.GO, fourth(1, 65, 240, 14, 24));
    }

    @Test
    public void fourthAndShort_goesPastOwn35_puntsDeepInOwnTerritory() {
        assertEquals(FourthDownCall.GO, fourth(1, 40, 2400, 7, 7));
        assertEquals(FourthDownCall.GO, fourth(2, 45, 2400, 7, 7));
        assertEquals(FourthDownCall.PUNT, fourth(1, 20, 2400, 7, 7));
        assertEquals(FourthDownCall.PUNT, fourth(10, 25, 3000, 0, 0));
        assertEquals(FourthDownCall.PUNT, fourth(7, 45, 3000, 0, 0));
    }

    @Test
    public void weakLeg_neverTriesLongShots() {
        kicker(55, 60);
        // 55-yard try (opponent 38): out of a weak leg's reach and odds.
        assertNotEquals(FourthDownCall.FIELD_GOAL, fourth(8, 62, 2400, 10, 10));
        assertEquals(0.0, g.fgChanceFromSpot(home), 1e-9);
        // 4th and 8 at the opponent 30: punting nets nothing, so it goes.
        assertEquals(FourthDownCall.GO, fourth(8, 70, 2400, 10, 10));
    }

    @Test
    public void strongLeg_kicksMakeableFieldGoals() {
        kicker(95, 90);
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(8, 70, 2400, 10, 10)); // 47 yds
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(6, 85, 2400, 10, 10)); // 32 yds
    }

    @Test
    public void makeChanceGatesTheTry_notJustRange() {
        // A long leg with a poor aim: 52 yds is in reach but well under 40%.
        kicker(95, 50);
        g.gameYardLine = 65;
        assertTrue("in range", g.fgChanceFromSpot(home) > 0);
        assertTrue("but unlikely", g.fgChanceFromSpot(home) < Game.FG_MIN_CHANCE);
        assertNotEquals(FourthDownCall.FIELD_GOAL, fourth(9, 65, 2400, 10, 10));
    }

    @Test
    public void fgChance_fallsWithDistanceAndRisesWithAccuracy() {
        kicker(90, 80);
        g.gameYardLine = 85;
        double short_ = g.fgChanceFromSpot(home);
        g.gameYardLine = 70;
        double long_ = g.fgChanceFromSpot(home);
        assertTrue(short_ > long_);
        kicker(90, 95);
        assertTrue(g.fgChanceFromSpot(home) > long_);
    }

    @Test
    public void trailingByAFieldGoalLate_kicksWhenMakeable_goesWhenNot() {
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(6, 80, 60, 17, 20));
        assertEquals(FourthDownCall.GO, fourth(6, 40, 60, 17, 20));
        // Lots of clock, backed up, long yardage: punt and play defense.
        assertEquals(FourthDownCall.PUNT, fourth(12, 20, 270, 17, 19));
    }

    @Test
    public void trailingByOneScoreLate_alwaysGoes() {
        assertEquals(FourthDownCall.GO, fourth(12, 20, 270, 10, 16));
        assertEquals(FourthDownCall.GO, fourth(3, 85, 200, 10, 17));
        assertEquals(FourthDownCall.GO, fourth(15, 30, 100, 0, 30));
    }

    @Test
    public void downNineToEleven_kicksWithTimeLeft_goesWithout() {
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(7, 85, 180, 10, 20));
        assertEquals(FourthDownCall.GO, fourth(7, 85, 60, 10, 20));
    }

    @Test
    public void goalLine_goesFromTheOne_kicksFromTheTwo() {
        assertEquals(FourthDownCall.GO, fourth(1, 99, 2400, 0, 0));
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(2, 98, 2400, 0, 0));
    }

    @Test
    public void lastSnapOfTheHalf_takesPointsOrGoes() {
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(9, 75, 1810, 0, 0));
        kicker(40, 60);
        assertEquals(FourthDownCall.GO, fourth(9, 60, 1810, 0, 0));
    }

    @Test
    public void coachDial_cautionAndAggressionBothMoveTheCall() {
        assertEquals(FourthDownCall.GO, fourth(1, 40, 2400, 7, 7, 0));
        assertEquals(FourthDownCall.PUNT, fourth(1, 40, 2400, 7, 7, -2));
        assertEquals(FourthDownCall.PUNT, fourth(3, 40, 2400, 7, 7, 0));
        assertEquals(FourthDownCall.GO, fourth(3, 40, 2400, 7, 7, 2));
    }

    @Test
    public void overtime_neverPunts() {
        g.playingOT = true;
        assertEquals(FourthDownCall.FIELD_GOAL, fourth(9, 75, -1, 21, 21));
        kicker(40, 60);
        assertEquals(FourthDownCall.GO, fourth(9, 62, -1, 21, 21));
        // Down a touchdown in the bottom half: a field goal loses.
        kicker(95, 95);
        assertEquals(FourthDownCall.GO, fourth(8, 85, -1, 21, 28));
    }

    @Test
    public void leadingLate_doesNotGambleDeepInOwnTerritory() {
        assertEquals(FourthDownCall.PUNT, fourth(4, 30, 200, 24, 20));
        assertEquals(FourthDownCall.PUNT, fourth(1, 25, 200, 24, 20));
    }

    // ---- timeouts and the clock --------------------------------------------

    @Test
    public void trailingDefense_stopsTheClockLate() {
        g.homeScore = 21;
        g.awayScore = 14;
        g.gameYardLine = 40;
        g.gameTime = 115; // a 35-second run from 2:30
        g.clockTimeout(home, true, 150);
        assertEquals(2, g.awayTimeouts);
        assertEquals(140, g.gameTime);
        assertEquals(1, g.awayCalls.timeouts);
    }

    @Test
    public void defense_keepsTimeoutsWhenAheadOrOutOfReach() {
        g.homeScore = 14;
        g.awayScore = 21; // offense trails, but it is not two minutes yet
        g.gameTime = 115;
        g.clockTimeout(home, true, 150);
        assertEquals(3, g.awayTimeouts);
        assertEquals(3, g.homeTimeouts);
        g.homeScore = 38;
        g.awayScore = 14; // defense down 24: timeouts can't save it
        g.gameTime = 115;
        g.clockTimeout(home, true, 150);
        assertEquals(3, g.awayTimeouts);
        // An incompletion already stopped the clock.
        g.homeScore = 21;
        g.awayScore = 14;
        g.gameTime = 140;
        g.clockTimeout(home, true, 150);
        assertEquals(3, g.awayTimeouts);
    }

    @Test
    public void trailingOffense_usesTimeoutsInTheTwoMinuteDrill() {
        g.homeScore = 17;
        g.awayScore = 20;
        g.gameYardLine = 55;
        g.gameTime = 70;
        g.clockTimeout(home, true, 100);
        assertEquals(2, g.homeTimeouts);
        assertEquals(90, g.gameTime);
    }

    @Test
    public void endOfHalf_offenseInRangeOfPointsStopsTheClock() {
        g.gameYardLine = 60;
        g.gameTime = 1815;
        g.clockTimeout(home, true, 1845);
        assertEquals(2, g.homeTimeouts);
        g.gameYardLine = 20; // backed up: let it run
        g.gameTime = 1815;
        g.clockTimeout(home, true, 1845);
        assertEquals(2, g.homeTimeouts);
    }

    @Test
    public void victoryFormation_onlyWhenTheKneelsRunOutTheClock() {
        g.homeScore = 24;
        g.awayScore = 21;
        g.gameDown = 1;
        g.gameYardLine = 40;
        g.gameTime = 100;
        g.awayTimeouts = 0;
        assertTrue(g.canKneelOut(home));
        g.awayTimeouts = 2;
        assertFalse("two timeouts leave the opponent time", g.canKneelOut(home));
        g.awayTimeouts = 0;
        g.gameDown = 3;
        assertFalse("one kneel can't burn 1:40", g.canKneelOut(home));
        g.gameDown = 1;
        g.homeScore = 20;
        assertFalse("trailing teams don't kneel", g.canKneelOut(home));
    }

    // ---- two-point chart and onside ---------------------------------------

    @Test
    public void twoPointChart() {
        g.gameTime = 1200;
        int[][] rows = {{1, 1}, {5, 1}, {12, 1}, {-2, 1}, {-5, 1}, {-10, 1}, {7, 0}, {-1, 0}, {0, 0}, {8, 0}};
        for (int[] r : rows) {
            g.homeScore = 30 + r[0];
            g.awayScore = 30;
            assertEquals("margin " + r[0], r[1] == 1, g.wantsTwoPoint());
        }
        g.gameTime = 2400; // first half: kick
        g.homeScore = 31;
        assertFalse(g.wantsTwoPoint());
    }

    @Test
    public void onsideKick_onlyWhenTheBallIsNeededBack() {
        // Home is kicking (it just scored) with gamePoss still true.
        g.homeScore = 20;
        g.awayScore = 27;
        g.gameTime = 150;
        assertTrue(g.wantsOnsideKick());
        g.gameTime = 400;
        assertFalse(g.wantsOnsideKick());
        g.awayScore = 34;
        g.gameTime = 280;
        assertTrue("down 14 inside 5:00", g.wantsOnsideKick());
        g.awayScore = 17;
        g.gameTime = 60;
        assertFalse("leading teams kick deep", g.wantsOnsideKick());
    }

    // ---- league-wide rates -------------------------------------------------

    /**
     * Over 150 simulated games: FBS-like fourth-down aggression (~1.5-2 tries a
     * team a game, ~50-55% converted; it was 0.45), 4-4.5 punts, field goals
     * from ~37 yards with few 50+ tries, and timeouts actually used.
     */
    @Test
    public void leagueRates_lookLikeCollegeFootball() {
        SimRandom.pinNextSeed(0xA1A2L);
        List<Team> teams = league.getTeamList();
        long teamGames = 0, go = 0, conv = 0, punts = 0, fga = 0, fgm = 0, fgYards = 0, fg50 = 0, timeouts = 0;
        for (int i = 0; i < 150; i++) {
            Game game = new Game(teams.get((i * 11) % teams.size()), teams.get((i * 11 + 5) % teams.size()), "Rates");
            game.playGame();
            for (Game.CallStats c : new Game.CallStats[]{game.homeCalls, game.awayCalls}) {
                teamGames++;
                go += c.goAttempts;
                conv += c.goConversions;
                punts += c.punts;
                fga += c.fgAttempts;
                fgm += c.fgMade;
                fgYards += c.fgYards;
                fg50 += c.fgAttempts50;
                timeouts += c.timeouts;
            }
        }
        double goPerGame = go / (double) teamGames;
        double convRate = conv / (double) Math.max(1, go);
        double puntsPerGame = punts / (double) teamGames;
        double fgaPerGame = fga / (double) teamGames;
        double avgFg = fgYards / (double) Math.max(1, fga);
        double share50 = fg50 / (double) Math.max(1, fga);
        String summary = String.format("go %.2f conv %.2f punts %.2f fga %.2f fg%% %.2f avg %.1f 50+ %.2f TO %.2f",
                goPerGame, convRate, puntsPerGame, fgaPerGame, fgm / (double) Math.max(1, fga), avgFg, share50,
                timeouts / (double) teamGames);
        assertTrue(summary, goPerGame > 1.1 && goPerGame < 2.6);
        assertTrue(summary, convRate > 0.42 && convRate < 0.66);
        assertTrue(summary, puntsPerGame > 3.3 && puntsPerGame < 5.0);
        assertTrue(summary, fgaPerGame > 1.1 && fgaPerGame < 2.2);
        assertTrue(summary, avgFg > 33 && avgFg < 40);
        assertTrue(summary, share50 < 0.12);
        assertTrue(summary, timeouts / (double) teamGames > 0.5);
    }
}
