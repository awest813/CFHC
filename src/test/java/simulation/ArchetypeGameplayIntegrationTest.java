package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import positions.*;

import static org.junit.Assert.*;

public class ArchetypeGameplayIntegrationTest {

    private League league;
    private Team homeTeam;
    private Team awayTeam;

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
        homeTeam = league.getTeamList().get(0);
        awayTeam = league.getTeamList().get(1);
    }

    private void assignAllArchetypes(Team t) {
        t.getQB(0).archetypeTag = Archetypes.QB_POCKET;
        t.getQB(1).archetypeTag = Archetypes.QB_SCRAMBLER;

        t.getRB(0).archetypeTag = Archetypes.RB_SPEED;
        t.getRB(1).archetypeTag = Archetypes.RB_POWER;
        t.getRB(2).archetypeTag = Archetypes.RB_RECEIVING;

        t.getWR(0).archetypeTag = Archetypes.WR_DEEP_THREAT;
        t.getWR(1).archetypeTag = Archetypes.WR_ROUTE_RUNNER;
        t.getWR(2).archetypeTag = Archetypes.WR_SLOT;

        t.getTE(0).archetypeTag = Archetypes.TE_RECEIVING;
        t.getTE(1).archetypeTag = Archetypes.TE_BLOCKING;

        for (int i = 0; i < 5 && t.getOL(i) != null; i++) {
            t.getOL(i).archetypeTag = (i % 2 == 0) ? Archetypes.OL_PASS_PROTECTOR : Archetypes.OL_RUN_BLOCKER;
        }

        for (int i = 0; i < 4 && t.getDL(i) != null; i++) {
            t.getDL(i).archetypeTag = (i % 2 == 0) ? Archetypes.DL_PASS_RUSHER : Archetypes.DL_RUN_STOPPER;
        }

        for (int i = 0; i < 3 && t.getLB(i) != null; i++) {
            t.getLB(i).archetypeTag = (i % 2 == 0) ? Archetypes.LB_COVERAGE : Archetypes.LB_BLITZER;
        }

        for (int i = 0; i < 3 && t.getCB(i) != null; i++) {
            t.getCB(i).archetypeTag = (i == 0) ? Archetypes.CB_SHUTDOWN : (i == 1 ? Archetypes.CB_SPEED : Archetypes.CB_PHYSICAL);
        }

        for (int i = 0; i < 2 && t.getS(i) != null; i++) {
            t.getS(i).archetypeTag = (i == 0) ? Archetypes.S_BALL_HAWK : Archetypes.S_RUN_SUPPORT;
        }

        t.getK(0).archetypeTag = Archetypes.K_POWER;
    }

    @Test
    public void gameWithArchetypes_completesSuccessfully() {
        assignAllArchetypes(homeTeam);
        assignAllArchetypes(awayTeam);
        league.fullGameLog = true;

        Game g = new Game(homeTeam, awayTeam, "Archetype Integration");
        g.playGame();

        assertTrue("Game should have been played", g.hasPlayed);
        assertTrue("Game should have home score >= 0", g.homeScore >= 0);
        assertTrue("Game should have away score >= 0", g.awayScore >= 0);

        String pbp = g.getPlayByPlayLog();
        assertNotNull("Play-by-play should not be null", pbp);
        assertFalse("Play-by-play should not be empty", pbp.isEmpty());
    }

    @Test
    public void gameWithArchetypes_logsContainExpectedContent() {
        assignAllArchetypes(homeTeam);
        assignAllArchetypes(awayTeam);
        league.fullGameLog = true;

        Game g = new Game(homeTeam, awayTeam, "Archetype Log Test");
        g.playGame();

        String log = g.gameEventLog.toString();
        assertTrue("Log should contain first down or scoring", log.contains("FIRST DOWN") || log.contains("TOUCHDOWN") || log.contains("field goal"));
        assertTrue("Log should contain play results", log.contains("caught") || log.contains("dropped") || log.contains("incomplete") || log.contains("rushed"));
    }

    @Test
    public void gameWithArchetypes_differentArchetypesProduceValidStats() {
        assignAllArchetypes(homeTeam);
        assignAllArchetypes(awayTeam);

        Game g = new Game(homeTeam, awayTeam, "Archetype Stats Test");
        g.playGame();

        assertNotNull("Passing stats should exist", g.homePassingStats);
        assertNotNull("Rushing stats should exist", g.homeRushingStats);
        assertNotNull("Receiving stats should exist", g.homeReceivingStats);

        assertEquals("Home pass + rush should equal total yards",
                g.homePassYards + g.homeRushYards, g.homeYards);
        assertEquals("Away pass + rush should equal total yards",
                g.awayPassYards + g.awayRushYards, g.awayYards);
    }
}
