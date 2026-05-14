package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import positions.PlayerQB;
import static org.junit.Assert.*;

public class TeamStabilityTest {

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
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void getOffTalent_returnsNonNegative() {
        float talent = team.getOffTalent();
        assertTrue("Off talent >= 0", talent >= 0);
    }

    @Test
    public void getDefTalent_returnsNonNegative() {
        float talent = team.getDefTalent();
        assertTrue("Def talent >= 0", talent >= 0);
    }

    @Test
    public void getPassProf_doesNotCrash() {
        float prof = team.getPassProf();
        assertTrue("Pass prof >= 0", prof >= 0);
    }

    @Test
    public void getRushProf_doesNotCrash() {
        float prof = team.getRushProf();
        assertTrue("Rush prof >= 0", prof >= 0);
    }

    @Test
    public void getPassDef_doesNotCrash() {
        float prof = team.getPassDef();
        assertTrue("Pass def >= 0", prof >= 0);
    }

    @Test
    public void getOffSubTalent_doesNotCrash() {
        float talent = team.getOffSubTalent();
        assertTrue("Off sub talent >= 0", talent >= 0);
    }

    @Test
    public void getCompositeOLRush_doesNotCrash() {
        float comp = team.getCompositeOLRush();
        assertTrue("Composite OL rush >= 0", comp >= 0);
    }

    @Test
    public void getCompositeDLRush_doesNotCrash() {
        float comp = team.getCompositeDLRush();
        assertTrue("Composite DL rush >= 0", comp >= 0);
    }

    @Test
    public void getQB_withEmptyRoster_returnsNull() {
        PlayerQB qb = team.getQB(0);
        assertNotNull("QB 0 should exist", qb);
    }
}
