package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Regression: composite ratings must tolerate depleted position groups.
 */
public class StatsTrackerDepthSafetyTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources =
                new FileSystemResourceProvider(System.getProperty("user.dir"));
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
    public void composites_surviveClearedSkillGroups() {
        team.teamQBs.clear();
        team.teamOLs.clear();
        team.teamDLs.clear();
        team.teamWRs.clear();
        team.teamRBs.clear();
        team.teamCBs.clear();
        team.teamLBs.clear();
        team.teamSs.clear();
        team.teamTEs.clear();
        team.teamKs.clear();

        float iq = team.statsTracker.getCompositeFootIQ();
        float pass = team.statsTracker.getPassProf();
        float rush = team.statsTracker.getRushProf();
        float passDef = team.statsTracker.getPassDef();
        float rushDef = team.statsTracker.getRushDef();
        float olPass = team.statsTracker.getCompositeOLPass();
        float olRush = team.statsTracker.getCompositeOLRush();
        float dlPass = team.statsTracker.getCompositeDLPass();
        float dlRush = team.statsTracker.getCompositeDLRush();

        assertFalse(Float.isNaN(iq));
        assertFalse(Float.isNaN(pass));
        assertFalse(Float.isNaN(rush));
        assertFalse(Float.isNaN(passDef));
        assertFalse(Float.isNaN(rushDef));
        assertFalse(Float.isNaN(olPass));
        assertFalse(Float.isNaN(olRush));
        assertFalse(Float.isNaN(dlPass));
        assertFalse(Float.isNaN(dlRush));
        assertTrue(iq >= 0);
    }

    @Test
    public void injuredListAccessors_areEncapsulated() {
        assertTrue(team.getPlayersInjured().isEmpty() || team.getPlayersInjured() != null);
        team.clearPlayersInjured();
        assertTrue(team.getPlayersInjured().isEmpty());
        assertTrue(team.getRedshirtList() != null);
        assertTrue(team.getTransferringPlayers() != null);
    }
}
