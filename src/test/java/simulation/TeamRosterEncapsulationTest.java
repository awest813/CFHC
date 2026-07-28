package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.PlayerQB;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Encapsulation guards for Team position rosters and schedule lists.
 */
public class TeamRosterEncapsulationTest {

    private Team team;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
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
    public void getTeamQBs_isUnmodifiable() {
        List<PlayerQB> qbs = team.getTeamQBs();
        assertFalse(qbs.isEmpty());
        try {
            qbs.clear();
            fail("getTeamQBs should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        assertFalse(team.getTeamQBs().isEmpty());
    }

    @Test
    public void clearAllRosters_emptiesPositionLists() {
        assertFalse(team.getTeamQBs().isEmpty());
        team.clearAllRosters();
        assertTrue(team.getTeamQBs().isEmpty());
        assertTrue(team.getTeamRBs().isEmpty());
        assertTrue(team.getTeamSs().isEmpty());
    }

    @Test
    public void getGameSchedule_isUnmodifiableAndClearWorks() {
        assertFalse(team.getGameSchedule().isEmpty());
        try {
            team.getGameSchedule().clear();
            fail("getGameSchedule should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        team.clearGameSchedule();
        assertTrue(team.getGameSchedule().isEmpty());
    }
}
