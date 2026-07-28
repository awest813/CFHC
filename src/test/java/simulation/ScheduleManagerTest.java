package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards the ScheduleManager extraction from League.setupSeason().
 */
public class ScheduleManagerTest {

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
    public void newLeague_hasRegularSeasonSchedulesFromScheduleManager() {
        assertFalse(league.getTeamList().isEmpty());
        assertTrue(league.teamsFCSList != null);
        int target = league.regSeasonWeeks - 1;
        for (Team t : league.getTeamList()) {
            assertTrue(t.getName() + " schedule too short: " + t.getGameSchedule().size(),
                    t.getGameSchedule().size() >= target);
        }
    }
}
