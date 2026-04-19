package simulation;

import java.util.Random;

import positions.Player;

/**
 * Controller for managing the flow of a season.
 * This should be independent of any UI framework.
 */
public final class SeasonController {

    private final League league;
    private final GameUiBridge bridge;
    private final GameFlowManager flowManager;
    private final Random random = new Random();

    // Configuration
    private final int regSeasonWeeks;
    private boolean redshirtComplete = false;

    public SeasonController(League league, GameUiBridge bridge, GameFlowManager flowManager) {
        this.league = league;
        this.bridge = bridge;
        this.flowManager = flowManager;
        this.regSeasonWeeks = league.regSeasonWeeks;
    }

    public void advanceWeek() {
        league.clearNewsHeadlines();

        if (league.currentWeek == 0 && !redshirtComplete) {
            handlePreseasonTransition();
        } else if (league.currentWeek <= regSeasonWeeks + 3) {
            handleInSeasonWeek();
        } else {
            handleOffseasonWeek();
        }

        bridge.refreshCurrentPage();
    }

    private void handlePreseasonTransition() {
        redshirtComplete = true;
        league.userTeam.recruitWalkOns();
        league.preseasonNews();
        bridge.updateSimStatus("Preseason", "Play Week 1", true);
    }

    private void handleInSeasonWeek() {
        int weekBefore = league.currentWeek;
        league.playWeek();

        if (weekBefore == regSeasonWeeks / 2) {
            bridge.showMidseasonSummary();
        }


        updateInSeasonStatus();
    }

    private void updateInSeasonStatus() {
        String buttonText;
        if (league.currentWeek < regSeasonWeeks - 1) {
            buttonText = "Play Week " + (league.currentWeek + 1);
        } else if (league.currentWeek == regSeasonWeeks - 1) {
            buttonText = "Play Conf Championships";
        } else if (league.currentWeek == regSeasonWeeks) {
            String awards = league.getHeismanCeremonyStr();
            Player heismanWinner = league.getHeismanWinner();
            bridge.showAwardsSummary(heismanWinner != null ? heismanWinner.getAwardDescription() : awards);

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
        bridge.updateSimStatus("In Season", buttonText, false);
    }

    private void handleOffseasonWeek() {
        if (league.currentWeek == regSeasonWeeks + 4) {
            bridge.showSeasonSummary();
            handleSeasonSummary();

        } else if (league.currentWeek == regSeasonWeeks + 5) {
            handleContracts();
        } else if (league.currentWeek == regSeasonWeeks + 6) {
            handleJobOffers();
        } else if (league.currentWeek == regSeasonWeeks + 7) {
            handleCoachCarousel();
        } else if (league.currentWeek == regSeasonWeeks + 8) {
            handleHireAssistants();
        } else if (league.currentWeek == regSeasonWeeks + 9) {
            handleSeasonAdvance();
        } else if (league.currentWeek == regSeasonWeeks + 10) {
            handleTransferLogic();
        } else if (league.currentWeek == regSeasonWeeks + 11) {
            handleTransferList();
        } else if (league.currentWeek == regSeasonWeeks + 12) {
            handleRealignment();
        } else if (league.currentWeek >= regSeasonWeeks + 13) {
            bridge.showNotification("Recruiting", "National Letter of Intent Day Begins!");
            bridge.startRecruitingFlow();
        }
    }


    private void handleSeasonSummary() {
        league.enterOffseason();
        league.checkLeagueRecords();
        league.updateHCHistory();
        league.updateTeamHistories();
        league.updateLeagueHistory();
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Contracts", true);
    }

    private void handleContracts() {
        league.advanceStaff();
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Job Offers", true);
        bridge.showContractDialog();
    }


    private void handleJobOffers() {
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Coaching Changes", true);
        bridge.showJobOffersDialog();
    }

    private void handleCoachCarousel() {
        league.coachCarousel();
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Coordinator Changes", true);
        bridge.showPromotionsDialog();
    }

    private void handleHireAssistants() {
        if (league.userTeam.isUserControlled()) {
            // This usually triggers a separate flow/activity
            // flowManager.hireStaff(league.userTeam.strRep());
        }
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Graduation", true);
    }


    private void handleSeasonAdvance() {
        league.advanceSeason();
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Transfer List", true);
        bridge.showRedshirtList();
    }


    private void handleTransferLogic() {
        league.transferPlayers(bridge);
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Complete Transfers", true);
    }

    private void handleTransferList() {
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Off-Season: Continue", true);
        bridge.showTransferList();
    }

    private void handleRealignment() {
        // These methods should ideally move to League if they are pure logic
        league.hireMissingCoaches();
        league.currentWeek++;
        bridge.updateSimStatus("Offseason", "Begin Recruiting", true);
        bridge.showRealignmentSummary();
    }


}

