package desktop;

import org.junit.Test;
import simulation.FileSystemResourceProvider;
import simulation.Game;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;

import static org.junit.Assert.assertEquals;

public class CoachDecisionDialogTest {

    @Test
    public void schemeOptionsNameTheSchemeTheySelect() {
        FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        Team user = league.getTeamList().get(0);
        user.setUserControlled(true);
        user.setPlaybookOffNum(3);
        user.setPlaybookDefNum(0);
        Game game = new Game(user, league.getTeamList().get(1), "Test");

        String[] off = CoachDecisionDialog.planOffNames(game);
        assertEquals(Team.OFFENSE_PLAYBOOKS + 1, off.length);
        assertEquals("Keep current (Air Raid)", off[0]);
        for (int i = 0; i < Team.OFFENSE_PLAYBOOKS; i++) {
            // Option i + 1 becomes plan.offScheme = i.
            assertEquals(PlaybookOffense.forIndex(i).getStratName(), off[i + 1]);
        }
        assertEquals("Multiple Pro", off[1]);
        assertEquals("Spread RPO", off[6]);

        String[] def = CoachDecisionDialog.planDefNames(game);
        assertEquals(Team.DEFENSE_PLAYBOOKS + 1, def.length);
        assertEquals("Keep current (Multiple 4-2-5)", def[0]);
        for (int i = 0; i < Team.DEFENSE_PLAYBOOKS; i++) {
            assertEquals(PlaybookDefense.forIndex(i).getStratName(), def[i + 1]);
        }
    }
}
