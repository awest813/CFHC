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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Interactive dialog for coaching job offers during the offseason.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class JobOffersDialog extends JDialog {

    private static final Color BG_COLOR = new Color(15, 20, 28);
    private static final Color SURFACE_COLOR = new Color(25, 32, 45);
    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color DANGER_RED = new Color(231, 76, 60);
    private static final Color TEXT_SECONDARY = new Color(171, 178, 191);

    private static final String[] COLUMNS = {"Team", "Prestige", "Conf", "Off Tal", "Def Tal", "Fit Req"};
    private static final DecimalFormat DF = new DecimalFormat("#0.0");
    private static final int MIN_COACH_RATING = 40;
    private static final int NEW_JOB_CONTRACT_LENGTH = 6;

    private final JFrame ownerFrame;
    private final League league;
    private final HeadCoach userHC;
    private final boolean isPromotion;

    /** Set to true if the user accepted a new job. */
    private boolean accepted = false;

    public JobOffersDialog(JFrame owner, League league, boolean isPromotion) {
        super(owner, isPromotion ? "CAREER ADVANCEMENT OPPORTUNITIES" : "HEAD COACH OPPORTUNITIES", true);
        this.ownerFrame = owner;
        this.league = league;
        this.isPromotion = isPromotion;
        this.userHC = league.userTeam != null ? league.userTeam.getHeadCoach() : null;
        setSize(1000, 650);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

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
        if (ratOvr < MIN_COACH_RATING) ratOvr = MIN_COACH_RATING;
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
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));
        
        JLabel msg = new JLabel("CAREER MODE DEACTIVATED", JLabel.CENTER);
        msg.setFont(new Font("SansSerif", Font.BOLD, 20));
        msg.setForeground(DANGER_RED);
        panel.add(msg, BorderLayout.NORTH);
        
        JLabel desc = new JLabel("<html><center>Job offers and career transitions are only available in a persistent career universe.</center></html>", JLabel.CENTER);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 14));
        desc.setForeground(TEXT_SECONDARY);
        panel.add(desc, BorderLayout.CENTER);

        JButton ok = createGlassButton("RETURN TO HUB", ACCENT_BLUE);
        ok.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void buildNoOffersPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));
        
        JLabel msg = new JLabel("NO ACTIVE OPPORTUNITIES", JLabel.CENTER);
        msg.setFont(new Font("SansSerif", Font.BOLD, 20));
        msg.setForeground(TEXT_SECONDARY);
        panel.add(msg, BorderLayout.NORTH);
        
        String message = isPromotion
                ? "Your program has not built sufficient momentum this season to trigger external interest. Programs are currently prioritizing other candidates."
                : "No programs are currently extending invitations for vacancy interviews. You will remain on the open market for the next cycle.";
        
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setFont(new Font("Serif", Font.ITALIC, 18));
        area.setForeground(new Color(255, 255, 255, 180));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        panel.add(area, BorderLayout.CENTER);

        JButton ok = createGlassButton("ACKNOWLEDGE", ACCENT_BLUE);
        ok.addActionListener(e -> {
            if (isPromotion) userHC.promotionCandidate = false;
            dispose();
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void buildOffersPanel(ArrayList<Team> vacancies) {
        // Left: team list table
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 1) return Integer.class;
                return String.class;
            }
        };

        for (Team t : vacancies) {
            model.addRow(new Object[]{
                    t.getName().toUpperCase(),
                    t.getTeamPrestige(),
                    t.getConference().toUpperCase(),
                    DF.format(t.getTeamOffTalent()),
                    DF.format(t.getTeamDefTalent()),
                    t.getMinCoachHireReq()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(38);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(BG_COLOR);
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(255, 255, 255, 10));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(ACCENT_BLUE);
        
        table.getTableHeader().setBackground(SURFACE_COLOR);
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 10)));

        // Right side: Program Snapshot Card
        JPanel detailPanel = new JPanel(new BorderLayout(15, 15));
        detailPanel.setBackground(SURFACE_COLOR);
        detailPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel snapshotTitle = new JLabel("PROGRAM INTELLIGENCE");
        snapshotTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        snapshotTitle.setForeground(ACCENT_BLUE);
        detailPanel.add(snapshotTitle, BorderLayout.NORTH);

        JTextArea detail = new JTextArea("SELECT A PROGRAM TO VIEW PERSONNEL EVALUATIONS AND ALUMNI EXPECTATIONS.");
        detail.setEditable(false);
        detail.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detail.setBackground(BG_COLOR);
        detail.setForeground(TEXT_SECONDARY);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JScrollPane detailScroll = new JScrollPane(detail);
        detailScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 10)));
        detailPanel.add(detailScroll, BorderLayout.CENTER);

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
                new JScrollPane(table) {{ setBorder(BorderFactory.createEmptyBorder()); setViewportBackground(BG_COLOR); }}, 
                detailPanel);
        split.setDividerLocation(550);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(5);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        headerPanel.setBackground(SURFACE_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        JLabel headerLabel = new JLabel(isPromotion ? "CAREER ADVANCEMENT PROTOCOL" : "JOB OPPORTUNITY REGISTRY");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.WEST);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        buttons.setBackground(SURFACE_COLOR);
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)));
        
        JButton declineBtn = createGlassButton(isPromotion ? "DECLINE ALL OFFERS" : "DECLINE OPPORTUNITY", DANGER_RED);
        declineBtn.addActionListener(e -> {
            if (isPromotion) userHC.promotionCandidate = false;
            dispose();
        });

        JButton acceptBtn = createGlassButton("ACCEPT CONTRACT", SUCCESS_GREEN);
        acceptBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0 && row < vacancies.size()) {
                confirmAcceptJob(vacancies.get(row));
            } else {
                JOptionPane.showMessageDialog(this, "Select a program first.", "No Selection",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        buttons.add(declineBtn);
        buttons.add(acceptBtn);

        add(headerPanel, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private JButton createGlassButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        return btn;
    }

    private String buildTeamSummary(Team team) {
        int prestigeDelta = team.getTeamPrestige() - userHC.baselinePrestige;
        String direction = prestigeDelta > 0 ? "+" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("PROGRAM SNAPSHOT: ").append(team.getName().toUpperCase()).append("\n");
        sb.append("═".repeat(45)).append("\n\n");
        sb.append("PRESTIGE: #").append(team.getRankTeamPrestige())
          .append(" (").append(team.getTeamPrestige()).append(")\n");
        sb.append("CONFERENCE: ").append(team.getConference().toUpperCase())
          .append(" (CONF PRESTIGE: ").append(team.getConfPrestige()).append(")\n\n");
        sb.append("ROSTER EVALUATION:\n");
        sb.append("  OFFENSIVE TALENT: ").append(DF.format(team.getTeamOffTalent())).append("\n");
        sb.append("  DEFENSIVE TALENT: ").append(DF.format(team.getTeamDefTalent())).append("\n");
        sb.append("  COMPOSITE SCORE:  ").append(DF.format(team.getTeamOffTalent() + team.getTeamDefTalent())).append("\n\n");
        sb.append("FIT THRESHOLD: ").append(team.getMinCoachHireReq()).append("\n");
        sb.append("PRESTIGE SWING: ").append(direction).append(prestigeDelta).append(" PT\n\n");
        sb.append("TERMS: If accepted, your contract resets to a fresh\n");
        sb.append(NEW_JOB_CONTRACT_LENGTH).append("-YEAR DEAL. AD expectations will be calibrated\n");
        sb.append("to this program's historical baseline.\n\n");

        sb.append("--- PROJECTED DEPTH CHART ---\n");
        String[] roster = team.getTeamRosterString();
        if (roster != null) {
            for (String s : roster) {
                if (s != null) sb.append(s.toUpperCase()).append("\n");
            }
        }
        return sb.toString();
    }

    private void confirmAcceptJob(Team selectedTeam) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "ACCEPT THE HEAD COACHING POSITION AT " + selectedTeam.getName().toUpperCase() + "?\n\n"
                        + "CONTRACT RESET: " + NEW_JOB_CONTRACT_LENGTH + " YEARS.\n"
                        + "BASELINE PRESTIGE: " + selectedTeam.getTeamPrestige() + ".",
                "CONFIRM CAREER TRANSITION", JOptionPane.YES_NO_OPTION);
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
        userHC.contractLength = NEW_JOB_CONTRACT_LENGTH;
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
