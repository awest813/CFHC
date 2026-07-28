package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Program prestige bounds across construction, knockdown, and offseason
 * prestige recalculation. Complements ranking invariants for the career loop.
 */
public class PrestigeBoundsTest {

    /** Soft ceiling — prestige grows slowly; values above this signal corruption. */
    private static final int PRESTIGE_SOFT_MAX = 200;

    private League league;
    private SeasonController controller;

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
        assertFalse(league.getTeamList().isEmpty());
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        controller = new SeasonController(league, noOpBridge());
    }

    @Test
    public void prestige_atLeagueConstruction_isNonNegativeAndBelowSoftMax() {
        assertAllTeamsInBounds("after construction");
    }

    @Test
    public void prestige_afterCalcSeasonPrestige_clampsAtZero() {
        Team t = league.userTeam;
        t.teamPrestige = 0;
        t.projectedPollRank = league.getTeamList().size();
        t.rankTeamPollScore = 1; // huge underperformance vs projection
        t.wins = 0;
        t.projectedWins = 10;
        t.bowlBan = false;
        t.penalized = false;

        int[] pts = t.calcSeasonPrestige();
        assertTrue("calcSeasonPrestige must not return negative prestige", pts[0] >= 0);
        t.enterOffSeason();
        assertTrue("enterOffSeason prestige must be >= 0", t.teamPrestige >= 0);
    }

    @Test
    public void prestige_fireKnockdown_staysNonNegative() {
        Team t = league.getTeamList().get(1);
        int before = t.teamPrestige;
        assertTrue(before >= 0);
        t.teamPrestige = (int) (t.teamPrestige * Team.knockdownFired);
        assertTrue("knockdownFired prestige must stay >= 0", t.teamPrestige >= 0);
        assertTrue("knockdownFired should not increase prestige", t.teamPrestige <= before);
    }

    @Test
    public void prestige_afterMidseason_remainsInBounds() {
        controller.advanceWeek();
        for (int w = 0; w < Math.max(1, league.regSeasonWeeks / 2); w++) {
            controller.advanceWeek();
        }
        assertAllTeamsInBounds("after midseason");
    }

    @Test
    public void prestige_rankMatchesOrdering_afterSetTeamRanks() {
        league.setTeamRanks();
        for (Team a : league.getTeamList()) {
            for (Team b : league.getTeamList()) {
                if (a.teamPrestige > b.teamPrestige) {
                    assertTrue(
                            a.name + " (prs " + a.teamPrestige + ", rank " + a.rankTeamPrestige
                                    + ") should rank ahead of " + b.name
                                    + " (prs " + b.teamPrestige + ", rank " + b.rankTeamPrestige + ")",
                            a.rankTeamPrestige < b.rankTeamPrestige);
                }
            }
        }
    }

    private void assertAllTeamsInBounds(String when) {
        for (Team t : league.getTeamList()) {
            assertTrue(when + ": " + t.name + " prestige >= 0, was " + t.teamPrestige,
                    t.teamPrestige >= 0);
            assertTrue(when + ": " + t.name + " prestige < " + PRESTIGE_SOFT_MAX
                            + ", was " + t.teamPrestige,
                    t.teamPrestige < PRESTIGE_SOFT_MAX);
        }
    }

    private static GameUiBridge noOpBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {}
            @Override public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {}
            @Override public void showNotification(String title, String message) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String summaryText) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        };
    }
}
