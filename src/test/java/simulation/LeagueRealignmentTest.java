package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LeagueRealignmentTest {

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
        league.newsRealignment = "stale headline";
        league.countRealignment = 99;
    }

    @Test
    public void runOffseasonRealignment_clearsStaleNewsWhenDisabled() {
        league.confRealignment = false;
        league.enableUnivProRel = false;

        league.runOffseasonRealignment(GameUiBridge.NO_OP);

        assertEquals("", league.newsRealignment);
        assertEquals(0, league.countRealignment);
    }

    @Test
    public void offseasonWeek_runsRealignmentAndRequestsSummaryDialog() {
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        league.confRealignment = true;

        final boolean[] summaryShown = {false};
        SeasonController controller = new SeasonController(league, new GameUiBridge() {
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
            @Override public void showRealignmentSummary() { summaryShown[0] = true; }
            @Override public void startRecruitingFlow() {}
        });

        league.currentWeek = league.regSeasonWeeks + 12;
        SeasonAdvanceResult result = controller.advanceWeek();

        assertTrue(summaryShown[0]);
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
        assertEquals(league.regSeasonWeeks + 13, result.weekAfter);
    }
}
