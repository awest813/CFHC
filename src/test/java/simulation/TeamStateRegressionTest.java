package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import positions.Player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TeamStateRegressionTest {
    private League league;
    private Team team;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void scheduleAndOocViews_areReadOnlyButMutatorsWork() {
        assertUnmodifiable(team.getGameSchedule());
        assertUnmodifiable(team.getOocTeams());
        assertUnmodifiable(team.getOocWeeks());

        Team opponent = league.getTeamList().get(1);
        Game game = new Game(team, opponent, "OOC");

        team.clearGameSchedule();
        team.clearOocTeams();
        team.clearOocWeeks();

        team.addGameToSchedule(game);
        team.addOocTeam(opponent);
        team.addOocWeek(4);

        assertEquals(1, team.getGameSchedule().size());
        assertSame(game, team.getGameSchedule().get(0));
        assertEquals(1, team.getOocTeams().size());
        assertSame(opponent, team.getOocTeams().get(0));
        assertEquals(1, team.getOocWeeks().size());
        assertEquals(4, (int) team.getOocWeeks().get(0));

        team.clearGameSchedule();
        team.clearOocTeams();
        team.clearOocWeeks();

        assertTrue(team.getGameSchedule().isEmpty());
        assertTrue(team.getOocTeams().isEmpty());
        assertTrue(team.getOocWeeks().isEmpty());
    }

    @Test
    public void curePlayers_clearsInjuryFlagsAndTrackedInjuryList() {
        Player player = team.getAllPlayers().get(0);
        player.injury = new Injury(2, "Audit injury", player);
        team.clearPlayersInjured();
        team.addPlayerInjured(player);

        assertTrue(player.isInjured);
        assertEquals(1, team.getPlayersInjured().size());

        team.curePlayers();

        assertFalse(player.isInjured);
        assertEquals(null, player.injury);
        assertEquals(0, team.getPlayersInjured().size());
    }

    @Test
    public void healSuspension_expiresOneWeekSuspension() {
        Player player = team.getAllPlayers().get(0);
        player.isSuspended = true;
        player.weeksSuspended = 1;

        team.updateSuspensions();

        assertFalse(player.isSuspended);
        assertEquals(0, player.weeksSuspended);
    }

    @Test
    public void coreTeamStatsRemainCallableFromSharedModel() {
        assertTrue(team.getOffTalent() > 0);
        assertTrue(team.getDefTalent() > 0);
        assertTrue(team.getCompositeFootIQ() > 0);
        assertTrue(team.getTeamChemistry() > 0);
        assertTrue(league.getAverageOffTalent() > 0);
        assertTrue(league.getAverageDefTalent() > 0);
    }

    private static <T> void assertUnmodifiable(List<T> view) {
        try {
            view.clear();
            fail("Expected view to be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            assertNotNull(expected);
        }
    }
}
