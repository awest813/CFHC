package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Season-loop edge invariants from the cleanup audit: short/padded schedules and overtime.
 */
public class SeasonEdgeInvariantTest {

    private League league;

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
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
    }

    @Test
    public void schedules_arePaddedToRegularSeasonLength() {
        int target = league.regSeasonWeeks - 1;
        assertTrue("regSeasonWeeks should be > 1", target > 0);

        for (Team t : league.getTeamList()) {
            assertTrue(
                    "Team " + t.name + " schedule size " + t.getGameSchedule().size()
                            + " should be at least " + target,
                    t.getGameSchedule().size() >= target);
        }
    }

    @Test
    public void shortSchedules_areNormalizedWithByeWeeksWhenOddConference() {
        // Force odd-conference scheduling path used when regSeasonWeeks != 13.
        league.regSeasonWeeks = 15;
        for (Team t : league.getTeamList()) {
            t.clearGameSchedule();
        }

        Conference odd = null;
        for (Conference c : league.conferences) {
            if (c.confTeams.size() % 2 != 0 && c.confTeams.size() >= c.minConfTeams) {
                odd = c;
                break;
            }
        }
        assumeTrue("Need an odd-sized conference in the default universe", odd != null);

        odd.setUpSchedule();

        boolean sawBye = false;
        for (Team t : odd.confTeams) {
            assertFalse("BYE placeholder must not stay on roster", "BYE".equals(t.getName()));
            for (Game g : t.getGameSchedule()) {
                if (g.isByeWeek()) {
                    sawBye = true;
                }
            }
        }
        assertTrue("Odd conference schedule should include at least one BYE WEEK slot", sawBye);
    }

    @Test
    public void overtime_producesWinnerAndRecordsOtPeriods() {
        Team home = league.getTeamList().get(0);
        Team away = league.getTeamList().get(1);

        Game otGame = null;
        for (int attempt = 0; attempt < 250; attempt++) {
            Game g = new Game(home, away, "OT Edge Test");
            g.playGame();
            if (g.numOT > 0) {
                otGame = g;
                break;
            }
        }
        assumeTrue("Could not force an OT game in 250 tries (random sim)", otGame != null);

        assertTrue(otGame.hasPlayed);
        assertTrue("OT game should not remain tied", otGame.homeScore != otGame.awayScore);
        assertTrue(otGame.numOT >= 1);
        int homeOtPoints = 0;
        int awayOtPoints = 0;
        for (int i = 4; i < 4 + otGame.numOT && i < otGame.homeQScore.length; i++) {
            homeOtPoints += otGame.homeQScore[i];
            awayOtPoints += otGame.awayQScore[i];
        }
        assertTrue("OT periods should contribute points for at least one team",
                homeOtPoints + awayOtPoints > 0);
    }

    @Test
    public void playedGames_neverHaveNegativeScores() {
        Team home = league.getTeamList().get(0);
        Team away = league.getTeamList().get(1);
        for (int i = 0; i < 20; i++) {
            Game g = new Game(home, away, "Score Bounds " + i);
            g.playGame();
            assertTrue(g.hasPlayed);
            assertTrue(g.homeScore >= 0);
            assertTrue(g.awayScore >= 0);
            assertFalse(g.isByeWeek());
        }
    }
}
