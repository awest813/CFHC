package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SeasonControllerResultTest {
    private League league;
    private SeasonController controller;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
        controller = new SeasonController(league, noOpBridge(), SimulationFacade.NO_OP_FLOW_MANAGER);
    }

    @Test
    public void advanceWeek_returnsPreseasonStatusAndRefreshEvent() {
        SeasonAdvanceResult result = controller.advanceWeek();

        assertEquals(0, result.weekBefore);
        assertEquals(1, result.weekAfter);
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.WEEK_ADVANCED));
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.REFRESH_REQUESTED));
        SeasonAdvanceResult.Event status = result.lastStatusEvent();
        assertNotNull(status);
        assertEquals("Preseason", status.statusText);
        assertEquals("Play Week 1", status.buttonText);
        assertTrue(status.majorEvent);
    }

    @Test
    public void recruitingBoundary_returnsRecruitingStartedEvent() {
        league.currentWeek = league.regSeasonWeeks + 13;

        SeasonAdvanceResult result = controller.advanceWeek();

        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.NOTIFICATION));
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.RECRUITING_STARTED));
    }

    @Test
    public void facadeAdvanceWeek_exposesStructuredSeasonResult() {
        SimulationFacade facade = new SimulationFacade(new File(System.getProperty("java.io.tmpdir")), new DesktopResourceProvider(System.getProperty("user.dir")));
        facade.setLeague(league, league.userTeam, league.userTeam);

        SeasonAdvanceResult result = facade.advanceWeek();

        assertEquals(0, result.weekBefore);
        assertEquals(1, result.weekAfter);
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.STATUS_UPDATED));
    }

    private static GameUiBridge noOpBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(File saveFile, Team userTeam) {}
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
