package desktop;

import recruiting.RecruitingController;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingPresentation;
import recruiting.RecruitingSessionData;
import simulation.GameFlowManager;
import simulation.League;
import simulation.PlatformLog;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive recruiting UI for the desktop shell.  Mirrors the Android
 * {@code RecruitingActivity} workflow using the platform-agnostic
 * {@link RecruitingController} and {@link RecruitingSessionData}.
 *
 * <p>The dialog shows:
 * <ul>
 *   <li>Position/region filter combo box</li>
 *   <li>Recruit board table (position, name, stars, cost, overall)</li>
 *   <li>Detail panel with scouting grades</li>
 *   <li>Current roster summary</li>
 *   <li>Budget and recruited-players count</li>
 *   <li>Scout / Recruit / Done buttons</li>
 * </ul>
 */
public class RecruitingView extends JDialog {

    private static final String TAG = "RecruitingView";
    private static final String[] BOARD_COLUMNS = {"Pos", "Name", "Stars", "Cost", "OVR"};
    private static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    // Minimum roster sizes per position (used for need calculations)
    private static final int MIN_QBS = 2;
    private static final int MIN_RBS = 3;
    private static final int MIN_WRS = 4;
    private static final int MIN_TES = 2;
    private static final int MIN_OLS = 6;
    private static final int MIN_KS  = 1;
    private static final int MIN_DLS = 4;
    private static final int MIN_LBS = 4;
    private static final int MIN_CBS = 4;
    private static final int MIN_SS  = 2;

    /** Minimum total roster size used for budget bonuses. */
    private static final int MIN_ROSTER_SIZE = 55;
    /** Maximum roster size shown in recruit confirmation dialog. */
    private static final int MAX_ROSTER_SIZE = 70;

    private final League league;
    private final RecruitingController controller;
    private final RecruitingSessionData sessionData;
    private final RecruitingSessionData.PositionNeeds needs;
    private final ArrayList<String> positionLabels;

    private JComboBox<String> filterBox;
    private DefaultTableModel boardModel;
    private JTable boardTable;
    private JTextArea detailArea;
    private JLabel budgetLabel;
    private JLabel recruitedLabel;
    private JTextArea rosterArea;

    /** The current list backing the board table. */
    private List<RecruitingPlayerRecord> currentList;

    /** Set to true when the user clicks Done. */
    private boolean finished = false;

    /**
     * Creates the recruiting dialog.
     *
     * @param owner       parent frame
     * @param league      the active league (CPU teams are auto-recruited before this dialog opens)
     * @param flowManager flow manager to call {@code finishRecruiting()} on
     */
    public RecruitingView(JFrame owner, League league, GameFlowManager flowManager) {
        super(owner, "Recruiting — " + league.userTeam.getName(), true);
        this.league = league;
        this.sessionData = buildSessionData(league.userTeam);
        this.controller = new RecruitingController(sessionData, flowManager);
        this.needs = sessionData.calculateNeeds(MIN_QBS, MIN_RBS, MIN_WRS, MIN_TES, MIN_OLS,
                MIN_KS, MIN_DLS, MIN_LBS, MIN_CBS, MIN_SS);
        this.positionLabels = sessionData.buildPositionLabels(needs);

        sessionData.applyBudgetBonuses(MIN_ROSTER_SIZE);
        setSize(1100, 700);
        setLayout(new BorderLayout());

        add(buildToolBar(), BorderLayout.NORTH);
        add(buildCenterContent(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        loadBoard(0);
    }

    // -------------------------------------------------------------------------
    // Build session data from live team (mirrors Android's beginRecruiting)
    // -------------------------------------------------------------------------

    private static RecruitingSessionData buildSessionData(Team userTeam) {
        StringBuilder sb = new StringBuilder();
        userTeam.sortPlayers();
        sb.append(userTeam.getConference()).append(",")
          .append(userTeam.getName()).append(",")
          .append(userTeam.getAbbr()).append(",")
          .append(userTeam.getUserRecruitBudget()).append(",")
          .append(userTeam.getHeadCoach().ratTalent).append("%\n");
        sb.append(userTeam.getPlayerInfoSaveFile());
        sb.append("END_TEAM_INFO%\n");
        sb.append(userTeam.getRecruitsInfoSaveFile());
        return RecruitingSessionData.fromUserTeamInfo(sb.toString());
    }

    // -------------------------------------------------------------------------
    // Toolbar (filter + sort)
    // -------------------------------------------------------------------------

    private JPanel buildToolBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        bar.add(new JLabel("Filter:"));
        filterBox = new JComboBox<>(positionLabels.toArray(new String[0]));
        filterBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        filterBox.addActionListener(e -> loadBoard(filterBox.getSelectedIndex()));
        bar.add(filterBox);

        JButton sortGradeBtn = new JButton("Sort by Grade");
        sortGradeBtn.addActionListener(e -> {
            controller.sortByGrade();
            loadBoard(filterBox.getSelectedIndex());
        });
        bar.add(sortGradeBtn);

        JButton sortCostBtn = new JButton("Sort by Cost");
        sortCostBtn.addActionListener(e -> {
            controller.sortByCost();
            loadBoard(filterBox.getSelectedIndex());
        });
        bar.add(sortCostBtn);

        budgetLabel = new JLabel();
        budgetLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        bar.add(budgetLabel);

        recruitedLabel = new JLabel();
        recruitedLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        bar.add(recruitedLabel);

        return bar;
    }

    // -------------------------------------------------------------------------
    // Center content (board table + detail + roster)
    // -------------------------------------------------------------------------

    private JSplitPane buildCenterContent() {
        // Left: recruit board table
        boardModel = new DefaultTableModel(BOARD_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 2, 3, 4 -> Integer.class;
                    default -> String.class;
                };
            }
        };
        boardTable = new JTable(boardModel);
        boardTable.setRowHeight(22);
        boardTable.setAutoCreateRowSorter(true);
        boardTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardTable.setFillsViewportHeight(true);
        boardTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showRecruitDetail();
        });

        JScrollPane tableScroll = new JScrollPane(boardTable);
        tableScroll.setPreferredSize(new Dimension(550, 0));

        // Right: detail + roster
        JPanel rightPanel = new JPanel(new BorderLayout(0, 6));

        detailArea = new JTextArea("Select a recruit to view details.");
        detailArea.setEditable(false);
        detailArea.setFont(MONO);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Recruit Details"));
        detailScroll.setPreferredSize(new Dimension(400, 250));

        rosterArea = new JTextArea();
        rosterArea.setEditable(false);
        rosterArea.setFont(MONO);
        JScrollPane rosterScroll = new JScrollPane(rosterArea);
        rosterScroll.setBorder(BorderFactory.createTitledBorder("Current Roster"));

        rightPanel.add(detailScroll, BorderLayout.NORTH);
        rightPanel.add(rosterScroll, BorderLayout.CENTER);

        // Action buttons
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JButton scoutBtn = new JButton("Scout (10% cost)");
        scoutBtn.addActionListener(e -> scoutSelected());
        actionPanel.add(scoutBtn);

        JButton recruitBtn = new JButton("Recruit");
        recruitBtn.addActionListener(e -> recruitSelected());
        actionPanel.add(recruitBtn);

        rightPanel.add(actionPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, rightPanel);
        split.setDividerLocation(550);
        return split;
    }

    // -------------------------------------------------------------------------
    // Bottom bar (done)
    // -------------------------------------------------------------------------

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));

        JButton doneBtn = new JButton("Finish Recruiting");
        doneBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        doneBtn.addActionListener(e -> finishRecruiting());
        bar.add(doneBtn);
        return bar;
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private void loadBoard(int filterIndex) {
        String label = positionLabels.get(filterIndex);
        currentList = controller.getPlayersForPosition(filterIndex, label);

        boardModel.setRowCount(0);
        for (RecruitingPlayerRecord r : currentList) {
            boardModel.addRow(new Object[]{
                    r.position(),
                    r.name(),
                    r.stars(),
                    r.cost(),
                    r.recruitOverall()
            });
        }

        updateLabels();
        updateRoster();
    }

    private void updateLabels() {
        budgetLabel.setText("  Budget: $" + sessionData.recruitingBudget);
        recruitedLabel.setText("  Recruited: " + sessionData.playersRecruited.size());

        // Refresh filter labels to update counts
        RecruitingSessionData.PositionNeeds currentNeeds =
                sessionData.calculateNeeds(MIN_QBS, MIN_RBS, MIN_WRS, MIN_TES, MIN_OLS,
                        MIN_KS, MIN_DLS, MIN_LBS, MIN_CBS, MIN_SS);
        ArrayList<String> newLabels = sessionData.buildPositionLabels(currentNeeds);
        int sel = filterBox.getSelectedIndex();
        filterBox.removeAllItems();
        for (String l : newLabels) {
            filterBox.addItem(l);
        }
        if (sel >= 0 && sel < filterBox.getItemCount()) {
            filterBox.setSelectedIndex(sel);
        }
    }

    private void updateRoster() {
        RecruitingSessionData.PositionNeeds currentNeeds =
                sessionData.calculateNeeds(MIN_QBS, MIN_RBS, MIN_WRS, MIN_TES, MIN_OLS,
                        MIN_KS, MIN_DLS, MIN_LBS, MIN_CBS, MIN_SS);
        rosterArea.setText(RecruitingPresentation.buildRosterText(sessionData, currentNeeds));
        rosterArea.setCaretPosition(0);
    }

    // -------------------------------------------------------------------------
    // Recruit detail
    // -------------------------------------------------------------------------

    private void showRecruitDetail() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            detailArea.setText("Select a recruit to view details.");
            return;
        }
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) return;

        RecruitingPlayerRecord recruit = currentList.get(modelRow);
        String pos = recruit.position();

        StringBuilder sb = new StringBuilder();
        sb.append(recruit.name()).append("  (").append(pos).append(")\n");
        sb.append("Stars: ").append(RecruitingPresentation.getPlayerListRightLabel(recruit)).append("\n");
        sb.append("Cost: $").append(recruit.cost()).append("\n");
        sb.append("Overall: ").append(recruit.recruitOverall()).append("\n\n");
        sb.append(RecruitingPresentation.buildRecruitBoardDetails(recruit, pos)).append("\n\n");
        sb.append(RecruitingPresentation.buildPotentialDetails(recruit));
        if (recruit.isTransfer()) sb.append("\n\n[TRANSFER]");

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    private void scoutSelected() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) return;
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) return;

        RecruitingPlayerRecord recruit = currentList.get(modelRow);
        boolean ok = controller.scoutPlayer(recruit);
        if (!ok) {
            JOptionPane.showMessageDialog(this,
                    "Not enough budget to scout this player.\nScouting costs 10% of the recruit price.",
                    "Cannot Scout", JOptionPane.WARNING_MESSAGE);
            return;
        }
        loadBoard(filterBox.getSelectedIndex());
        showRecruitDetail();
    }

    private void recruitSelected() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) return;
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) return;

        RecruitingPlayerRecord recruit = currentList.get(modelRow);

        if (recruit.cost() > sessionData.recruitingBudget) {
            JOptionPane.showMessageDialog(this,
                    "Not enough budget ($" + sessionData.recruitingBudget + ") to recruit "
                            + recruit.name() + " ($" + recruit.cost() + ").",
                    "Cannot Recruit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String msg = RecruitingPresentation.buildRecruitConfirmMessage(sessionData, MAX_ROSTER_SIZE, recruit);
        int choice = JOptionPane.showConfirmDialog(this, msg, "Confirm Recruit", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        controller.recruitPlayer(recruit, false);
        PlatformLog.i(TAG, "Recruited " + recruit.position() + " " + recruit.name()
                + " for $" + recruit.cost());

        loadBoard(filterBox.getSelectedIndex());
    }

    private void finishRecruiting() {
        String exitMsg = RecruitingPresentation.buildExitConfirmMessage(positionLabels);
        int choice = JOptionPane.showConfirmDialog(this, exitMsg,
                "Finish Recruiting?", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        finished = true;
        dispose();
    }

    // -------------------------------------------------------------------------
    // Result
    // -------------------------------------------------------------------------

    /** True if the user completed recruiting (not cancelled). */
    public boolean isFinished() {
        return finished;
    }

    /** Returns the serialized recruit data to pass to the league engine. */
    public String getRecruitsSaveData() {
        return sessionData.buildRecruitsSaveData();
    }

    /**
     * Shows the recruiting UI and blocks until the user finishes or closes.
     *
     * @param owner  parent frame
     * @param league the active league (CPU teams should already be auto-recruited)
     * @return the recruits save data string, or null if the user cancelled
     */
    public static String showRecruiting(JFrame owner, League league) {
        // No-op flow manager — desktop handles the result inline
        GameFlowManager noOpFlow = new GameFlowManager() {
            @Override public void startNewGame(simulation.LeagueLaunchCoordinator.LaunchRequest.PrestigeMode p, String u) {}
            @Override public void loadGame(String s) {}
            @Override public void importSave(String u) {}
            @Override public void finishRecruiting(String r) {}
            @Override public void startRecruiting(String u) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void returnToMainHub() {}
        };

        RecruitingView view = new RecruitingView(owner, league, noOpFlow);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);

        if (view.isFinished()) {
            return view.getRecruitsSaveData();
        }
        return null;
    }
}
