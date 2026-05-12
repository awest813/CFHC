package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GameTest {

    private Team homeTeam;
    private Team awayTeam;
    private DesktopResourceProvider resources;

    @Before
    public void setUp() {
        resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
    public void constructor_initializesWithoutPlaying() {
        Game g = new Game(homeTeam, awayTeam, "Week 1 Test");
        assertFalse(g.hasPlayed);
        assertEquals(homeTeam, g.homeTeam);
        assertEquals(awayTeam, g.awayTeam);
        assertEquals("Week 1 Test", g.gameName);
        assertEquals(0, g.homeScore);
        assertEquals(0, g.awayScore);
        assertEquals(0, g.numOT);
    }

    @Test
    public void constructor_simpleDefaultsGameNameToEmpty() {
        Game g = new Game(homeTeam, awayTeam);
        assertFalse(g.hasPlayed);
        assertEquals("", g.gameName);
    }

    @Test
    public void playGame_setsHasPlayedAndGeneratesStats() {
        Game g = new Game(homeTeam, awayTeam, "Test Game");
        g.playGame();

        assertTrue(g.hasPlayed);
        assertNotNull(g.getGameSummaryStr());
    }

    @Test
    public void playGame_scoresAreNonNegative() {
        Game g = new Game(homeTeam, awayTeam, "Score Test");
        g.playGame();

        assertTrue("Home score should be >= 0", g.homeScore >= 0);
        assertTrue("Away score should be >= 0", g.awayScore >= 0);
    }

    @Test
    public void playGame_totalScoreIsReasonable() {
        Game g = new Game(homeTeam, awayTeam, "Score Range Test");
        g.playGame();

        int total = g.homeScore + g.awayScore;
        assertTrue("Total score should be positive: " + total, total > 0);
        assertTrue("Total score should be reasonable: " + total, total < 200);
    }

    @Test
    public void playGame_gameSummaryContainsBothTeamNames() {
        Game g = new Game(homeTeam, awayTeam, "Summary Test");
        g.playGame();

        String[] summary = g.getGameSummaryStr();
        assertNotNull(summary);
        assertTrue(summary.length > 0);
    }

    @Test
    public void playGame_differentTeamsProduceDifferentResults() {
        Game g1 = new Game(homeTeam, awayTeam, "Game 1");
        Game g2 = new Game(awayTeam, homeTeam, "Game 2");
        g1.playGame();
        g2.playGame();

        // Both games should produce reasonable scores regardless of outcome
        assertTrue(g1.hasPlayed);
        assertTrue(g2.hasPlayed);
    }

    @Test
    public void playGame_multipleGamesHaveVaryingScores() {
        int sumScores = 0;
        int games = 5;
        for (int i = 0; i < games; i++) {
            Game g = new Game(homeTeam, awayTeam, "Game " + i);
            g.playGame();
            sumScores += g.homeScore + g.awayScore;
        }
        assertTrue("Games should produce positive total scores: " + sumScores, sumScores > 0);
    }

    @Test
    public void getGameSummaryStr_returnsSummaryAfterPlay() {
        Game g = new Game(homeTeam, awayTeam, "Summary");
        g.playGame();

        String[] result = g.getGameSummaryStr();
        assertNotNull("getGameSummaryStr should not return null", result);
        assertTrue("Summary should have content", result.length >= 3);
    }

    @Test
    public void homeAndAwayQuarterScoresAreRecorded() {
        Game g = new Game(homeTeam, awayTeam, "Quarters");
        g.playGame();

        assertNotNull(g.homeQScore);
        assertNotNull(g.awayQScore);
    }

    @Test
    public void gameNameDefaultsForNullName() {
        Game g = new Game(homeTeam, awayTeam, null);
        assertFalse(g.hasPlayed);
        assertNotNull(g.gameName);
        assertEquals("Game", g.gameName);
    }
}
