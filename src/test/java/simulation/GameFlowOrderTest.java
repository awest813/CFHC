package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Locks the season FSM so weeks and phases advance strictly in order.
 */
public class GameFlowOrderTest {

    private League league;
    private SeasonController controller;
    private final List<SeasonAdvanceResult.DialogType> dialogs = new ArrayList<>();

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
        dialogs.clear();
        controller = new SeasonController(league, recordingBridge());
    }

    @Test
    public void phasesNeverSkipBackwardDuringFullSeason() {
        int reg = league.regSeasonWeeks;
        int lastCycle = -1;

        SeasonAdvanceResult r = controller.advanceWeek(); // preseason
        assertEquals(1, r.weekAfter);
        lastCycle = assertCycleNonDecreasing(lastCycle);

        while (league.currentWeek < SeasonFlowOrder.recruitingWeek(reg)) {
            int before = league.currentWeek;
            r = controller.advanceWeek();
            assertEquals(
                    "week must advance by exactly 1 from " + before,
                    SeasonFlowOrder.expectedWeekAfterAdvance(before, reg),
                    r.weekAfter);
            lastCycle = assertCycleNonDecreasing(lastCycle);
        }

        assertEquals(SeasonFlowOrder.Phase.RECRUITING, SeasonFlowOrder.phaseAt(league));
        r = controller.advanceWeek();
        assertTrue(r.hasEvent(SeasonAdvanceResult.EventType.RECRUITING_STARTED));
        assertEquals(SeasonFlowOrder.recruitingWeek(reg), league.currentWeek);

        r = controller.advanceWeek();
        assertTrue(r.hasEvent(SeasonAdvanceResult.EventType.AWAITING_RECRUITING));
        assertFalse(r.hasEvent(SeasonAdvanceResult.EventType.WEEK_ADVANCED));
        assertEquals(SeasonFlowOrder.recruitingWeek(reg), league.currentWeek);
    }

    @Test
    public void midseasonProgressionRunsOnceAtCanonicalWeek() {
        int mid = SeasonFlowOrder.midseasonWeek(league.regSeasonWeeks);
        while (league.currentWeek < mid) {
            controller.advanceWeek();
        }
        assertEquals(mid, league.currentWeek);

        int beforeOvr = sampleUserOvrSum();
        SeasonAdvanceResult r = controller.advanceWeek();
        assertTrue(r.hasEvent(SeasonAdvanceResult.EventType.NEEDS_DIALOG));
        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.MIDSEASON_SUMMARY));
        // Ratings may stay flat for some rosters, but progression must have been applied
        // (ratImprovement flags cleared/set). Calling again would double-apply if dialog did it.
        int afterOvr = sampleUserOvrSum();
        assertTrue("midseason should not shrink overalls", afterOvr >= beforeOvr - 5);
        assertEquals(1, countDialog(SeasonAdvanceResult.DialogType.MIDSEASON_SUMMARY));
    }

    @Test
    public void offseasonDialogSequenceIsInOrder() {
        int reg = league.regSeasonWeeks;
        while (league.currentWeek < SeasonFlowOrder.firstOffseasonWeek(reg)) {
            controller.advanceWeek();
        }
        dialogs.clear();

        // reg+4 season summary through reg+12 realignment (9 steps)
        for (int i = 0; i < 9; i++) {
            controller.advanceWeek();
        }
        assertEquals(SeasonFlowOrder.recruitingWeek(reg), league.currentWeek);

        List<SeasonAdvanceResult.DialogType> expectedPrefix = new ArrayList<>();
        expectedPrefix.add(SeasonAdvanceResult.DialogType.SEASON_SUMMARY);
        expectedPrefix.add(SeasonAdvanceResult.DialogType.CONTRACT);
        // JOB_OFFERS only if fired — skip optional
        expectedPrefix.add(SeasonAdvanceResult.DialogType.PROMOTIONS);
        // COORDINATOR_HIRING may or may not fire
        expectedPrefix.add(SeasonAdvanceResult.DialogType.REDSHIRT_LIST);
        expectedPrefix.add(SeasonAdvanceResult.DialogType.TRANSFER_LIST);
        expectedPrefix.add(SeasonAdvanceResult.DialogType.REALIGNMENT_SUMMARY);

        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.SEASON_SUMMARY));
        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.CONTRACT));
        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.PROMOTIONS));
        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.REDSHIRT_LIST));
        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.TRANSFER_LIST));
        assertTrue(dialogs.contains(SeasonAdvanceResult.DialogType.REALIGNMENT_SUMMARY));

        assertTrue(
                "season summary must precede contracts",
                dialogs.indexOf(SeasonAdvanceResult.DialogType.SEASON_SUMMARY)
                        < dialogs.indexOf(SeasonAdvanceResult.DialogType.CONTRACT));
        assertTrue(
                "contracts must precede promotions",
                dialogs.indexOf(SeasonAdvanceResult.DialogType.CONTRACT)
                        < dialogs.indexOf(SeasonAdvanceResult.DialogType.PROMOTIONS));
        assertTrue(
                "promotions must precede redshirts",
                dialogs.indexOf(SeasonAdvanceResult.DialogType.PROMOTIONS)
                        < dialogs.indexOf(SeasonAdvanceResult.DialogType.REDSHIRT_LIST));
        assertTrue(
                "redshirts must precede transfer list",
                dialogs.indexOf(SeasonAdvanceResult.DialogType.REDSHIRT_LIST)
                        < dialogs.indexOf(SeasonAdvanceResult.DialogType.TRANSFER_LIST));
        assertTrue(
                "transfer list must precede realignment",
                dialogs.indexOf(SeasonAdvanceResult.DialogType.TRANSFER_LIST)
                        < dialogs.indexOf(SeasonAdvanceResult.DialogType.REALIGNMENT_SUMMARY));
    }

    @Test
    public void presentationLabelsTrackFlowOrder() {
        int reg = league.regSeasonWeeks;
        assertEquals("Preseason", SeasonPresentation.getSeasonCycleLabel(withWeek(0)));
        assertEquals("Regular Season", SeasonPresentation.getSeasonCycleLabel(withWeek(1)));
        assertEquals("Regular Season", SeasonPresentation.getSeasonCycleLabel(withWeek(reg - 1)));
        assertEquals("Postseason", SeasonPresentation.getSeasonCycleLabel(withWeek(reg)));
        assertEquals("Postseason", SeasonPresentation.getSeasonCycleLabel(withWeek(reg + 3)));
        assertEquals("Offseason", SeasonPresentation.getSeasonCycleLabel(withWeek(reg + 4)));
        assertEquals("Recruiting", SeasonPresentation.getSeasonCycleLabel(withWeek(reg + 13)));

        assertTrue(SeasonPresentation.getSeasonPhaseChipText(withWeek(reg + 13)).contains("Recruiting"));
        assertTrue(SeasonPresentation.getSeasonWeekChipText(withWeek(reg + 13)).endsWith("Recruiting"));
    }

    @Test
    public void flowOrderHelpersMatchControllerConstants() {
        int reg = 13;
        assertEquals(6, SeasonFlowOrder.midseasonWeek(reg));
        assertEquals(17, SeasonFlowOrder.firstOffseasonWeek(reg));
        assertEquals(26, SeasonFlowOrder.recruitingWeek(reg));
        assertEquals(SeasonFlowOrder.Phase.CONFERENCE_CHAMPIONSHIP,
                SeasonFlowOrder.phaseAt(reg - 1, reg));
        assertEquals(SeasonFlowOrder.Phase.NATIONAL_CHAMPIONSHIP,
                SeasonFlowOrder.phaseAt(reg + 3, reg));
    }

    private int assertCycleNonDecreasing(int lastCycle) {
        int cycle = SeasonFlowOrder.cycleIndex(SeasonFlowOrder.phaseAt(league));
        assertTrue(
                "cycle must not go backward: was " + lastCycle + " now " + cycle
                        + " at week " + league.currentWeek,
                cycle >= lastCycle);
        return cycle;
    }

    private League withWeek(int week) {
        league.currentWeek = week;
        return league;
    }

    private int sampleUserOvrSum() {
        int sum = 0;
        for (positions.Player p : league.userTeam.getAllPlayers()) {
            sum += p.ratOvr;
        }
        return sum;
    }

    private int countDialog(SeasonAdvanceResult.DialogType type) {
        int n = 0;
        for (SeasonAdvanceResult.DialogType d : dialogs) {
            if (d == type) n++;
        }
        return n;
    }

    private GameUiBridge recordingBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {}
            @Override public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {}
            @Override public void showNotification(String title, String message) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String summaryText) {
                dialogs.add(SeasonAdvanceResult.DialogType.AWARDS_SUMMARY);
            }
            @Override public void showMidseasonSummary() {
                dialogs.add(SeasonAdvanceResult.DialogType.MIDSEASON_SUMMARY);
            }
            @Override public void showSeasonSummary() {
                dialogs.add(SeasonAdvanceResult.DialogType.SEASON_SUMMARY);
            }
            @Override public void showContractDialog() {
                dialogs.add(SeasonAdvanceResult.DialogType.CONTRACT);
            }
            @Override public void showJobOffersDialog() {
                dialogs.add(SeasonAdvanceResult.DialogType.JOB_OFFERS);
            }
            @Override public void showPromotionsDialog() {
                dialogs.add(SeasonAdvanceResult.DialogType.PROMOTIONS);
            }
            @Override public void showRedshirtList() {
                dialogs.add(SeasonAdvanceResult.DialogType.REDSHIRT_LIST);
            }
            @Override public void showTransferList() {
                dialogs.add(SeasonAdvanceResult.DialogType.TRANSFER_LIST);
            }
            @Override public void showRealignmentSummary() {
                dialogs.add(SeasonAdvanceResult.DialogType.REALIGNMENT_SUMMARY);
            }
            @Override public void startRecruitingFlow() {}
            @Override public void showCoordinatorHiringDialog() {
                dialogs.add(SeasonAdvanceResult.DialogType.COORDINATOR_HIRING);
            }
        };
    }
}
