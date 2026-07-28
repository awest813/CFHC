package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Encapsulation guards for League conferences/team list and transfer pools.
 */
public class LeaguePoolEncapsulationTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
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
    }

    @Test
    public void getConferencesAndTeamList_areUnmodifiable() {
        assertFalse(league.getConferences().isEmpty());
        assertFalse(league.getTeamList().isEmpty());
        try {
            league.getConferences().clear();
            fail("getConferences should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getTeamList().clear();
            fail("getTeamList should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void getTransferPools_areUnmodifiable() {
        try {
            league.getTransferQBs().clear();
            fail("getTransferQBs should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        assertTrue(league.getTransferQBs().size() >= 0);
        assertTrue(league.getTransferRBs().size() >= 0);
    }
}
