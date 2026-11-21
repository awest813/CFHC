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

    // Re-read from the league on every call: a legacy (13-week) league moves to
    // the standard length at season rollover while this controller is reused.
    private int regSeasonWeeks;
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
        this.regSeasonWeeks = league.regSeasonWeeks;
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

        // Fixed engine step for derived conference prestige (see League#refreshConferencePrestige).
        league.refreshConferencePrestige();
        bridge.refreshCurrentPage();
        result.refreshRequested();
        return result.weekAfter(league.currentWeek).build();
    }

    private void handlePreseasonTransition(SeasonAdvanceResult.Builder result) {
        redshirtComplete = true;
        if (league.userTeam != null) {
            league.userTeam.recruitWalkOns();
        }
        // Fold preseason roster/depth-chart/redshirt changes into talent and
        // projections at one fixed step, identically on every platform (this
        // used to happen only when Android's Season Goals screen was opened).
        league.updateTeamTalentRatings();
        league.setTeamBenchMarks();
        league.preseasonNews();
        league.currentWeek++; // Advance to Week 1
        updateSimStatus(result, "Preseason", nextLabel(), true);
        result.audioEvent(AudioEvent.WHISTLE);
        result.weekAdvanced();
    }

    private void handleInSeasonWeek(SeasonAdvanceResult.Builder result) {
        int weekBefore = league.currentWeek;
        java.util.List<Game> userUnplayed = unplayedUserGames();
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
        result.audioEvent(weekAudioEvent(weekBefore, justPlayed(userUnplayed)));
        updateInSeasonStatus(result);
    }

    /** The user's real (non-bye) games that have not been played yet. */
    private java.util.List<Game> unplayedUserGames() {
        java.util.List<Game> out = new java.util.ArrayList<>();
        if (league.userTeam == null || league.userTeam.getGameSchedule() == null) {
            return out;
        }
        for (Game g : league.userTeam.getGameSchedule()) {
            if (g != null && !g.hasPlayed && !g.isByeWeek()) {
                out.add(g);
            }
        }
        return out;
    }

    /** The user game this advance played, or null (bye, eliminated, not in the postseason). */
    private static Game justPlayed(java.util.List<Game> before) {
        for (Game g : before) {
            if (g.hasPlayed) {
                return g;
            }
        }
        return null;
    }

    /**
     * The single result cue for the week just played — platforms play only
     * this (plus their own click feedback when it is null), so cues never
     * stack. Keyed off the user's actual game this week:
     * <ul>
     *   <li>loss → LOSS</li>
     *   <li>national title → FIGHT_SONG</li>
     *   <li>conference title / bowl / playoff win, or a ranked upset → CROWD_ROAR</li>
     *   <li>the win that makes the team bowl eligible → FIRST_DOWN ("moving the chains")</li>
     *   <li>any other win → TOUCHDOWN_CHEER</li>
     *   <li>title game played without the user → STADIUM_ORGAN (season finale)</li>
     * </ul>
     */
    AudioEvent weekAudioEvent(int weekPlayed, Game g) {
        Team user = league.userTeam;
        if (user == null) {
            return null;
        }
        if (g == null) {
            return weekPlayed == SeasonFlowOrder.nationalChampionshipWeek(regSeasonWeeks)
                    ? AudioEvent.STADIUM_ORGAN : null;
        }
        boolean home = g.homeTeam == user;
        int us = home ? g.homeScore : g.awayScore;
        int them = home ? g.awayScore : g.homeScore;
        if (us <= them) {
            return AudioEvent.LOSS;
        }
        if (weekPlayed == SeasonFlowOrder.nationalChampionshipWeek(regSeasonWeeks)) {
            return AudioEvent.FIGHT_SONG;
        }
        if (weekPlayed >= SeasonFlowOrder.conferenceChampionshipWeek(regSeasonWeeks)) {
            return AudioEvent.CROWD_ROAR;
        }
        Team opp = home ? g.awayTeam : g.homeTeam;
        int oppRank = opp.getRankTeamPollScore();
        int ourRank = user.getRankTeamPollScore();
        if (oppRank >= 1 && oppRank <= 25 && oppRank + 4 < ourRank) {
            return AudioEvent.CROWD_ROAR; // upset over a better-ranked opponent
        }
        if (user.getWins() == BOWL_ELIGIBLE_WINS) {
            return AudioEvent.FIRST_DOWN;
        }
        return AudioEvent.TOUCHDOWN_CHEER;
    }

    /** Wins needed for bowl eligibility. */
    private static final int BOWL_ELIGIBLE_WINS = 6;

    private void updateInSeasonStatus(SeasonAdvanceResult.Builder result) {
        if (league.currentWeek == SeasonFlowOrder.bowlWeek1(regSeasonWeeks)) {
            String awards = league.getHeismanCeremonyStr();
            String linemanAwards = league.getLinemanPOTYStr();
            Player heismanWinner = league.getHeismanWinner();
            String awardsSummary = heismanWinner != null ? heismanWinner.getAwardDescription() : awards;
            if (awardsSummary == null) awardsSummary = "";
            awardsSummary = awardsSummary + "\n\n" + linemanAwards;
            bridge.showAwardsSummary(awardsSummary);
            result.needsDialog(SeasonAdvanceResult.DialogType.AWARDS_SUMMARY, awardsSummary);
        }
        updateSimStatus(result, "In Season", nextLabel(), false);
    }

    /** Label for the next advance, from the shared presentation rule. */
    private String nextLabel() {
        return SeasonPresentation.getPlayWeekLabel(league);
    }

    private void handleOffseasonWeek(SeasonAdvanceResult.Builder result) {
        if (league.currentWeek == SeasonFlowOrder.seasonSummaryWeek(regSeasonWeeks)) {
            // Close the books first: the summary reads this season's prestige change
            // (Team.prestigePts, set by enterOffseason) and the records it just broke.
            // Showing it before left prestigePts null in a dynasty's first season (an
            // Android crash; desktop showed "summary unavailable") and a year stale after.
            handleSeasonSummary(result);
            bridge.showSeasonSummary();
            result.needsDialog(SeasonAdvanceResult.DialogType.SEASON_SUMMARY, null);

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
                updateSimStatus(result, "Recruiting", nextLabel(), true);
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
        this.regSeasonWeeks = league.regSeasonWeeks;
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
        updateSimStatus(result, "Offseason", nextLabel(), true);
    }

    private void handleContracts(SeasonAdvanceResult.Builder result) {
        league.advanceStaff();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", nextLabel(), true);
        bridge.showContractDialog();
        result.needsDialog(SeasonAdvanceResult.DialogType.CONTRACT, null);
    }


    /**
     * Job offers (fired) or promotion offers (a coach on the rise) come while this
     * offseason's openings are still open: the carousel on the next step fills every
     * one, and promotions shown after it never had a job to offer.
     */
    private void handleJobOffers(SeasonAdvanceResult.Builder result) {
        league.currentWeek++;
        result.weekAdvanced();
        league.jobInterestNews();
        updateSimStatus(result, "Offseason", nextLabel(), true);
        if (league.userTeam != null && league.userTeam.fired) {
            bridge.showJobOffersDialog();
            result.needsDialog(SeasonAdvanceResult.DialogType.JOB_OFFERS, null);
        } else {
            bridge.showPromotionsDialog();
            result.needsDialog(SeasonAdvanceResult.DialogType.PROMOTIONS, null);
        }
    }

    private void handleCoachCarousel(SeasonAdvanceResult.Builder result) {
        league.coachCarousel();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", nextLabel(), true);
    }

    private void handleHireAssistants(SeasonAdvanceResult.Builder result) {
        if (league.userTeam != null && league.userTeam.isUserControlled()) {
            bridge.showCoordinatorHiringDialog();
            result.needsDialog(SeasonAdvanceResult.DialogType.COORDINATOR_HIRING, null);
        }
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", nextLabel(), true);
    }


    private void handleSeasonAdvance(SeasonAdvanceResult.Builder result) {
        league.advanceSeason();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", nextLabel(), true);
        bridge.showRedshirtList();
        result.needsDialog(SeasonAdvanceResult.DialogType.REDSHIRT_LIST, null);
    }


    private void handleTransferLogic(SeasonAdvanceResult.Builder result) {
        league.transferPlayers(bridge);
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", nextLabel(), true);
    }

    private void handleTransferList(SeasonAdvanceResult.Builder result) {
        league.currentWeek++;
        result.weekAdvanced();
        league.portalNeedsNews();
        updateSimStatus(result, "Offseason", nextLabel(), true);
        bridge.showTransferList();
        result.needsDialog(SeasonAdvanceResult.DialogType.TRANSFER_LIST, null);
    }

    private void handleRealignment(SeasonAdvanceResult.Builder result) {
        league.runOffseasonRealignment(bridge);
        league.hireMissingCoaches();
        league.currentWeek++;
        result.weekAdvanced();
        updateSimStatus(result, "Offseason", nextLabel(), true);
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
