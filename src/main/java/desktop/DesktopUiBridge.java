package desktop;

import simulation.GameUiBridge;
import simulation.League;
import simulation.Team;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
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
 */
public class DesktopUiBridge implements GameUiBridge {

    private final JFrame owner;
    private final League league;
    private boolean newSeasonPending = false;
    /** User must finish recruiting in {@link LeagueHomeView}'s Recruiting tab. */
    private boolean awaitingDockedRecruiting = false;

    public DesktopUiBridge(JFrame owner, League league) {
        this.owner = owner;
        this.league = league;
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
        if (recruitsData != null && !recruitsData.isEmpty()
                && league.userTeam != null) {
            league.userTeam.recruitPlayersFromStr(recruitsData);
            league.updateTeamTalentRatings();
        }
        newSeasonPending = true;
    }

    // -------------------------------------------------------------------------
    // GameUiBridge implementation
    // -------------------------------------------------------------------------

    @Override
    public void crash() {
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

        // Show accept/decline dialog for user team transfer offer
        int choice = javax.swing.JOptionPane.showOptionDialog(owner,
                DesktopTheme.messageForDialog(buildTransferOfferText(player)),
                "Transfer Offer: " + player.position + " " + player.name,
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Accept", "Decline"},
                "Accept");

        if (choice == 0) {
            // Accept
            league.userTransfers = league.userTransfers
                    + player.position + " " + player.name + " " + player.getYrStr()
                    + " Ovr: " + player.ratOvr + " (" + player.team.getName() + ")\n";
            league.sumTransfers = league.sumTransfers
                    + player.ratOvr + " " + player.position + " " + player.name
                    + " [" + player.getTransferStatus() + "] "
                    + league.userTeam.getName() + " (" + player.team.getAbbr() + ")";
            player.team = league.userTeam;
            league.userTeam.addPlayer(player);
        } else {
            // Decline — return player to their original team
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
        SeasonAwardsDialog.show(owner, league, summaryText);
    }

    @Override
    public void showMidseasonSummary() {
        String summary = buildMidseasonSummary();
        showScrollableText("Mid-Season Summary", summary);
    }

    @Override
    public void showSeasonSummary() {
        String summary = league.seasonSummaryStr();
        showScrollableText("Season Summary", summary);
    }

    @Override
    public void showContractDialog() {
        if (league.isCareerMode() && league.userTeam != null) {
            ContractDialog.show(owner, league);
        }
    }

    @Override
    public void showJobOffersDialog() {
        if (league.isCareerMode()) {
            boolean accepted = JobOffersDialog.showJobOffers(owner, league);
            if (accepted) {
                // After accepting a new job, hire coordinators for the new team
                CoordinatorHiringDialog.show(owner, league);
            }
        }
    }

    @Override
    public void showPromotionsDialog() {
        if (league.isCareerMode()) {
            boolean accepted = JobOffersDialog.showPromotions(owner, league);
            if (accepted) {
                league.coachCarousel();
                CoordinatorHiringDialog.show(owner, league);
            } else {
                // Still need to hire coordinators if contracts expired
                CoordinatorHiringDialog.show(owner, league);
            }
        }
    }

    @Override
    public void showRedshirtList() {
        RedshirtDialog.show(owner, league);
    }

    @Override
    public void showTransferList() {
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

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(owner, DesktopTheme.messageForDialog(message), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildTransferOfferText(positions.Player player) {
        return "Transfer " + (player.isTransfer ? player.getTransferStatus() : "") + " Request\n\n"
                + "Player: " + player.position + " " + player.name + "\n"
                + "Year: " + player.getYrStr() + "\n"
                + "Overall: " + player.ratOvr + "\n"
                + "From: " + (player.team != null ? player.team.getName() : "Unknown") + "\n\n"
                + "Accept this transfer to your roster?";
    }

    private void showScrollableText(String title, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        DesktopTheme.styleTextContent(area);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        scroll.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(owner, scroll, title, JOptionPane.INFORMATION_MESSAGE);
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
