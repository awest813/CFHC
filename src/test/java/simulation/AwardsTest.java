package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import positions.Player;

import static org.junit.Assert.*;

public class AwardsTest {

    private League league;
    private SeasonController controller;
    private int awardsSummaryCount;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);

        awardsSummaryCount = 0;

        controller = new SeasonController(league, new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(Player player, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String summaryText) {
                awardsSummaryCount++;
            }
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        });
    }

    @Test
    public void awards_heismanCeremonyStr_returnsNonEmpty() {
        // Advance through full season to awards stage
        int reg = league.regSeasonWeeks;
        // Preseason
        controller.advanceWeek();
        // Full regular season + postseason weeks
        for (int w = 0; w < reg + 3; w++) {
            controller.advanceWeek();
        }

        String awards = league.getHeismanCeremonyStr();
        assertNotNull("Awards ceremony string should not be null", awards);
        assertTrue("Awards ceremony should have content", awards.length() > 0);
    }

    @Test
    public void awards_summaryIsShownDuringSeasonCompletePhase() {
        int reg = league.regSeasonWeeks;
        controller.advanceWeek(); // preseason
        for (int w = 0; w < reg + 3; w++) {
            controller.advanceWeek(); // regular season + postseason
        }

        // Offseason: award summary should be shown
        for (int w = 0; w < 2; w++) {
            controller.advanceWeek();
        }

        assertTrue("Awards summary should have been shown at least once",
                awardsSummaryCount >= 1);
    }

    @Test
    public void awards_heismanWinnerHasValidAwardDescription() {
        int reg = league.regSeasonWeeks;
        controller.advanceWeek();
        for (int w = 0; w < reg + 3; w++) {
            controller.advanceWeek();
        }

        String awards = league.getHeismanCeremonyStr();
        assertNotNull(awards);
        assertTrue("Awards should mention Offensive Player of the Year",
                awards.contains("Offensive Player of the Year")
                        || awards.contains("Heisman")
                        || awards.contains("Player of the Year"));
    }

    @Test
    public void awards_awardsPersistThroughAdvanceSeason() {
        int reg = league.regSeasonWeeks;
        controller.advanceWeek();
        for (int w = 0; w < reg + 3; w++) {
            controller.advanceWeek();
        }
        // Collect awards string
        String beforeAdvance = league.getHeismanCeremonyStr();
        assertNotNull(beforeAdvance);

        // Advance season
        league.advanceSeason();
        league.startNextSeason();

        // Awards should remain accessible
        String afterAdvance = league.getHeismanCeremonyStr();
        assertNotNull("Awards should persist after season advance", afterAdvance);
    }

    @Test
    public void awards_playerAwardsAreAccessible() {
        String awards = league.getHeismanCeremonyStr();
        assertNotNull(awards);
    }
}
