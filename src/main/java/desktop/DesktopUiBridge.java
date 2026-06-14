package desktop;

import simulation.GameUiBridge;
import simulation.League;
import simulation.PlatformLog;
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
 * modal dialogs do not deadlock worker threads.
 */
public class DesktopUiBridge implements GameUiBridge {

    private static final String TAG = "DesktopUiBridge";

    private final JFrame owner;
    private final League league;
    private boolean newSeasonPending = false;
    /** User must finish recruiting in {@link LeagueHomeView}'s Recruiting tab. */
    private boolean awaitingDockedRecruiting = false;
    /** When true, offseason prompts are logged instead of shown modally. */
    private boolean suppressBlockingUi = false;

    public DesktopUiBridge(JFrame owner, League league) {
        this.owner = owner;
        this.league = league;
    }

    /**
     * Suppress modal dialogs during background simulation. League home enables
     * this on {@link javax.swing.SwingWorker} threads and clears it afterward.
     */
    public void setSuppressBlockingUi(boolean suppressBlockingUi) {
        this.suppressBlockingUi = suppressBlockingUi;
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
        if (league.userTeam != null) {
            league.userTeam.recruitPlayersFromStr(recruitsData == null ? "" : recruitsData);
            league.updateTeamTalentRatings();
        }
        newSeasonPending = true;
    }

    // -------------------------------------------------------------------------
    // GameUiBridge implementation
    // -------------------------------------------------------------------------

    @Override
    public void crash() {
        if (!canShowBlockingDialog()) {
            PlatformLog.e(TAG, "Simulation crash reported while UI suppressed");
            return;
        }
        JOptionPane.showMessageDialog(owner,
                DesktopTheme.messageForDialog("A fatal simulation error occurred."),
                "Simulation Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void startRecruiting(File saveFile, Team userTeam) throws InterruptedException, IOException {
        // Desktop uses auto-recruiting; this path is not reached via SeasonController.
    }

    @Override
    public void transferPlayer(positions.Player player) {
        if (player == null || league.userTeam == null) return;

        if (!canShowBlockingDialog()) {
            player.isTransfer = false;
            if (player.team != null) {
                player.team.addPlayer(player);
            }
            return;
        }

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
            player.team.addPlayer(player);
        }
    }

    @Override
    public void updateSpinners() {
        // No spinner widgets on the desktop.
    }

    @Override
    public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {
        if (player == null) return;
        String teamName = player.team != null ? player.team.getName() : "Unknown";
        showInfo("Discipline",
                player.name + " (" + player.position + ", " + teamName + ")\n"
                        + "Issue: " + issue + "\n"
                        + "Suspended " + gamesA + " to " + gamesB + " games.");
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
        if (!canShowBlockingDialog()) {
            logDialog("Awards", summaryText);
            return;
        }
        SeasonAwardsDialog.show(owner, league, summaryText);
    }

    @Override
    public void showMidseasonSummary() {
        showScrollableText("Mid-Season Summary", buildMidseasonSummary());
    }

    @Override
    public void showSeasonSummary() {
        showScrollableText("Season Summary", league.seasonSummaryStr());
    }

    @Override
    public void showContractDialog() {
        if (!canShowBlockingDialog()) {
            return;
        }
        if (league.isCareerMode() && league.userTeam != null) {
            ContractDialog.show(owner, league);
        }
    }

    @Override
    public void showJobOffersDialog() {
        if (!canShowBlockingDialog()) {
            return;
        }
        if (league.isCareerMode()) {
            boolean accepted = JobOffersDialog.showJobOffers(owner, league);
            if (accepted) {
                CoordinatorHiringDialog.show(owner, league);
            }
        }
    }

    @Override
    public void showPromotionsDialog() {
        if (!canShowBlockingDialog()) {
            return;
        }
        if (league.isCareerMode()) {
            boolean accepted = JobOffersDialog.showPromotions(owner, league);
            if (accepted) {
                CoordinatorHiringDialog.show(owner, league);
            } else {
                CoordinatorHiringDialog.show(owner, league);
            }
        }
    }

    @Override
    public void showCoordinatorHiringDialog() {
        if (!canShowBlockingDialog()) {
            return;
        }
        if (league.userTeam == null || !league.userTeam.isUserControlled()) {
            return;
        }
        if (userTeamNeedsCoordinatorHire(league.userTeam)) {
            CoordinatorHiringDialog.show(owner, league);
        }
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
        if (!canShowBlockingDialog()) {
            return;
        }
        RedshirtDialog.show(owner, league);
    }

    @Override
    public void showTransferList() {
        if (!canShowBlockingDialog()) {
            return;
        }
        TransferPortalDialog.show(owner, league);
    }

    @Override
    public void showRealignmentSummary() {
        String news = league.newsRealignment;
        if (news == null || news.isEmpty()) {
            news = "No conference realignment occurred this off-season.";
        }
        showScrollableText("Conference Realignment", news);
    }

    @Override
    public void startRecruitingFlow() {
        if (awaitingDockedRecruiting) {
            return;
        }
        league.recruitPlayers();

        if (league.userTeam != null && league.userTeam.isUserControlled()) {
            awaitingDockedRecruiting = true;
            return;
        }

        newSeasonPending = true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean canShowBlockingDialog() {
        return owner != null && !suppressBlockingUi;
    }

    private void showInfo(String title, String message) {
        if (!canShowBlockingDialog()) {
            logDialog(title, message);
            return;
        }
        JOptionPane.showMessageDialog(owner, DesktopTheme.messageForDialog(message), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showScrollableText(String title, String text) {
        if (!canShowBlockingDialog()) {
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
        StringBuilder sb = new StringBuilder("Mid-Season Summary\n\n");
        if (league.userTeam != null) {
            simulation.Team t = league.userTeam;
            sb.append("Your team: ").append(t.getName())
              .append("  (").append(t.getWins()).append("-").append(t.getLosses()).append(")\n\n");
        }
        sb.append("Top 5 by prestige:\n");
        java.util.List<simulation.Team> teams = league.getTeamList();
        if (teams != null) {
            teams.stream()
                    .sorted(java.util.Comparator.comparingInt((simulation.Team t) -> t.getTeamPrestige()).reversed())
                    .limit(5)
                    .forEach(t -> sb.append("  ").append(t.getName())
                            .append("  ").append(t.getWins()).append("-").append(t.getLosses()).append("\n"));
        }
        return sb.toString();
    }
}
