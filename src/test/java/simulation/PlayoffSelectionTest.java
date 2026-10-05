package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Regression tests for the poll/playoff audit (Oct 2026). */
public class PlayoffSelectionTest {

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
                false, false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
    }

    private Conference firstPowerConference() {
        for (Conference c : league.getConferences()) {
            if (!c.confName.equals("Independent") && !c.confName.equals("FCS Division") && c.confTeams.size() > 4) {
                return c;
            }
        }
        throw new AssertionError("no conference");
    }

    @Test
    public void autoBid_goesToActualChampion_notConfWinsLeader() {
        Conference conf = firstPowerConference();
        for (Team t : conf.confTeams) t.setConfChampion("");
        // Crown the team at the bottom of the conference; projection by conf
        // wins would never pick it.
        Team champ = conf.confTeams.get(conf.confTeams.size() - 1);
        champ.setConfChampion("CC");
        // Selection happens in championship week, before currentWeek passes regSeasonWeeks.
        league.currentWeek = league.regSeasonWeeks - 1;

        ArrayList<Team> qualified = new ArrayList<>(league.getTeamList());
        // Make the champion the top-ranked leader so it must land in the top five.
        champ.setTeamPollScore(1_000_000f);
        ArrayList<Team> bids = league.bowlManager.getExpandedPlayoffAutoBids(qualified);

        assertTrue("actual champion gets the auto-bid", bids.contains(champ));
        for (Team t : conf.confTeams) {
            if (t != champ) assertFalse(t.getName() + " is not the champion", bids.contains(t));
        }
        assertTrue(bids.size() <= BowlManager.EXPANDED_PLAYOFF_AUTO_BIDS);
    }

    @Test
    public void autoBid_fallsBackToProjectionBeforeTitleGames() {
        for (Team t : league.getTeamList()) t.setConfChampion("");
        league.currentWeek = 5;
        ArrayList<Team> bids = league.bowlManager.getExpandedPlayoffAutoBids(new ArrayList<>(league.getTeamList()));
        assertEquals(BowlManager.EXPANDED_PLAYOFF_AUTO_BIDS, bids.size());
    }

    @Test
    public void bowlWatchPreview_isReadOnly_in12TeamMode() {
        league.expPlayoffs = true;
        league.currentWeek = 6;
        assertFalse(league.hasScheduledBowls);

        String preview = league.getBowlGameWatchStr();

        assertTrue(preview, preview.startsWith("Projected College Football Playoff field"));
        assertFalse("preview must not schedule bowls", league.hasScheduledBowls);
        for (Game g : league.bowlGames) assertNull("no bowl game created by a preview", g);
        assertTrue("preview must not set the real field", league.playoffTeams.isEmpty());
    }

    @Test
    public void setTeamRanks_isStableAcrossRepeatedCalls() {
        league.setTeamRanks();
        Map<String, Integer> first = new HashMap<>();
        for (Team t : league.getTeamList()) first.put(t.getName(), t.getRankTeamPollScore());
        league.setTeamRanks();
        for (Team t : league.getTeamList()) {
            assertEquals(t.getName(), first.get(t.getName()).intValue(), t.getRankTeamPollScore());
        }
    }

    @Test
    public void expandedField_isTwelveDistinctTeamsSeededByPoll() {
        league.currentWeek = league.regSeasonWeeks - 1;
        ArrayList<Team> qualified = league.bowlManager.getQualifiedTeams();
        ArrayList<Team> field = league.bowlManager.selectExpPlayoffField(qualified);
        assertEquals(League.EXPANDED_PLAYOFF_TEAM_COUNT, field.size());
        assertEquals(field.size(), new java.util.HashSet<>(field).size());
        for (int i = 1; i < field.size(); i++) {
            assertTrue("seeded by poll score",
                    field.get(i - 1).getTeamPollScore() >= field.get(i).getTeamPollScore());
        }
    }

    @Test
    public void bowlWatch_afterSelection_showsBracketAndBowls() {
        league.expPlayoffs = true;
        league.currentWeek = league.regSeasonWeeks - 1;
        // Bowl eligibility needs 6+ wins; a fresh league has played no games.
        for (Team t : league.getTeamList()) t.wins = 7;
        league.scheduleExpPlayoff();
        assertTrue(league.hasScheduledBowls);
        String watch = league.getBowlGameWatchStr();
        assertTrue(watch, watch.contains("First Round:"));
        assertTrue(watch, watch.contains("(5 v 12)"));
        assertTrue(watch, watch.contains("First-round byes: #1 seed"));
        assertTrue(watch, watch.contains("BOWL GAMES"));
    }
}
