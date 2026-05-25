package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameBoxScoreTest {

    private Team homeTeam;
    private Team awayTeam;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        homeTeam = league.getTeamList().get(0);
        awayTeam = league.getTeamList().get(1);
    }

    @Test
    public void boxScore_invariant_scoresMatchStatSummary() {
        Game g = new Game(homeTeam, awayTeam, "Box Score Test");
        g.playGame();

        String[] summary = g.getGameSummaryStr();
        assertNotNull("Summary should not be null", summary);
        assertEquals("Summary should have 19 parts", 19, summary.length);

        String centerCol = summary[1];
        assertTrue("Center column should contain away score: " + g.awayScore,
                centerCol.contains(String.valueOf(g.awayScore)));
        String rightCol = summary[2];
        assertTrue("Right column should contain home score: " + g.homeScore,
                rightCol.contains(String.valueOf(g.homeScore)));
    }

    @Test
    public void boxScore_passAndRushYardsSumToTotalYards() {
        Game g = new Game(homeTeam, awayTeam, "Yardage Test");
        g.playGame();

        assertEquals("Home pass + rush should equal homeYards",
                g.homePassYards + g.homeRushYards, g.homeYards);
        assertEquals("Away pass + rush should equal awayYards",
                g.awayPassYards + g.awayRushYards, g.awayYards);
    }

    @Test
    public void boxScore_allStatListsInitializedAfterGame() {
        Game g = new Game(homeTeam, awayTeam, "Stats Init Test");
        g.playGame();

        assertNotNull("Home passing stats", g.homePassingStats);
        assertNotNull("Away passing stats", g.awayPassingStats);
        assertNotNull("Home rushing stats", g.homeRushingStats);
        assertNotNull("Away rushing stats", g.awayRushingStats);
        assertNotNull("Home receiving stats", g.homeReceivingStats);
        assertNotNull("Away receiving stats", g.awayReceivingStats);
        assertNotNull("Home kicking stats", g.homeKickingStats);
        assertNotNull("Away kicking stats", g.awayKickingStats);
        assertNotNull("Home defense stats", g.homeDefenseStats);
        assertNotNull("Away defense stats", g.awayDefenseStats);
    }

    @Test
    public void boxScore_passingStatsHaveCorrectFormat() {
        Game g = new Game(homeTeam, awayTeam, "Pass Format Test");
        g.playGame();

        for (String stat : g.homePassingStats) {
            String[] parts = stat.split(",");
            assertEquals("Passing stat should have 9 fields", 9, parts.length);
        }
        for (String stat : g.awayPassingStats) {
            String[] parts = stat.split(",");
            assertEquals("Passing stat should have 9 fields", 9, parts.length);
        }
    }

    @Test
    public void boxScore_rushingStatsHaveCorrectFormat() {
        Game g = new Game(homeTeam, awayTeam, "Rush Format Test");
        g.playGame();

        for (String stat : g.homeRushingStats) {
            String[] parts = stat.split(",");
            assertEquals("Rushing stat should have 7 fields", 7, parts.length);
        }
        for (String stat : g.awayRushingStats) {
            String[] parts = stat.split(",");
            assertEquals("Rushing stat should have 7 fields", 7, parts.length);
        }
    }

    @Test
    public void boxScore_receivingStatsHaveCorrectFormat() {
        Game g = new Game(homeTeam, awayTeam, "Rec Format Test");
        g.playGame();

        for (String stat : g.homeReceivingStats) {
            String[] parts = stat.split(",");
            assertEquals("Receiving stat should have 8 fields", 8, parts.length);
        }
        for (String stat : g.awayReceivingStats) {
            String[] parts = stat.split(",");
            assertEquals("Receiving stat should have 8 fields", 8, parts.length);
        }
    }

    @Test
    public void boxScore_kickingStatsHaveCorrectFormat() {
        Game g = new Game(homeTeam, awayTeam, "Kick Format Test");
        g.playGame();

        for (String stat : g.homeKickingStats) {
            String[] parts = stat.split(",");
            assertEquals("Kicking stat should have 7 fields", 7, parts.length);
        }
        for (String stat : g.awayKickingStats) {
            String[] parts = stat.split(",");
            assertEquals("Kicking stat should have 7 fields", 7, parts.length);
        }
    }

    @Test
    public void boxScore_defenseStatsHaveCorrectFormat() {
        Game g = new Game(homeTeam, awayTeam, "Def Format Test");
        g.playGame();

        for (String stat : g.homeDefenseStats) {
            String[] parts = stat.split(",");
            assertEquals("Defense stat should have 9 fields", 9, parts.length);
        }
        for (String stat : g.awayDefenseStats) {
            String[] parts = stat.split(",");
            assertEquals("Defense stat should have 9 fields", 9, parts.length);
        }
    }

    @Test
    public void boxScore_quarterScoresAreNonNegative() {
        Game g = new Game(homeTeam, awayTeam, "Quarter Test");
        g.playGame();

        for (int i = 0; i < g.homeQScore.length; i++) {
            assertTrue("Home Q" + i + " should be >= 0: " + g.homeQScore[i],
                    g.homeQScore[i] >= 0);
            assertTrue("Away Q" + i + " should be >= 0: " + g.awayQScore[i],
                    g.awayQScore[i] >= 0);
        }
    }

    @Test
    public void boxScore_overtimeScoreIsRecorded() {
        Game g = new Game(homeTeam, awayTeam, "OT Test");
        g.playGame();

        if (g.numOT > 0) {
            int homeOT = 0;
            for (int i = 4; i < 4 + g.numOT; i++) {
                homeOT += g.homeQScore[i];
            }
            int awayOT = 0;
            for (int i = 4; i < 4 + g.numOT; i++) {
                awayOT += g.awayQScore[i];
            }
            assertTrue("OT home score should be > 0 when numOT > 0: " + homeOT, homeOT > 0);
            assertTrue("OT away score should be > 0 when numOT > 0: " + awayOT, awayOT > 0);
        }
    }

    @Test
    public void boxScore_getGameSummaryStrV2_returnsValidStructure() {
        Game g = new Game(homeTeam, awayTeam, "V2 Summary Test");
        g.playGame();

        String[] v2 = g.getGameSummaryStrV2();
        assertNotNull("V2 summary should not be null", v2);
        assertEquals("V2 summary should have 19 parts", 19, v2.length);
    }

    @Test
    public void boxScore_scoutStr_containsTeamInfo() {
        Game g = new Game(homeTeam, awayTeam, "Scout Test");
        g.playGame();

        String[] scout = g.getGameScoutStr();
        assertNotNull("Scout should not be null", scout);
        assertEquals("Scout should have 4 parts", 4, scout.length);
        assertNotNull("Scout label should not be null", scout[0]);
        assertTrue("Scout label should mention ranking or record",
                scout[0].contains("Ranking") || scout[0].contains("Record")
                        || scout[0].contains("HC") || scout[0].contains("PPG"));
    }

    @Test
    public void boxScore_playByPlayLog_nonEmptyAfterGame() {
        Game g = new Game(homeTeam, awayTeam, "PBP Test");
        g.playGame();

        String pbp = g.getPlayByPlayLog();
        assertNotNull("Play-by-play should not be null", pbp);
        assertTrue("Play-by-play should not be empty", pbp.length() > 0);
        assertFalse("Play-by-play should not contain placeholder",
                pbp.contains("No play-by-play data available"));
    }

    @Test
    public void boxScore_returnAverages_areNonNegativeWithGamesPlayed() {
        Game g = new Game(homeTeam, awayTeam, "Return Avg Test");
        g.playGame();

        assertTrue("Home kick return avg should be >= 0", g.hkReturnAvg >= 0);
        assertTrue("Away kick return avg should be >= 0", g.akReturnAvg >= 0);
        assertTrue("Home punt return avg should be >= 0", g.hpReturnAvg >= 0);
        assertTrue("Away punt return avg should be >= 0", g.apReturnAvg >= 0);
    }

    @Test
    public void boxScore_turnoverCountsAreConsistent() {
        Game g = new Game(homeTeam, awayTeam, "TO Test");
        g.playGame();

        assertTrue("Home TOs should be >= 0", g.homeTOs >= 0);
        assertTrue("Away TOs should be >= 0", g.awayTOs >= 0);
    }

    @Test
    public void boxScore_totalYardsConsistentAcrossMultipleGames() {
        for (int i = 0; i < 5; i++) {
            Game g = new Game(homeTeam, awayTeam, "Multi Game " + i);
            g.playGame();

            int homeTotal = g.homePassYards + g.homeRushYards;
            int awayTotal = g.awayPassYards + g.awayRushYards;
            assertEquals("Game " + i + " home yards mismatch", g.homeYards, homeTotal);
            assertEquals("Game " + i + " away yards mismatch", g.awayYards, awayTotal);
        }
    }
}
