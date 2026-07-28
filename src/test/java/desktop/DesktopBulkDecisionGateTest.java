package desktop;

import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.SeasonAdvanceResult;
import simulation.SeasonController;
import simulation.SeasonFlowOrder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Career decision callbacks must still be reachable when informational UI is suppressed
 * (bulk advance). Subclass hooks prove the controller still invokes decision methods.
 */
public class DesktopBulkDecisionGateTest {

    private League league;

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
    }

    @Test
    public void suppressBlockingUi_stillInvokesContractDecisionHook() {
        AtomicInteger contracts = new AtomicInteger();
        DesktopUiBridge bridge = new DesktopUiBridge(null, league) {
            @Override public void showSeasonSummary() { /* skip NCG-dependent summary text */ }
            @Override public void showContractDialog() { contracts.incrementAndGet(); }
        };
        bridge.setSuppressBlockingUi(true);
        SeasonController controller = new SeasonController(league, bridge);

        // Jump straight to the contracts offseason step (reg+5).
        league.currentWeek = SeasonFlowOrder.firstOffseasonWeek(league.regSeasonWeeks) + 1;
        SeasonAdvanceResult result = controller.advanceWeek();
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
        assertEquals(1, contracts.get());
    }

    @Test
    public void offseasonDialogOrder_emitsNeedsDialogForSummaryThenContract() {
        AtomicInteger summaries = new AtomicInteger();
        AtomicInteger contracts = new AtomicInteger();
        DesktopUiBridge bridge = new DesktopUiBridge(null, league) {
            @Override public void showSeasonSummary() { summaries.incrementAndGet(); }
            @Override public void showContractDialog() { contracts.incrementAndGet(); }
        };
        bridge.setSuppressBlockingUi(true);
        SeasonController controller = new SeasonController(league, bridge);

        league.currentWeek = SeasonFlowOrder.firstOffseasonWeek(league.regSeasonWeeks);
        SeasonAdvanceResult summary = controller.advanceWeek();
        assertTrue(summary.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
        assertEquals(1, summaries.get());

        SeasonAdvanceResult contract = controller.advanceWeek();
        assertTrue(contract.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
        assertEquals(1, contracts.get());
    }
}
