package simulation;

import positions.Player;

/**
 * Controller for managing the flow of a season.
 * This should be independent of any UI framework.
 * Navigation/recruiting intents stay with {@link SimulationFacade} and {@link GameFlowManager};
 * this class only advances simulation state and pushes UI prompts through {@link GameUiBridge}.
 *
 * <p>Offseason weeks after the national title are mapped relative to {@link League#regSeasonWeeks}:
 * {@code +4} season summary, {@code +5} contracts, {@code +6} job offers, {@code +7} coach carousel,
 * {@code +8} coordinator hiring slot (UI may intercept), {@code +9} graduation/advance season,
 * {@code +10}–{@code +12} transfers/realignment, {@code +13} recruiting begins.
 */
public final class SeasonController {

    private final League league;
    private final GameUiBridge bridge;

    // Configuration
    private final int regSeasonWeeks;
    private boolean redshirtComplete = false;

    public SeasonController(League league, GameUiBridge bridge) {
        if (league == null) {
            throw new IllegalArgumentException("league is required");
        }
        this.league = league;
        this.bridge = bridge != null ? bridge : GameUiBridge.NO_OP;
        this.regSeasonWeeks = league.regSeasonWeeks;
    }

    public SeasonAdvanceResult advanceWeek() {
        SeasonAdvanceResult.Builder result = new SeasonAdvanceResult.Builder(league.currentWeek);
        league.clearNewsHeadlines();
        if (league.currentWeek == 0 && redshirtComplete) {
            // A new season can reset currentWeek to 0 while reusing this controller instance.
            redshirtComplete = false;
        }

        if (league.currentWeek == 0 && !redshirtComplete) {
            handlePreseasonTransition(result);
        } else if (league.currentWeek <= SeasonFlowOrder.nationalChampionshipWeek(regSeasonWeeks)) {
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
        if (league.userTeam != null) {
            league.userTeam.recruitWalkOns();
        }
        league.preseasonNews();
        league.currentWeek++; // Advance to Week 1
        updateSimStatus(result, "Preseason", "Play Week 1", true);
        result.audioEvent(AudioEvent.WHISTLE);
        result.weekAdvanced();
    }

    private void handleInSeasonWeek(SeasonAdvanceResult.Builder result) {
        int weekBefore = league.currentWeek;
        league.playWeek();
        result.weekAdvanced();

        if (weekBefore == SeasonFlowOrder.midseasonWeek(regSeasonWeeks)) {
            // Apply progression in the controller so desktop, Android, and bulk sim
            // all advance player ratings in the same order — UI only presents the report.
            league.midSeasonProgression();
            bridge.showMidseasonSummary();
            result.needsDialog(SeasonAdvanceResult.DialogType.MIDSEASON_SUMMARY, null);
        }

        result.weekDigest(league.buildWeekDigest(weekBefore));
        result.audioEvent(weekAudioEvent(weekBefore));
        updateInSeasonStatus(result);
    }

    /**
     * Result-atmosphere audio cue for the week just played: postseason crowd noise,
     * the championship organ, a roar for a ranked upset, a cheer for a routine win.
     */
    private AudioEvent weekAudioEvent(int weekPlayed) {
        if (league.userTeam == null) {
            return null;
        }
        if (weekPlayed == SeasonFlowOrder.nationalChampionshipWeek(regSeasonWeeks)) {
            return AudioEvent.STADIUM_ORGAN;
        }
        if (weekPlayed >= SeasonFlowOrder.conferenceChampionshipWeek(regSeasonWeeks)) {
            return AudioEvent.CROWD_ROAR;
        }
        Game g = league.findPlayedUserGame(weekPlayed);
        if (g == null) {
            return null;
        }
        boolean home = g.homeTeam == league.userTeam;
        int us = home ? g.homeScore : g.awayScore;
        int them = home ? g.awayScore : g.homeScore;
        if (us <= them) {
            return null;
        }
        Team opp = home ? g.awayTeam : g.homeTeam;
        int oppRank = opp.getRankTeamPollScore();
        int ourRank = league.userTeam.getRankTeamPollScore();
        if (oppRank >= 1 && oppRank <= 25 && oppRank + 4 < ourRank) {
            return AudioEvent.CROWD_ROAR; // upset over a better-ranked opponent
        }
        return AudioEvent.TOUCHDOWN_CHEER;
    }

    private void updateInSeasonStatus(SeasonAdvanceResult.Builder result) {
        String buttonText;
        if (league.currentWeek < SeasonFlowOrder.conferenceChampionshipWeek(regSeasonWeeks)) {
            buttonText = "Play Week " + (league.currentWeek + 1);
        } else if (league.currentWeek == SeasonFlowOrder.conferenceChampionshipWeek(regSeasonWeeks)) {
            buttonText = "Play Conf Championships";
        } else if (league.currentWeek == SeasonFlowOrder.bowlWeek1(regSeasonWeeks)) {
            String awards = league.getHeismanCeremonyStr();
            String linemanAwards = league.getLinemanPOTYStr();
            Player heismanWinner = league.getHeismanWinner();
            String awardsSummary = heismanWinner != null ? heismanWinner.getAwardDescription() : awards;
            if (awardsSummary == null) awardsSummary = "";
            awardsSummary = awardsSummary + "\n\n" + linemanAwards;
            bridge.showAwardsSummary(awardsSummary);
            result.needsDialog(SeasonAdvanceResult.DialogType.AWARDS_SUMMARY, awardsSummary);

            buttonText = league.expPlayoffs ? "Play First Round" : "Play Bowl Week 1";

        } else if (league.currentWeek == SeasonFlowOrder.bowlWeek2(regSeasonWeeks)) {
            buttonText = league.expPlayoffs ? "Play Quarterfinals" : "Play Bowl Week 2";
        } else if (league.currentWeek == SeasonFlowOrder.bowlWeek3(regSeasonWeeks)) {
            buttonText = league.expPlayoffs ? "Play Semifinals" : "Play Bowl Week 3";
        } else if (league.currentWeek == SeasonFlowOrder.nationalChampionshipWeek(regSeasonWeeks)) {
            buttonText = "Play National Championship";
        } else {
            buttonText = "Season Summary";
        }
        updateSimStatus(result, "In Season", buttonText, false);
    }

    private void handleOffseasonWeek(SeasonAdvanceResult.Builder result) {
        if (league.currentWeek == SeasonFlowOrder.seasonSummaryWeek(regSeasonWeeks)) {
            bridge.showSeasonSummary();
            result.needsDialog(SeasonAdvanceResult.DialogType.SEASON_SUMMARY, null);
            handleSeasonSummary(result);

        } else if (league.currentWeek == SeasonFlowOrder.contractsWeek(regSeasonWeeks)) {
            handleContracts(result);
        } else if (league.currentWeek == SeasonFlowOrder.jobOffersWeek(regSeasonWeeks)) {
            handleJobOffers(result);
        } else if (league.currentWeek == SeasonFlowOrder.coachCarouselWeek(regSeasonWeeks)) {
            handleCoachCarousel(result);
        } else if (league.currentWeek == SeasonFlowOrder.coordinatorHiringWeek(regSeasonWeeks)) {
            handleHireAssistants(result);
        } else if (league.currentWeek == SeasonFlowOrder.graduationWeek(regSeasonWeeks)) {
            handleSeasonAdvance(result);
        } else if (league.currentWeek == SeasonFlowOrder.transferPortalWeek(regSeasonWeeks)) {
            handleTransferLogic(result);
        } else if (league.currentWeek == SeasonFlowOrder.transferListWeek(regSeasonWeeks)) {
            handleTransferList(result);
        } else if (league.currentWeek == SeasonFlowOrder.realignmentWeek(regSeasonWeeks)) {
            handleRealignment(result);
        } else if (SeasonFlowOrder.isRecruitingGate(league.currentWeek, regSeasonWeeks)) {
            if (!league.recruitingPhaseActive) {
                league.recruitingPhaseActive = true;
                showNotification(result, "Recruiting", "National Letter of Intent Day Begins!");
                bridge.startRecruitingFlow();
                result.recruitingStarted();
            } else {
                // Hard gate: do not advance past recruiting until finishRecruiting / startNextSeason.
                updateSimStatus(result, "Recruiting", "Complete Recruiting", true);
                result.awaitingRecruiting();
            }
        }
    }

    /**
     * Clears the recruiting gate without any UI: runs the CPU recruiting pass and
     * rolls straight into the next season with an empty user class. Headless hosts
     * and automation call this instead of pressing {@link #advanceWeek()} at the
     * gate, which never advances the week on its own.
     *
     * @return true when the gate was active and the year rolled over
     */
    public boolean autoCompleteRecruiting() {
        if (!SeasonFlowOrder.isRecruitingGate(league.currentWeek, regSeasonWeeks)) {
            return false;
        }
        league.recruitPlayers();
        league.finishRecruitingSeason("");
        return true;
    }


    private void handleSeasonSummary(SeasonAdvanceResult.Builder result) {
        league.enterOffseason();
        league.checkLeagueRecords();
        league.updateHCHistory();
        league.updateTeamHistories();
        league.updateLeagueHistory();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Offseason: Contracts", true);
    }

    private void handleContracts(SeasonAdvanceResult.Builder result) {
        league.advanceStaff();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Offseason: Job Offers", true);
        bridge.showContractDialog();
        result.needsDialog(SeasonAdvanceResult.DialogType.CONTRACT, null);
    }


    private void handleJobOffers(SeasonAdvanceResult.Builder result) {
        league.currentWeek++;
        result.weekAdvanced();
        league.jobInterestNews();
        updateSimStatus(result, "Offseason", "Offseason: Coaching Changes", true);
        if (league.userTeam != null && league.userTeam.fired) {
            bridge.showJobOffersDialog();
            result.needsDialog(SeasonAdvanceResult.DialogType.JOB_OFFERS, null);
        }
    }

    private void handleCoachCarousel(SeasonAdvanceResult.Builder result) {
        league.coachCarousel();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Offseason: Coordinator Changes", true);
        bridge.showPromotionsDialog();
        result.needsDialog(SeasonAdvanceResult.DialogType.PROMOTIONS, null);
    }

    private void handleHireAssistants(SeasonAdvanceResult.Builder result) {
        if (league.userTeam != null && league.userTeam.isUserControlled()) {
            bridge.showCoordinatorHiringDialog();
            result.needsDialog(SeasonAdvanceResult.DialogType.COORDINATOR_HIRING, null);
        }
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Offseason: Graduation", true);
    }


    private void handleSeasonAdvance(SeasonAdvanceResult.Builder result) {
        league.advanceSeason();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Offseason: Transfer List", true);
        bridge.showRedshirtList();
        result.needsDialog(SeasonAdvanceResult.DialogType.REDSHIRT_LIST, null);
    }


    private void handleTransferLogic(SeasonAdvanceResult.Builder result) {
        league.transferPlayers(bridge);
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", "Offseason: Complete Transfers", true);
    }

    private void handleTransferList(SeasonAdvanceResult.Builder result) {
        league.currentWeek++;
        result.weekAdvanced();
        league.portalNeedsNews();
        updateSimStatus(result, "Offseason", "Offseason: Continue", true);
        bridge.showTransferList();
        result.needsDialog(SeasonAdvanceResult.DialogType.TRANSFER_LIST, null);
    }

    private void handleRealignment(SeasonAdvanceResult.Builder result) {
        league.runOffseasonRealignment(bridge);
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
