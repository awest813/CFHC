package desktop;

import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

import javax.swing.JTextField;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlayerSearchPanelTest {

    private League league;
    private LeagueScreenContext ctx;

    @Before
    public void setUp() throws Exception {
        DesktopTheme.load();
        PlatformResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        Map<String, Team> teamMap = new HashMap<>();
        for (Team t : league.getTeamList()) {
            teamMap.put(t.getName(), t);
        }
        ctx = new LeagueScreenContext(league, league.toRecord(), teamMap,
                null, null, null, null);
    }

    @Test
    public void searchTarget_returnsNameFieldAfterBuild() {
        PlayerSearchPanel panel = new PlayerSearchPanel();
        panel.build(ctx);

        assertNotNull(panel.searchTarget());
        assertTrue(panel.searchTarget() instanceof JTextField);
    }
}
