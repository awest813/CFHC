package staff;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;
import static org.junit.Assert.*;

public class StaffTest {

    private Team team;

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
        team = league.getTeamList().get(0);
    }

    @Test
    public void staffConstructor_nonZeroRandomReduction() {
        // Test that ratOff and ratDef can be reduced by more than 0
        // Previously (int)Math.random()*25 was always 0
        boolean foundNonZeroReduction = false;
        for (int i = 0; i < 1000; i++) {
            Staff oc = new OC("Test", 5, 0, team);
            if (oc.ratDef < 50 + 5 * 5 - 15) {
                foundNonZeroReduction = true;
                break;
            }
        }
        assertTrue("OC ratDef should sometimes be reduced by random amount", foundNonZeroReduction);

        foundNonZeroReduction = false;
        for (int i = 0; i < 1000; i++) {
            Staff dc = new DC("Test", 5, 0, team);
            if (dc.ratOff < 50 + 5 * 5 - 15) {
                foundNonZeroReduction = true;
                break;
            }
        }
        assertTrue("DC ratOff should sometimes be reduced by random amount", foundNonZeroReduction);
    }
}
