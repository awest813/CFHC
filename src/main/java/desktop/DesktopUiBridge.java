package desktop;

import simulation.GameUiBridge;
import simulation.League;
import simulation.PlatformLog;
import simulation.SimulationFacade;
import simulation.Team;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.IOException;

/**
 * Desktop implementation of {@link GameUiBridge}.  Each callback is handled
 * with a lightweight Swing dialog so that the full season + offseason loop can
 * run without any Android dependencies.
 *
 * <p>After recruiting completes, {@link #isNewSeasonPending()} becomes
 * {@code true} so {@link LeagueHomeView} can call {@link League#startNextSeason()}.
 * Interactive recruiting runs in the docked Recruiting tab ({@link #isAwaitingDockedRecruiting()}).
 *
 * <p>Background bulk simulation sets {@link #setSuppressBlockingUi(boolean)} so
 * <em>informational</em> modals (awards, midseason text, news) do not spam the
 * worker thread. Career <em>decisions</em> (contracts, jobs, transfers, discipline,
 * redshirts, coordinator hires) still block on the EDT via
 * {@link javax.swing.SwingUtilities#invokeAndWait} so bulk advance cannot skip them.
 */
public class DesktopUiBridge implements GameUiBridge {

    private static final String TAG = "DesktopUiBridge";

    private final JFrame owner;
    private final League league;
    private boolean newSeasonPending = false;
    /** User must finish recruiting in {@link LeagueHomeView}'s Recruiting tab. */
    private boolean awaitingDockedRecruiting = false;
    /** When true, informational prompts are logged instead of shown modally. */
    private boolean suppressBlockingUi = false;

    public DesktopUiBridge(JFrame owner, League league) {
        this.owner = owner;
        this.league = league;
    }

    /**
     * Suppress informational modal dialogs during background simulation.
     * Decision dialogs still run on the EDT so career choices are not skipped.
     */
    /** Informational dialogs deferred during a bulk run; replayed by {@link #drainDeferredDialogs()}. */
    private final java.util.List<Runnable> deferredDialogs = new java.util.ArrayList<>();
    /** Text digests deferred during a bulk run; replayed as ONE dialog by {@link #drainDeferredDialogs()}. */
    private final java.util.List<Object[]> deferredDigests = new java.util.ArrayList<>();

    public void setSuppressBlockingUi(boolean suppressBlockingUi) {
        this.suppressBlockingUi = suppressBlockingUi;
        if (suppressBlockingUi) {
            deferredDialogs.clear();
            deferredDigests.clear();
        }
    }

    /**
     * Replays the informational dialogs a bulk run deferred. Text digests
     * (season summary, midseason report, realignment) are merged into a single
     * scrollable dialog so the user reviews one digest instead of clicking
     * through a modal stack; richer dialogs (awards) still replay on their own.
     * Call once when the bulk finishes — before any new-season prompt.
     */
    public void drainDeferredDialogs() {
        if (deferredDialogs.isEmpty() && deferredDigests.isEmpty()) {
            return;
        }
        java.util.List<Runnable> replay = new java.util.ArrayList<>(deferredDialogs);
        deferredDialogs.clear();

        final StringBuilder digest = new StringBuilder();
        for (Object[] entry : deferredDigests) {
            String title = (String) entry[0];
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<String> content =
                    (java.util.function.Supplier<String>) entry[1];
            String body;
            try {
                body = content.get();
            } catch (RuntimeException ex) {
                body = "(unavailable)";
            }
            if (digest.length() > 0) {
                digest.append("\n\n");
            }
            digest.append("═══ ").append(title.toUpperCase(java.util.Locale.ROOT))
                    .append(" ═══\n\n").append(body == null ? "" : body.trim());
        }
        deferredDigests.clear();
        final String digestText = digest.toString();

        if (!replay.isEmpty()) {
            for (Runnable r : replay) {
                javax.swing.SwingUtilities.invokeLater(r);
            }
        }
        if (!digestText.isEmpty()) {
            javax.swing.SwingUtilities.invokeLater(
                    () -> DesktopTheme.showScrollableText(owner, "Season Digest", digestText));
        }
    }

    /** Queue-or-drop for informational dialogs: defer when bulking with a window, drop when headless. */
    private void deferOrLog(String title, Runnable show) {
        if (owner != null) {
            deferredDialogs.add(show);
        } else {
            logDialog(title, "(deferred)");
        }
    }

    /** Queue-or-merge a text digest section for the combined Season Digest dialog. */
    private void deferDigest(String title, java.util.function.Supplier<String> content) {
        if (owner != null) {
            deferredDigests.add(new Object[]{title, content});
        } else {
            String text;
            try {
                text = content.get();
            } catch (RuntimeException ex) {
                text = "(unavailable)";
            }
            logDialog(title, text);
        }
    }

    /** True after {@link #startRecruitingFlow()} has been called. */
    public boolean isNewSeasonPending() {
        return newSeasonPending;
    }

    /** Reset the flag once the caller has handled the new-season transition. */
    public void clearNewSeasonPending() {
        newSeasonPending = false;
    }

    /** True after NLI week begins until the user finishes the Recruiting tab. */
    public boolean isAwaitingDockedRecruiting() {
        return awaitingDockedRecruiting;
    }

    /**
     * Applies recruit signings from the docked tab and marks the season ready to roll.
     */
    public void completeDockedRecruiting(String recruitsData) {
        if (!awaitingDockedRecruiting) {
            return;
        }
        awaitingDockedRecruiting = false;
        league.applyRecruitingSignings(recruitsData);
        newSeasonPending = true;
    }

    // -------------------------------------------------------------------------
    // GameUiBridge implementation
    // -------------------------------------------------------------------------

    @Override
    public void crash() {
        runDecisionUi(() -> JOptionPane.showMessageDialog(owner,
                DesktopTheme.messageForDialog(
                        "A fatal simulation error occurred. The save may be invalid, unsupported, or missing required data."),
                "Simulation Error", JOptionPane.ERROR_MESSAGE));
    }

    @Override
    public void startRecruiting(File saveFile, Team userTeam) throws InterruptedException, IOException {
        // Desktop uses auto-recruiting; this path is not reached via SeasonController.
    }

    @Override
    public void transferPlayer(positions.Player player) {
        if (player == null || league.userTeam == null) return;

        runDecisionUi(() -> {
            int choice = JOptionPane.showOptionDialog(owner,
                    DesktopTheme.messageForDialog(buildTransferOfferText(player)),
                    "Transfer Offer: " + player.position + " " + player.name,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Accept", "Decline"},
                    "Accept");

            if (choice == 0) {
                league.userTransfers = league.userTransfers
                        + player.position + " " + player.name + " " + player.getYrStr()
                        + " Ovr: " + player.ratOvr + " (" + player.team.getName() + ")\n";
                league.sumTransfers = league.sumTransfers
                        + player.ratOvr + " " + player.position + " " + player.name
                        + " [" + player.getTransferStatus() + "] "
                        + league.userTeam.getName() + " (" + player.team.getAbbr() + ")";
                Team oldTeam = player.team;
                player.team = league.userTeam;
                if (oldTeam != null) {
                    oldTeam.removePlayer(player);
                }
                league.userTeam.addPlayer(player);
            } else {
                player.isTransfer = false;
                if (player.team != null) {
                    player.team.addPlayer(player);
                }
            }
        });
    }

    @Override
    public void updateSpinners() {
        // No spinner widgets on the desktop.
    }

    @Override
    public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {
        if (player == null || league.userTeam == null) return;
        runDecisionUi(() -> {
            DisciplineDialog.show(owner, league.userTeam, player, issue, gamesA, gamesB);
            if (league.userTeam.suspension) {
                String news = league.userTeam.suspensionNews;
                if (news == null || news.isEmpty()) {
                    news = "A player was suspended.";
                }
                JOptionPane.showMessageDialog(owner,
                        DesktopTheme.messageForDialog(news),
                        "Disciplinary Action",
                        JOptionPane.WARNING_MESSAGE);
                league.userTeam.suspension = false;
            }
        });
    }

    @Override
    public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {
        // Status is reflected by LeagueHomeView.refresh() after advanceWeek() returns.
    }

    @Override
    public void showNotification(String title, String message) {
        showInfo(title, message);
    }

    @Override
    public void refreshCurrentPage() {
        // Handled by LeagueHomeView after advanceWeek() returns.
    }

    @Override
    public void showAwardsSummary(String summaryText) {
        if (suppressInformationalUi()) {
            deferOrLog("Awards", () -> SeasonAwardsDialog.show(owner, league, summaryText));
            return;
        }
        SeasonAwardsDialog.show(owner, league, summaryText);
    }

    @Override
    public void showMidseasonSummary() {
        if (suppressInformationalUi()) {
            deferDigest("Mid-Season Progress Report", this::buildMidseasonSummary);
            return;
        }
        showScrollableText("Mid-Season Progress Report", buildMidseasonSummary());
    }

    @Override
    public void showSeasonSummary() {
        String summary;
        try {
            summary = league.seasonSummaryStr();
        } catch (RuntimeException ex) {
            PlatformLog.w(TAG, "Season summary unavailable: " + ex.getMessage());
            summary = "Season summary is unavailable (championship data not ready).";
        }
        final String summaryFinal = summary;
        if (suppressInformationalUi()) {
            deferDigest("Season Summary", () -> summaryFinal);
            return;
        }
        showScrollableText("Season Summary", summary);
    }

    @Override
    public void showContractDialog() {
        runDecisionUi(() -> {
            if (league.isCareerMode() && league.userTeam != null) {
                boolean retired = ContractDialog.show(owner, league);
                if (retired) {
                    handleRetirement();
                }
            }
        });
    }

    /**
     * Retirement follow-through: the CONTRACT dialog only sets the flag —
     * without this the season rolls on with a "retired" coach and the same
     * panel reappears next offseason. Show the career retrospective, then let
     * the user start a new career at the same program or return to the Hub.
     */
    private void handleRetirement() {
        Team team = league.userTeam;
        String name = team != null && team.getHeadCoach() != null
                ? team.getHeadCoach().name : "Head Coach";
        String record = team != null
                ? team.getName() + " — " + team.getWins() + "-" + team.getLosses()
                        + " this season, " + team.getTotalWins() + "-" + team.getTotalLosses()
                        + " all-time (" + team.getTotalCCs() + " conference titles, "
                        + team.getTotalNCs() + " national titles)"
                : "Career complete.";
        StringBuilder text = new StringBuilder();
        text.append(name).append(" has retired from college football.\n\n")
                .append(record).append("\n\n")
                .append("Your coaching legacy is written into the league and team histories.");

        if (owner instanceof LeagueHomeView view) {
            DesktopTheme.showScrollableText(view, "COACH RETIREMENT", text.toString());
            String[] options = team != null
                    ? new String[]{"NEW CAREER AT " + team.getAbbr(), "CAREER HUB"}
                    : new String[]{"CAREER HUB"};
            int choice = JOptionPane.showOptionDialog(view,
                    DesktopTheme.messageForDialog("Life after football — what's next?\n\n"
                            + "New career: take over " + (team != null ? team.getName() : "the program")
                            + " with a fresh coach (small prestige hit).\n"
                            + "Career Hub: leave the sideline; you can keep this save or start a dynasty elsewhere."),
                    "Life After Football",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);
            if (choice == 0 && team != null) {
                view.reincarnateCoach();
            } else {
                view.retireToLauncher();
            }
        } else {
            showScrollableText("Coach Retirement", text.toString());
        }
    }

    @Override
    public void showJobOffersDialog() {
        runDecisionUi(() -> {
            if (league.isCareerMode() && league.userTeam != null && league.userTeam.fired) {
                boolean accepted = JobOffersDialog.showJobOffers(owner, league);
                if (accepted && userTeamNeedsCoordinatorHire(league.userTeam)) {
                    // Only open the hire pass when the new staff is actually
                    // short a coordinator; the dialog itself runs the CPU
                    // carousel on close.
                    CoordinatorHiringDialog.show(owner, league);
                } else if (accepted) {
                    league.coordinatorCarousel();
                }
            }
        });
    }

    @Override
    public void showPromotionsDialog() {
        runDecisionUi(() -> {
            if (!league.isCareerMode()) {
                return;
            }
            // Skip the empty-notice dialog in quiet years: nothing to decide
            // when the coach is not a promotion candidate.
            if (league.userTeam != null && league.userTeam.getHeadCoach() != null
                    && !league.userTeam.getHeadCoach().promotionCandidate) {
                return;
            }
            // Hiring is owned by showCoordinatorHiringDialog on the next offseason step.
            JobOffersDialog.showPromotions(owner, league);
        });
    }

    @Override
    public void showCoordinatorHiringDialog() {
        runDecisionUi(() -> {
            if (league.userTeam == null || !league.userTeam.isUserControlled()) {
                return;
            }
            if (userTeamNeedsCoordinatorHire(league.userTeam)) {
                CoordinatorHiringDialog.show(owner, league);
            }
        });
    }

    private static boolean userTeamNeedsCoordinatorHire(Team team) {
        if (team.OC == null || team.DC == null) {
            return true;
        }
        return team.OC.contractYear >= team.OC.contractLength
                || team.DC.contractYear >= team.DC.contractLength;
    }

    @Override
    public void showRedshirtList() {
        runDecisionUi(() -> RedshirtDialog.show(owner, league));
    }

    @Override
    public void showTransferList() {
        runDecisionUi(() -> TransferPortalDialog.show(owner, league));
    }

    @Override
    public void showRealignmentSummary() {
        String news = league.newsRealignment;
        if (news == null || news.isEmpty()) {
            news = "No conference realignment occurred this off-season.";
        }
        final String newsFinal = news;
        if (suppressInformationalUi()) {
            deferDigest("Conference Realignment", () -> newsFinal);
            return;
        }
        showScrollableText("Conference Realignment", news);
    }

    @Override
    public void startRecruitingFlow() {
        if (awaitingDockedRecruiting) {
            return;
        }
        SimulationFacade.prepareCpuRecruiting(league);

        if (SimulationFacade.needsUserRecruiting(league)) {
            awaitingDockedRecruiting = true;
            return;
        }

        newSeasonPending = true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean suppressInformationalUi() {
        return owner == null || suppressBlockingUi;
    }

    /**
     * Career / roster decisions must run even during bulk sim. When called from a
     * worker thread, block that worker on the EDT until the user finishes.
     */
    private void runDecisionUi(Runnable action) {
        if (owner == null || action == null) {
            return;
        }
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            javax.swing.SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            PlatformLog.w(TAG, "Decision UI interrupted");
        } catch (java.lang.reflect.InvocationTargetException e) {
            PlatformLog.e(TAG, "Decision UI failed", e.getCause() != null ? e.getCause() : e);
        }
    }

    private void showInfo(String title, String message) {
        if (suppressInformationalUi()) {
            logDialog(title, message);
            return;
        }
        JOptionPane.showMessageDialog(owner, DesktopTheme.messageForDialog(message), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showScrollableText(String title, String text) {
        if (suppressInformationalUi()) {
            logDialog(title, text);
            return;
        }
        DesktopTheme.showScrollableText(owner, title, text);
    }

    private static void logDialog(String title, String text) {
        PlatformLog.i(TAG, title + ": " + (text != null ? text.replace('\n', ' ') : ""));
    }

    private String buildTransferOfferText(positions.Player player) {
        return "Transfer " + (player.isTransfer ? player.getTransferStatus() : "") + " Request\n\n"
                + "Player: " + player.position + " " + player.name + "\n"
                + "Year: " + player.getYrStr() + "\n"
                + "Overall: " + player.ratOvr + "\n"
                + "From: " + (player.team != null ? player.team.getName() : "Unknown") + "\n\n"
                + "Accept this transfer to your roster?";
    }

    private String buildMidseasonSummary() {
        if (league.userTeam != null) {
            String report = league.userTeam.midseasonUserProgression();
            if (report != null && !report.trim().isEmpty()) {
                simulation.Team t = league.userTeam;
                return "Mid-Season Progress Report\n\n"
                        + t.getName() + "  (" + t.getWins() + "-" + t.getLosses() + ")\n\n"
                        + report;
            }
            return "No player rating changes to report this midseason.";
        }
        return "Midseason progression complete.";
    }
}
