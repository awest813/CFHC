package desktop;

import org.junit.Before;
import org.junit.Test;
import positions.Player;
import simulation.League;
import simulation.PlatformResourceProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DisciplineDialogTest {

    private League league;
    private Player player;

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
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        player = league.userTeam.teamQBs.get(0);
    }

    @Test
    public void applyChoice_shortSuspensionClearsFlagAndSuspends() {
        league.userTeam.disciplineAction = true;
        assertTrue(DisciplineDialog.applyChoice(league.userTeam, player, "conduct", 1, 3, 0));
        assertFalse(league.userTeam.disciplineAction);
        assertTrue(player.isSuspended);
    }

    @Test
    public void applyChoice_ignoreOrCancelClearsFlagWithoutSuspendPathChoice2() {
        league.userTeam.disciplineAction = true;
        player.isSuspended = false;
        assertTrue(DisciplineDialog.applyChoice(league.userTeam, player, "conduct", 1, 3, 2));
        assertFalse(league.userTeam.disciplineAction);

        league.userTeam.disciplineAction = true;
        assertTrue(DisciplineDialog.applyChoice(league.userTeam, player, "conduct", 1, 3, -1));
        assertFalse(league.userTeam.disciplineAction);
    }

    @Test
    public void applyChoice_nullGuards() {
        assertFalse(DisciplineDialog.applyChoice(null, player, "conduct", 1, 3, 0));
        assertFalse(DisciplineDialog.applyChoice(league.userTeam, null, "conduct", 1, 3, 0));
    }
}
