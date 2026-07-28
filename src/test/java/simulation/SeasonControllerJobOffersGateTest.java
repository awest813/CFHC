package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeasonControllerJobOffersGateTest {

    private League league;
    private SeasonController controller;
    private final boolean[] jobOffersShown = {false};

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
        league.careerMode = true;

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
            @Override public void showJobOffersDialog() { jobOffersShown[0] = true; }
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        });
    }

    @Test
    public void jobOffersWeek_skipsDialogWhenCoachNotFired() {
        league.currentWeek = league.regSeasonWeeks + 6;
        league.userTeam.fired = false;

        SeasonAdvanceResult result = controller.advanceWeek();

        assertFalse(jobOffersShown[0]);
        assertFalse(result.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
    }

    @Test
    public void jobOffersWeek_showsDialogWhenCoachFired() {
        league.currentWeek = league.regSeasonWeeks + 6;
        league.userTeam.fired = true;

        SeasonAdvanceResult result = controller.advanceWeek();

        assertTrue(jobOffersShown[0]);
        assertTrue(result.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
    }
}
