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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Embeddable recruiting UI (board, scout, recruit, finish) shared by the docked
 * {@link LeagueHomeView} tab and the legacy {@link RecruitingView} dialog.
 */
public class RecruitingPanel extends JPanel {

    private static final String TAG = "RecruitingPanel";
    private static final String[] BOARD_COLUMNS = {"Pos", "Name", "Stars", "Cost", "OVR"};
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private static final int MIN_QBS = 2;
    private static final int MIN_RBS = 3;
    private static final int MIN_WRS = 4;
    private static final int MIN_TES = 2;
    private static final int MIN_OLS = 6;
    private static final int MIN_KS = 1;
    private static final int MIN_DLS = 4;
    private static final int MIN_LBS = 4;
    private static final int MIN_CBS = 4;
    private static final int MIN_SS = 2;

    private static final int MIN_ROSTER_SIZE = 55;
    private static final int MAX_ROSTER_SIZE = 70;

    private final RecruitingController controller;
    private final RecruitingSessionData sessionData;
    private final ArrayList<String> positionLabels;
    private final Consumer<String> onFinish;

    private JComboBox<String> filterBox;
    private DefaultTableModel boardModel;
    private JTable boardTable;
    private JTextArea detailArea;
    private JLabel budgetLabel;
    private JLabel recruitedLabel;
    private JTextArea rosterArea;

    private List<RecruitingPlayerRecord> currentList;

    /**
     * @param league   active league (CPU teams should already be auto-recruited)
     * @param onFinish called on EDT after the user confirms finish; argument is
     *                 serialized recruit data (may be empty if they signed nobody)
     */
    public RecruitingPanel(League league, Consumer<String> onFinish) {
        super(new BorderLayout());
        this.onFinish = onFinish;
        this.sessionData = buildSessionData(league.userTeam);
        GameFlowManager noOpFlow = new GameFlowManager() {
            @Override public void startNewGame(simulation.LeagueLaunchCoordinator.LaunchRequest.PrestigeMode p, String u) {}
            @Override public void loadGame(String s) {}
            @Override public void importSave(String u) {}
            @Override public void finishRecruiting(String r) {}
            @Override public void startRecruiting(String u) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void returnToMainHub() {}
        };
        this.controller = new RecruitingController(sessionData, noOpFlow);
        RecruitingSessionData.PositionNeeds needs = sessionData.calculateNeeds(MIN_QBS, MIN_RBS, MIN_WRS, MIN_TES, MIN_OLS,
                MIN_KS, MIN_DLS, MIN_LBS, MIN_CBS, MIN_SS);
        this.positionLabels = sessionData.buildPositionLabels(needs);

        sessionData.applyBudgetBonuses(MIN_ROSTER_SIZE);

        setOpaque(true);
        setBackground(DesktopTheme.windowBackground());

        add(buildToolBar(), BorderLayout.NORTH);
        add(buildCenterContent(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        loadBoard(0);
    }

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

    private JPanel buildToolBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(true);
        bar.setBackground(DesktopTheme.windowBackground());
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setForeground(DesktopTheme.textPrimary());
        bar.add(filterLbl);
        filterBox = new JComboBox<>(positionLabels.toArray(new String[0]));
        filterBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
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
        budgetLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        bar.add(budgetLabel);

        recruitedLabel = new JLabel();
        recruitedLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        bar.add(recruitedLabel);

        return bar;
    }

    private JSplitPane buildCenterContent() {
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
            if (!e.getValueIsAdjusting()) {
                showRecruitDetail();
            }
        });

        JScrollPane tableScroll = new JScrollPane(boardTable);
        StripedRowRenderer.install(boardTable);
        DesktopTheme.styleDataTableInScroll(tableScroll, boardTable);
        tableScroll.setPreferredSize(new Dimension(550, 0));

        JPanel rightPanel = new JPanel(new BorderLayout(0, 6));
        rightPanel.setOpaque(true);
        rightPanel.setBackground(DesktopTheme.windowBackground());

        detailArea = new JTextArea("Select a recruit to view details.");
        detailArea.setEditable(false);
        detailArea.setFont(MONO);
        DesktopTheme.styleTextContent(detailArea);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        TitledBorder detailTitle = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()), "Recruit Details");
        detailTitle.setTitleColor(DesktopTheme.textPrimary());
        detailScroll.setBorder(detailTitle);
        detailScroll.setPreferredSize(new Dimension(400, 250));

        rosterArea = new JTextArea();
        rosterArea.setEditable(false);
        rosterArea.setFont(MONO);
        DesktopTheme.styleTextContent(rosterArea);
        JScrollPane rosterScroll = new JScrollPane(rosterArea);
        rosterScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        TitledBorder rosterTitle = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()), "Current Roster");
        rosterTitle.setTitleColor(DesktopTheme.textPrimary());
        rosterScroll.setBorder(rosterTitle);

        rightPanel.add(detailScroll, BorderLayout.NORTH);
        rightPanel.add(rosterScroll, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        actionPanel.setOpaque(true);
        actionPanel.setBackground(DesktopTheme.windowBackground());
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
        split.setOpaque(true);
        split.setBackground(DesktopTheme.windowBackground());
        return split;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        bar.setOpaque(true);
        bar.setBackground(DesktopTheme.windowBackground());

        JButton doneBtn = new JButton("Finish Recruiting");
        doneBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        doneBtn.addActionListener(e -> finishRecruiting());
        bar.add(doneBtn);
        return bar;
    }

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
        budgetLabel.setForeground(DesktopTheme.textPrimary());
        recruitedLabel.setText("  Recruited: " + sessionData.playersRecruited.size());
        recruitedLabel.setForeground(DesktopTheme.textSecondary());

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

    private void showRecruitDetail() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            detailArea.setText("Select a recruit to view details.");
            return;
        }
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) {
            return;
        }

        RecruitingPlayerRecord recruit = currentList.get(modelRow);
        String pos = recruit.position();

        StringBuilder sb = new StringBuilder();
        sb.append(recruit.name()).append("  (").append(pos).append(")\n");
        sb.append("Stars: ").append(RecruitingPresentation.getPlayerListRightLabel(recruit)).append("\n");
        sb.append("Cost: $").append(recruit.cost()).append("\n");
        sb.append("Overall: ").append(recruit.recruitOverall()).append("\n\n");
        sb.append(RecruitingPresentation.buildRecruitBoardDetails(recruit, pos)).append("\n\n");
        sb.append(RecruitingPresentation.buildPotentialDetails(recruit));
        if (recruit.isTransfer()) {
            sb.append("\n\n[TRANSFER]");
        }

        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private void scoutSelected() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            return;
        }
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) {
            return;
        }

        RecruitingPlayerRecord recruit = currentList.get(modelRow);
        boolean ok = controller.scoutPlayer(recruit);
        if (!ok) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "Not enough budget to scout this player.\nScouting costs 10% of the recruit price."),
                    "Cannot Scout", JOptionPane.WARNING_MESSAGE);
            return;
        }
        loadBoard(filterBox.getSelectedIndex());
        showRecruitDetail();
    }

    private void recruitSelected() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            return;
        }
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) {
            return;
        }

        RecruitingPlayerRecord recruit = currentList.get(modelRow);

        if (recruit.cost() > sessionData.recruitingBudget) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "Not enough budget ($" + sessionData.recruitingBudget + ") to recruit "
                            + recruit.name() + " ($" + recruit.cost() + ")."),
                    "Cannot Recruit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String msg = RecruitingPresentation.buildRecruitConfirmMessage(sessionData, MAX_ROSTER_SIZE, recruit);
        int choice = JOptionPane.showConfirmDialog(this,
                DesktopTheme.messageForDialog(msg), "Confirm Recruit", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        controller.recruitPlayer(recruit, false);
        PlatformLog.i(TAG, "Recruited " + recruit.position() + " " + recruit.name()
                + " for $" + recruit.cost());

        loadBoard(filterBox.getSelectedIndex());
    }

    private void finishRecruiting() {
        String exitMsg = RecruitingPresentation.buildExitConfirmMessage(positionLabels);
        int choice = JOptionPane.showConfirmDialog(this,
                DesktopTheme.messageForDialog(exitMsg),
                "Finish Recruiting?", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String data = sessionData.buildRecruitsSaveData();
        onFinish.accept(data);
    }
}
