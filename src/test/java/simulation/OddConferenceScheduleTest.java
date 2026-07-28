package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Odd-sized conferences use {@link Conference#setUpEvenOddSchedule()} when
 * {@code regSeasonWeeks != 13}. BYE placeholder teams must not remain on the conference.
 */
public class OddConferenceScheduleTest {

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
        league.regSeasonWeeks = 15;
    }

    @Test
    public void oddConference_buildsScheduleWithoutByeTeamOnRoster() {
        Conference target = null;
        for (Conference c : league.conferences) {
            if (c.confTeams.size() % 2 != 0 && c.confTeams.size() >= c.minConfTeams) {
                target = c;
                break;
            }
        }
        if (target == null) {
            return;
        }

        for (Team t : league.teamList) {
            t.gameSchedule.clear();
        }

        target.setUpSchedule();

        for (Team t : target.confTeams) {
            assertFalse("BYE placeholder should not remain on conference roster",
                    "BYE".equals(t.getName()));
            assertFalse("OOC placeholder should not remain on conference roster",
                    "OOC1".equals(t.getName()) || "OOC2".equals(t.getName()));
            assertTrue("Team " + t.getName() + " should have conference games scheduled",
                    !t.getGameSchedule().isEmpty());
        }
    }
}
