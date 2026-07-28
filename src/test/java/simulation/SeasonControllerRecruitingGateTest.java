package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeasonControllerRecruitingGateTest {

    private League league;
    private SeasonController controller;
    private final AtomicInteger recruitingStarts = new AtomicInteger();

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
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        recruitingStarts.set(0);

        controller = new SeasonController(league, new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(File f, Team t) {}
            @Override public void transferPlayer(positions.Player p) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player p, String i, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {
                recruitingStarts.incrementAndGet();
                league.recruitPlayers();
            }
        });
    }

    @Test
    public void recruitingWeek_firesOnceUntilNextSeason() {
        league.currentWeek = league.regSeasonWeeks + 13;

        SeasonAdvanceResult first = controller.advanceWeek();
        assertTrue(first.hasEvent(SeasonAdvanceResult.EventType.RECRUITING_STARTED));
        assertTrue(league.recruitingPhaseActive);
        assertEquals(1, recruitingStarts.get());

        SeasonAdvanceResult second = controller.advanceWeek();
        assertFalse(second.hasEvent(SeasonAdvanceResult.EventType.RECRUITING_STARTED));
        assertTrue(second.hasEvent(SeasonAdvanceResult.EventType.AWAITING_RECRUITING));
        assertEquals(1, recruitingStarts.get());
        assertEquals(league.regSeasonWeeks + 13, league.currentWeek);

        league.startNextSeason();
        assertFalse(league.recruitingPhaseActive);
        assertEquals(0, league.currentWeek);
    }
}
