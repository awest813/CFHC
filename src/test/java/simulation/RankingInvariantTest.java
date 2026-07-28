package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

import static org.junit.Assert.*;

/**
 * Ranking invariants for {@link League#setTeamRanks()} — unique 1..N ranks across
 * poll, prestige, and other leaderboards. Covers the cleanup-audit gap for
 * explicit ranking checks before and after games are played.
 */
public class RankingInvariantTest {

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
    public void rankings_beforeAnyGames_areUniqueOneThroughN() {
        league.setTeamRanks();
        assertUniqueRanksInRange("poll", Team::getRankTeamPollScore);
        assertUniqueRanksInRange("prestige", t -> t.rankTeamPrestige);
        assertUniqueRanksInRange("off talent", t -> t.rankTeamOffTalent);
        assertUniqueRanksInRange("def talent", t -> t.rankTeamDefTalent);
    }

    @Test
    public void rankings_afterMidseason_remainUniqueOneThroughN() {
        controller.advanceWeek(); // preseason
        int half = Math.max(1, league.regSeasonWeeks / 2);
        for (int w = 0; w < half; w++) {
            controller.advanceWeek();
        }
        league.setTeamRanks();
        assertUniqueRanksInRange("poll", Team::getRankTeamPollScore);
        assertUniqueRanksInRange("prestige", t -> t.rankTeamPrestige);
        assertUniqueRanksInRange("PPG", t -> t.rankTeamPoints);
        assertUniqueRanksInRange("YPG", t -> t.rankTeamYards);
        assertUniqueRanksInRange("RPI", t -> t.rankTeamRPI);
    }

    @Test
    public void projectedPollRanks_afterBenchmarks_areUniqueOneThroughN() {
        league.setTeamBenchMarks();
        assertUniqueRanksInRange("projected poll", t -> t.projectedPollRank);
    }

    private void assertUniqueRanksInRange(String label, ToIntFunction<Team> rankFn) {
        List<Team> teams = league.getTeamList();
        int n = teams.size();
        Set<Integer> seen = new HashSet<>();
        for (Team t : teams) {
            int rank = rankFn.applyAsInt(t);
            assertTrue(label + " rank for " + t.name + " must be in 1.." + n + ", was " + rank,
                    rank >= 1 && rank <= n);
            assertTrue(label + " rank " + rank + " duplicated (" + t.name + ")",
                    seen.add(rank));
        }
        assertEquals(label + " should cover every rank 1.." + n, n, seen.size());
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
