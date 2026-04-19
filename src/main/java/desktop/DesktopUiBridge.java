package desktop;

import simulation.GameUiBridge;
import simulation.League;
import simulation.Team;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

/**
 * Desktop implementation of {@link GameUiBridge}.  Each callback is handled
 * with a lightweight Swing dialog so that the full season + offseason loop can
 * run without any Android dependencies.
 *
 * <p>After {@link #startRecruitingFlow()} fires the bridge auto-recruits every
 * team and sets {@link #isNewSeasonPending()} to {@code true} so that
 * {@link LeagueHomeView} can call {@link League#startNextSeason()} and wire up
 * a new {@link simulation.SeasonController}.
 */
public class DesktopUiBridge implements GameUiBridge {

    private static final String TRANSFER_PORTAL_HEADER = "Transfer Portal:\n\n";

    private final JFrame owner;
    private final League league;
    private boolean newSeasonPending = false;

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

    // -------------------------------------------------------------------------
    // GameUiBridge implementation
    // -------------------------------------------------------------------------

    @Override
    public void crash() {
        JOptionPane.showMessageDialog(owner,
                "A fatal simulation error occurred.",
                "Simulation Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void startRecruiting(File saveFile, Team userTeam) throws InterruptedException, IOException {
        // Desktop uses auto-recruiting; this path is not reached via SeasonController.
    }

    @Override
    public void transferPlayer(positions.Player player) {
        // Auto-handled by League.transferPlayers() during the offseason.
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
        showScrollableText("End-of-Season Awards", summaryText);
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
        if (league.userTeam != null) {
            showInfo("Contract Status",
                    "Off-Season: Contract review is in progress for your staff.\n\n"
                            + league.userTeam.getContractString());
        }
    }

    @Override
    public void showJobOffersDialog() {
        showInfo("Job Openings",
                "Off-Season: Coaching job openings across the league are being processed.\n"
                        + "CPU teams will fill vacant head-coach positions automatically.");
    }

    @Override
    public void showPromotionsDialog() {
        showInfo("Coordinator Changes",
                "Off-Season: Offensive and defensive coordinator contracts are being evaluated.\n"
                        + "CPU coordinators will be re-assigned automatically.");
    }

    @Override
    public void showRedshirtList() {
        java.util.List<positions.Player> redshirts = league.getRedshirts();
        if (redshirts == null || redshirts.isEmpty()) {
            showInfo("Redshirt List", "No players are currently on the redshirt list.");
            return;
        }
        StringBuilder sb = new StringBuilder("Players on the redshirt list:\n\n");
        for (positions.Player p : redshirts) {
            if (p == null) continue;
            String teamName = p.team != null ? p.team.getName() : "";
            sb.append(p.position).append("  ").append(p.name);
            if (!teamName.isEmpty()) sb.append("  (").append(teamName).append(")");
            sb.append("\n");
        }
        showScrollableText("Redshirt List", sb.toString());
    }

    @Override
    public void showTransferList() {
        StringBuilder sb = new StringBuilder(TRANSFER_PORTAL_HEADER);
        appendTransferGroup(sb, "QB", league.getTransferQBs());
        appendTransferGroup(sb, "RB", league.getTransferRBs());
        appendTransferGroup(sb, "WR", league.getTransferWRs());
        appendTransferGroup(sb, "TE", league.getTransferTEs());
        appendTransferGroup(sb, "OL", league.getTransferOLs());
        appendTransferGroup(sb, "K",  league.getTransferKs());
        appendTransferGroup(sb, "DL", league.getTransferDLs());
        appendTransferGroup(sb, "LB", league.getTransferLBs());
        appendTransferGroup(sb, "CB", league.getTransferCBs());
        appendTransferGroup(sb, "S",  league.getTransferSs());
        if (sb.length() == TRANSFER_PORTAL_HEADER.length()) {
            sb.append("No players currently in the transfer portal.");
        }
        showScrollableText("Transfer Portal", sb.toString());
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
        // Auto-recruit all CPU teams (including the user's team in desktop mode).
        league.recruitPlayers();
        newSeasonPending = true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void showInfo(String title, String message) {
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showScrollableText(String title, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
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

    private <T extends positions.Player> void appendTransferGroup(
            StringBuilder sb, String label, java.util.List<T> list) {
        if (list == null || list.isEmpty()) return;
        for (T p : list) {
            sb.append(label).append("  ").append(p.name)
              .append("  OVR ").append(p.ratOvr).append("\n");
        }
    }
}
