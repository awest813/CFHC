package simulation;

import positions.Player;

/**
 * Controller for managing the flow of a season.
 * This should be independent of any UI framework.
 */
public final class SeasonController {

    private final League league;
    private final GameUiBridge bridge;

    // Configuration
    private final int regSeasonWeeks;
    private boolean redshirtComplete = false;

    public SeasonController(League league, GameUiBridge bridge, GameFlowManager flowManager) {
        this.league = league;
        this.bridge = bridge;
        this.regSeasonWeeks = league.regSeasonWeeks;
    }

    public SeasonAdvanceResult advanceWeek() {
        SeasonAdvanceResult.Builder result = new SeasonAdvanceResult.Builder(league.currentWeek);
        league.clearNewsHeadlines();

        if (league.currentWeek == 0 && !redshirtComplete) {
            handlePreseasonTransition(result);
        } else if (league.currentWeek <= regSeasonWeeks + 3) {
            handleInSeasonWeek(result);
        } else {
            handleOffseasonWeek(result);
        }

        bridge.refreshCurrentPage();
        result.refreshRequested();
        return result.weekAfter(league.currentWeek).build();
    }

    private void handlePreseasonTransition(SeasonAdvanceResult.Builder result) {
        redshirtComplete = true;
        league.userTeam.recruitWalkOns();
        league.preseasonNews();
        league.currentWeek++; // Advance to Week 1
        updateSimStatus(result, "Preseason", "Play Week 1", true);
        result.weekAdvanced();
    }

    private void handleInSeasonWeek(SeasonAdvanceResult.Builder result) {
        int weekBefore = league.currentWeek;
        league.playWeek();
        result.weekAdvanced();

        if (weekBefore == regSeasonWeeks / 2) {
            bridge.showMidseasonSummary();
            result.needsDialog(SeasonAdvanceResult.DialogType.MIDSEASON_SUMMARY, null);
        }


        updateInSeasonStatus(result);
    }

    private void updateInSeasonStatus(SeasonAdvanceResult.Builder result) {
        String buttonText;
        if (league.currentWeek < regSeasonWeeks - 1) {
            buttonText = "Play Week " + (league.currentWeek + 1);
        } else if (league.currentWeek == regSeasonWeeks - 1) {
            buttonText = "Play Conf Championships";
        } else if (league.currentWeek == regSeasonWeeks) {
            String awards = league.getHeismanCeremonyStr();
            Player heismanWinner = league.getHeismanWinner();
            String awardsSummary = heismanWinner != null ? heismanWinner.getAwardDescription() : awards;
            bridge.showAwardsSummary(awardsSummary);
            result.needsDialog(SeasonAdvanceResult.DialogType.AWARDS_SUMMARY, awardsSummary);

            buttonText = league.expPlayoffs ? "Play First Round" : "Play Bowl Week 1";

        } else if (league.currentWeek == regSeasonWeeks + 1) {
            buttonText = league.expPlayoffs ? "Play Quarterfinals" : "Play Bowl Week 2";
        } else if (league.currentWeek == regSeasonWeeks + 2) {
            buttonText = league.expPlayoffs ? "Play Semifinals" : "Play Bowl Week 3";
        } else if (league.currentWeek == regSeasonWeeks + 3) {
            buttonText = "Play National Championship";
        } else {
            buttonText = "Season Summary";
        }
        updateSimStatus(result, "In Season", buttonText, false);
    }

    private void handleOffseasonWeek(SeasonAdvanceResult.Builder result) {
        if (league.currentWeek == regSeasonWeeks + 4) {
            bridge.showSeasonSummary();
            result.needsDialog(SeasonAdvanceResult.DialogType.SEASON_SUMMARY, null);
            handleSeasonSummary(result);

        } else if (league.currentWeek == regSeasonWeeks + 5) {
            handleContracts(result);
        } else if (league.currentWeek == regSeasonWeeks + 6) {
            handleJobOffers(result);
        } else if (league.currentWeek == regSeasonWeeks + 7) {
            handleCoachCarousel(result);
        } else if (league.currentWeek == regSeasonWeeks + 8) {
            handleHireAssistants(result);
        } else if (league.currentWeek == regSeasonWeeks + 9) {
            handleSeasonAdvance(result);
        } else if (league.currentWeek == regSeasonWeeks + 10) {
            handleTransferLogic(result);
        } else if (league.currentWeek == regSeasonWeeks + 11) {
            handleTransferList(result);
        } else if (league.currentWeek == regSeasonWeeks + 12) {
            handleRealignment(result);
        } else if (league.currentWeek >= regSeasonWeeks + 13) {
            showNotification(result, "Recruiting", "National Letter of Intent Day Begins!");
            bridge.startRecruitingFlow();
            result.recruitingStarted();
        }
    }


    private void handleSeasonSummary(SeasonAdvanceResult.Builder result) {
        league.enterOffseason();
        league.checkLeagueRecords();
        league.updateHCHistory();
        league.updateTeamHistories();
        league.updateLeagueHistory();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Contracts", true);
    }

    private void handleContracts(SeasonAdvanceResult.Builder result) {
        league.advanceStaff();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Job Offers", true);
        bridge.showContractDialog();
        result.needsDialog(SeasonAdvanceResult.DialogType.CONTRACT, null);
    }


    private void handleJobOffers(SeasonAdvanceResult.Builder result) {
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Coaching Changes", true);
        bridge.showJobOffersDialog();
        result.needsDialog(SeasonAdvanceResult.DialogType.JOB_OFFERS, null);
    }

    private void handleCoachCarousel(SeasonAdvanceResult.Builder result) {
        league.coachCarousel();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Coordinator Changes", true);
        bridge.showPromotionsDialog();
        result.needsDialog(SeasonAdvanceResult.DialogType.PROMOTIONS, null);
    }

    private void handleHireAssistants(SeasonAdvanceResult.Builder result) {
        if (league.userTeam.isUserControlled()) {
            // This usually triggers a separate flow/activity
            // Future structured flow result can expose a hire-staff request here.
        }
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Graduation", true);
    }


    private void handleSeasonAdvance(SeasonAdvanceResult.Builder result) {
        league.advanceSeason();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Transfer List", true);
        bridge.showRedshirtList();
        result.needsDialog(SeasonAdvanceResult.DialogType.REDSHIRT_LIST, null);
    }


    private void handleTransferLogic(SeasonAdvanceResult.Builder result) {
        league.transferPlayers(bridge);
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Complete Transfers", true);
    }

    private void handleTransferList(SeasonAdvanceResult.Builder result) {
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Off-Season: Continue", true);
        bridge.showTransferList();
        result.needsDialog(SeasonAdvanceResult.DialogType.TRANSFER_LIST, null);
    }

    private void handleRealignment(SeasonAdvanceResult.Builder result) {
        // These methods should ideally move to League if they are pure logic
        league.hireMissingCoaches();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Begin Recruiting", true);
        bridge.showRealignmentSummary();
        result.needsDialog(SeasonAdvanceResult.DialogType.REALIGNMENT_SUMMARY, null);
    }

    private void updateSimStatus(SeasonAdvanceResult.Builder result, String statusText, String buttonText, boolean majorEvent) {
        bridge.updateSimStatus(statusText, buttonText, majorEvent);
        result.statusUpdated(statusText, buttonText, majorEvent);
    }

    private void showNotification(SeasonAdvanceResult.Builder result, String title, String message) {
        bridge.showNotification(title, message);
        result.notification(title, message);
    }

}

