package desktop;

import simulation.League;
import simulation.Team;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Interactive dialog for coaching job offers during the offseason.
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>Job Offers (fired/resigned)</b> — the user's coach was fired or
 *       left; they must pick a new team from available vacancies.</li>
 *   <li><b>Promotions</b> — the user's coach has drawn interest from other
 *       programs; they may accept or decline a move to a better job.</li>
 * </ul>
 *
 * <p>The user can view each team's roster/prestige before accepting.
 */
public class JobOffersDialog extends JDialog {

    private static final String[] COLUMNS = {"Team", "Prestige", "Conf", "Off Tal", "Def Tal", "Fit Req"};
    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final DecimalFormat DF = new DecimalFormat("#0.0");

    private final JFrame ownerFrame;
    private final League league;
    private final HeadCoach userHC;
    private final boolean isPromotion;

    /** Set to true if the user accepted a new job. */
    private boolean accepted = false;

    public JobOffersDialog(JFrame owner, League league, boolean isPromotion) {
        super(owner, isPromotion ? "Career Advancement Opportunities" : "Head Coach Opportunities", true);
        this.ownerFrame = owner;
        this.league = league;
        this.isPromotion = isPromotion;
        this.userHC = league.userTeam != null ? league.userTeam.getHeadCoach() : null;
        setSize(800, 550);
        setLayout(new BorderLayout());

        if (!league.isCareerMode() || userHC == null) {
            buildNoCareerPanel();
            return;
        }

        ArrayList<Team> vacancies = getVacancies();

        if (vacancies.isEmpty()) {
            buildNoOffersPanel();
        } else {
            buildOffersPanel(vacancies);
        }
    }

    private ArrayList<Team> getVacancies() {
        int ratOvr = userHC.getStaffOverall(userHC.overallWt);
        if (ratOvr < 40) ratOvr = 40;
        String oldTeam = userHC.team != null ? userHC.team.getName() : "NO TEAM";

        if (isPromotion) {
            if (!userHC.promotionCandidate) return new ArrayList<>();
            return league.getCoachPromotionList(ratOvr, 2.0, oldTeam);
        } else {
            return league.getCoachListFired(ratOvr, oldTeam);
        }
    }

    private void buildNoCareerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(new JLabel("Career mode is not active. Job offers are only available in career mode.",
                JLabel.CENTER), BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        JPanel bottom = new JPanel();
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void buildNoOffersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        String message = isPromotion
                ? "No job offers are available this offseason.\n"
                  + "Your program did not build enough momentum to put you on other schools' short lists."
                : "No immediate head coach openings are available right now.\n"
                  + "You will remain on the market until a program makes a move.";
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setFont(BODY_FONT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            if (isPromotion) userHC.promotionCandidate = false;
            dispose();
        });
        JPanel bottom = new JPanel();
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void buildOffersPanel(ArrayList<Team> vacancies) {
        // Left: team list table
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Team t : vacancies) {
            model.addRow(new Object[]{
                    t.getName(),
                    t.getTeamPrestige() + " (#" + t.getRankTeamPrestige() + ")",
                    t.getConference(),
                    DF.format(t.getTeamOffTalent()),
                    DF.format(t.getTeamDefTalent()),
                    String.valueOf(t.getMinCoachHireReq())
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(BODY_FONT);

        // Right: team detail when selected
        JTextArea detail = new JTextArea("Select a team to view details.");
        detail.setEditable(false);
        detail.setFont(BODY_FONT);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < vacancies.size()) {
                    detail.setText(buildTeamSummary(vacancies.get(row)));
                    detail.setCaretPosition(0);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < vacancies.size()) {
                        confirmAcceptJob(vacancies.get(row));
                    }
                }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), new JScrollPane(detail));
        split.setDividerLocation(420);

        // Header
        JLabel header = new JLabel(isPromotion
                ? "  Programs are interested in your work. Review and decide."
                : "  Review each opening before you decide.");
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton acceptBtn = new JButton("Accept Selected Job");
        acceptBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < vacancies.size()) {
                confirmAcceptJob(vacancies.get(row));
            } else {
                JOptionPane.showMessageDialog(this, "Select a team first.", "No Selection",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        JButton declineBtn = new JButton(isPromotion ? "Decline All Offers" : "Stay on Market");
        declineBtn.addActionListener(e -> {
            if (isPromotion) userHC.promotionCandidate = false;
            dispose();
        });

        buttons.add(acceptBtn);
        buttons.add(declineBtn);

        add(header, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private String buildTeamSummary(Team team) {
        int prestigeDelta = team.getTeamPrestige() - userHC.baselinePrestige;
        String direction = prestigeDelta > 0 ? "+" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("Program Snapshot: ").append(team.getName()).append("\n");
        sb.append("═".repeat(40)).append("\n\n");
        sb.append("Prestige: #").append(team.getRankTeamPrestige())
          .append(" (").append(team.getTeamPrestige()).append(")\n");
        sb.append("Conference: ").append(team.getConference())
          .append(" (Prestige ").append(team.getConfPrestige()).append(")\n\n");
        sb.append("Roster Talent:\n");
        sb.append("  Offense: ").append(DF.format(team.getTeamOffTalent())).append("\n");
        sb.append("  Defense: ").append(DF.format(team.getTeamDefTalent())).append("\n");
        sb.append("  Combined: ").append(DF.format(team.getTeamOffTalent() + team.getTeamDefTalent())).append("\n\n");
        sb.append("Coach Fit Threshold: ").append(team.getMinCoachHireReq()).append("\n");
        sb.append("Career Swing: ").append(direction).append(prestigeDelta).append(" prestige\n\n");
        sb.append("If you accept, your head coach contract resets to a\n");
        sb.append("fresh 6-year deal and your AD expectations will be\n");
        sb.append("recalibrated to this program.\n\n");

        // Show roster summary
        sb.append("--- Projected Roster ---\n");
        String[] roster = team.getTeamRosterString();
        if (roster != null) {
            for (String s : roster) {
                if (s != null) sb.append(s).append("\n");
            }
        }
        return sb.toString();
    }

    private void confirmAcceptJob(Team selectedTeam) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Accept the head coaching position at " + selectedTeam.getName() + "?\n\n"
                        + "Your contract will reset to 6 years.\n"
                        + "Baseline prestige set to " + selectedTeam.getTeamPrestige() + ".",
                "Confirm Job Change", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            changeTeams(selectedTeam);
            accepted = true;
            dispose();
        }
    }

    /**
     * Performs the actual team swap, mirroring the Android {@code changeTeams()} logic.
     */
    private void changeTeams(Team newTeam) {
        Team oldTeam = league.userTeam;
        oldTeam.newCoachTeamChanges();
        oldTeam.setUserControlled(false);
        oldTeam.setHeadCoach(null);
        league.coachHiringSingleTeam(oldTeam);

        league.newJobtransfer(newTeam.getName());
        Team freshTeam = league.userTeam; // updated by newJobtransfer

        freshTeam.setHeadCoach(userHC);
        userHC.team = freshTeam;
        freshTeam.setFired(false);
        userHC.contractYear = 0;
        userHC.contractLength = 6;
        userHC.baselinePrestige = freshTeam.getTeamPrestige();
        userHC.promotionCandidate = false;

        league.getNewsStories().get(league.currentWeek + 1).add(
                "Coaching Hire: " + freshTeam.getName()
                        + ">After an extensive search for a new head coach, "
                        + freshTeam.getName() + " has hired " + userHC.name
                        + " to lead the team.");
    }

    public boolean wasAccepted() {
        return accepted;
    }

    /**
     * Shows the job offers dialog (fired/resigned mode) and blocks until closed.
     * Returns true if a new job was accepted.
     */
    public static boolean showJobOffers(JFrame owner, League league) {
        JobOffersDialog dlg = new JobOffersDialog(owner, league, false);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        return dlg.wasAccepted();
    }

    /**
     * Shows the promotions dialog and blocks until closed.
     * Returns true if a promotion was accepted.
     */
    public static boolean showPromotions(JFrame owner, League league) {
        JobOffersDialog dlg = new JobOffersDialog(owner, league, true);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        return dlg.wasAccepted();
    }
}
