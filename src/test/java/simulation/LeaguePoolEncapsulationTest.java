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

    @Test
    public void coachPoolGetters_areUnmodifiable() {
        try {
            league.getCoachList().clear();
            fail("getCoachList should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getCoachStarList().clear();
            fail("getCoachStarList should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getCoachFreeAgents().clear();
            fail("getCoachFreeAgents should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getCoachDatabase().clear();
            fail("getCoachDatabase should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void newsListGetters_areUnmodifiable_andAddNewsStoryWorks() {
        assertFalse(league.getNewsStories().isEmpty());
        try {
            league.getNewsStories().clear();
            fail("getNewsStories should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getNewsStories().get(0).clear();
            fail("news week list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getNewsHeadlines().clear();
            fail("getNewsHeadlines should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getWeeklyScores().clear();
            fail("getWeeklyScores should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getNewsTV().add("should fail");
            fail("getNewsTV should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        int before = league.getNewsStoriesForWeek(0).size();
        league.addNewsStory(0, "Encapsulation Test>Story body");
        assertTrue(league.getNewsStoriesForWeek(0).size() == before + 1);
        league.addNewsHeadline("Encapsulation headline");
        assertTrue(league.getNewsHeadlines().contains("Encapsulation headline"));
    }

    @Test
    public void historyAndHofGetters_areUnmodifiable() {
        try {
            league.getLeagueHistory().clear();
            fail("getLeagueHistory should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getHeismanHistory().clear();
            fail("getHeismanHistory should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getLeagueHoF().clear();
            fail("getLeagueHoF should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getFreshmen().add(null);
            fail("getFreshmen should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getRedshirts().add(null);
            fail("getRedshirts should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getPlayoffTeams().clear();
            fail("getPlayoffTeams should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
        try {
            league.getTeamsFCSList().clear();
            fail("getTeamsFCSList should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        int histBefore = league.getLeagueHistory().size();
        league.addLeagueHistory(new String[]{"encap-test"});
        assertTrue(league.getLeagueHistory().size() == histBefore + 1);
    }
}
