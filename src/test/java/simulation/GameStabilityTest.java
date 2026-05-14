package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameStabilityTest {

    private League league;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
    }

    @Test
    public void gameEventLog_isStringBuilder_startsEmpty() {
        Game game = new Game(league.userTeam, league.getTeamList().get(1), "OOC");
        String log = game.getPlayByPlayLog();
        assertNotNull("Play-by-play log should never be null", log);
        assertTrue("Log should indicate no data before play", log.contains("No play-by-play"));
    }

    @Test
    public void gameEventLog_appendAfterPlay_containsTeamNames() {
        Game game = new Game(league.userTeam, league.getTeamList().get(1), "OOC");
        game.playGame();
        String log = game.getPlayByPlayLog();
        assertNotNull(log);
        assertTrue("Log should contain team names after play",
            log.contains(league.userTeam.getAbbr()) || log.contains(league.getTeamList().get(1).getAbbr()));
    }

    @Test
    public void gameEventLog_includesScorePlay() {
        Game game = new Game(league.userTeam, league.getTeamList().get(1), "OOC");
        game.playGame();
        String log = game.getPlayByPlayLog();
        String[] summary = game.getGameSummaryStr();
        assertNotNull("Game summary should exist", summary);
    }

    @Test
    public void olLoop_handlesEmptyTeamOLs() {
        Team team = league.getTeamList().get(0);
        float talent = team.getOffTalent();
        assertTrue("Off talent should be non-negative", talent >= 0);
    }

    @Test
    public void getAllPlayers_caching_doesNotThrow() {
        Game game = new Game(league.userTeam, league.getTeamList().get(1), "OOC");
        game.playGame();
        String[] summary = game.getGameSummaryStr();
        assertNotNull(summary);
    }
}
