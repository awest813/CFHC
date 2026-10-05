package desktop;

import recruiting.RecruitingController;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingPresentation;
import recruiting.RecruitingSessionData;
import simulation.GameFlowManager;
import simulation.League;
import simulation.PlatformLog;
import simulation.RosterRules;
import simulation.SimulationFacade;
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
import java.awt.Cursor;
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
    private static final String[] BOARD_COLUMNS = {"Pos", "Name", "Stars", "Cost", "Overall"};
    private static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private final RecruitingController controller;
    private final RecruitingSessionData sessionData;
    private final Consumer<String> onFinish;
    private final ArrayList<String> positionLabels;
    private final String finishButtonText;
    private final String finishDialogTitle;
    private final String finishDialogMessage;

    private JComboBox<String> filterBox;
    private DefaultTableModel boardModel;
    private JTable boardTable;
    private static final String SCOUT_LABEL = "Scout (10% cost)";
    /** Disabled for a recruit already scouted (mirrors the Android board). */
    private JButton scoutButton;
    private JTextArea detailArea;
    private JLabel budgetLabel;
    private JLabel recruitedLabel;
    private JTextArea rosterArea;

    private List<RecruitingPlayerRecord> currentList;
    private boolean updatingFilterItems;

    /**
     * @param league   active league (CPU teams should already be auto-recruited)
     * @param onFinish called on EDT after the user confirms finish; argument is
     *                 serialized recruit data (may be empty if they signed nobody)
     */
    public RecruitingPanel(League league, Consumer<String> onFinish) {
        this(league, null, "Finish Recruiting", "Finish Recruiting?", null, onFinish);
    }

    public RecruitingPanel(League league, RecruitingSessionData existingSession, String finishButtonText,
                           String finishDialogTitle, String finishDialogMessage, Consumer<String> onFinish) {
        super(new BorderLayout());
        this.onFinish = onFinish;
        this.finishButtonText = finishButtonText;
        this.finishDialogTitle = finishDialogTitle;
        this.finishDialogMessage = finishDialogMessage;
        this.sessionData = existingSession != null ? existingSession : buildSessionData(league.userTeam);
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
        RecruitingSessionData.PositionNeeds needs = sessionData.calculateNeeds();
        this.positionLabels = RecruitingPresentation.buildPositionLabels(sessionData, needs);

        setOpaque(true);
        setBackground(DesktopTheme.windowBackground());

        add(buildToolBar(), BorderLayout.NORTH);
        add(buildCenterContent(), BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        loadBoard(0);
    }

    private static RecruitingSessionData buildSessionData(Team userTeam) {
        return SimulationFacade.prepareRecruitingSession(userTeam);
    }

    private JPanel buildToolBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JLabel filterLbl = new JLabel("Position:");
        bar.add(filterLbl);
        filterBox = new JComboBox<>(positionLabels.toArray(new String[0]));
        filterBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        filterBox.getAccessibleContext().setAccessibleName("Recruiting position filter");
        filterLbl.setLabelFor(filterBox);
        filterBox.addActionListener(e -> {
            if (!updatingFilterItems) {
                loadBoard(filterBox.getSelectedIndex());
            }
        });
        bar.add(filterBox);

        JButton sortGradeBtn = new JButton("Sort by Grade");
        sortGradeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sortGradeBtn.addActionListener(e -> {
            controller.sortByGrade();
            loadBoard(filterBox.getSelectedIndex());
        });
        bar.add(sortGradeBtn);

        JButton sortCostBtn = new JButton("Sort by Cost");
        sortCostBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        DesktopTheme.styleToolbar(bar);
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
        DesktopTheme.styleDataTableInScroll(tableScroll, boardTable, "Recruiting board");
        // Weighted columns: long player names need the lion's share or the
        // default equal split ellipsizes them ("Yehuda Carpe...").
        boardTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Pos
        boardTable.getColumnModel().getColumn(1).setPreferredWidth(175); // Name
        boardTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Stars
        boardTable.getColumnModel().getColumn(3).setPreferredWidth(65);  // Cost
        boardTable.getColumnModel().getColumn(4).setPreferredWidth(70);  // Overall
        // Stars as gold glyphs (style guide), keeping the Integer model value so
        // the column still sorts numerically.
        boardTable.getColumnModel().getColumn(2).setCellRenderer(new StripedRowRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (c instanceof JLabel jl && value instanceof Integer stars) {
                    jl.setText("\u2605".repeat(Math.max(0, Math.min(5, stars))));
                    jl.setFont(table.getFont());
                    jl.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                    jl.setToolTipText(stars + "-star recruit");
                    if (!isSelected) {
                        jl.setForeground(DesktopTheme.isDark() ? DesktopTheme.gold() : DesktopTheme.warningText());
                    }
                }
                return c;
            }
        });
        tableScroll.setPreferredSize(new Dimension(620, 0));

        JPanel rightPanel = new JPanel(new BorderLayout(0, 6));
        rightPanel.setOpaque(true);
        rightPanel.setBackground(DesktopTheme.windowBackground());

        detailArea = new JTextArea("Select a recruit to see scouting, cost, and roster fit.");
        detailArea.setEditable(false);
        detailArea.setFont(MONO);
        DesktopTheme.styleTextContent(detailArea);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        detailArea.setBorder(BorderFactory.createCompoundBorder(
                DesktopTheme.titledBorder("Player Scouting"),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        rosterArea = new JTextArea();
        rosterArea.setEditable(false);
        rosterArea.setFont(MONO);
        rosterArea.setBackground(DesktopTheme.textAreaEditorBackground());
        rosterArea.setForeground(DesktopTheme.textPrimary());
        rosterArea.setBorder(BorderFactory.createCompoundBorder(
                DesktopTheme.titledBorder("Current Roster Overview"),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        JScrollPane rosterScroll = new JScrollPane(rosterArea);
        rosterScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());

        // Resizable split: as BorderLayout.NORTH the detail pane kept the height
        // of its one-line placeholder, cutting scouting (ratings, potential) off
        // below the recruit's name.
        detailScroll.setMinimumSize(new Dimension(0, 120));
        rosterScroll.setMinimumSize(new Dimension(0, 120));
        JSplitPane detailSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detailScroll, rosterScroll);
        detailSplit.setDividerLocation(320);
        detailSplit.setResizeWeight(0.45);
        detailSplit.setBorder(BorderFactory.createEmptyBorder());
        detailSplit.setOpaque(false);
        rightPanel.add(detailSplit, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        actionPanel.setOpaque(true);
        actionPanel.setBackground(DesktopTheme.windowBackground());
        actionPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // One accent action (Recruit) next to a regular secondary (Scout), using
        // the HUD button treatment instead of the off-palette selection blue.
        JButton scoutBtn = new JButton(SCOUT_LABEL);
        scoutButton = scoutBtn;
        DesktopTheme.styleHudQuickButton(scoutBtn, false);
        scoutBtn.setFont(scoutBtn.getFont().deriveFont(Font.BOLD, 13f));
        scoutBtn.addActionListener(e -> scoutSelected());
        actionPanel.add(scoutBtn);

        JButton recruitBtn = new JButton("Recruit");
        DesktopTheme.styleHudQuickButton(recruitBtn, true);
        recruitBtn.setFont(recruitBtn.getFont().deriveFont(Font.BOLD, 13f));
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

        JButton doneBtn = new JButton(finishButtonText);
        DesktopTheme.styleHudQuickButton(doneBtn, false);
        doneBtn.setFont(doneBtn.getFont().deriveFont(Font.BOLD, 13f));
        doneBtn.addActionListener(e -> finishRecruiting());
        bar.add(doneBtn);
        return bar;
    }

    private void loadBoard(int filterIndex) {
        if (filterIndex < 0 || filterIndex >= positionLabels.size()) {
            return;
        }
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

        if (currentList.isEmpty()) {
            detailArea.setText(RecruitingPresentation.buildEmptyBoardMessage());
        } else if (boardTable.getSelectedRow() < 0) {
            detailArea.setText("Select a recruit to see scouting, cost, and roster fit.");
        }

        updateLabels();
        updateRoster();
    }

    private void updateLabels() {
        budgetLabel.setText("  Budget: $" + sessionData.recruitingBudget
                + "   HC recruiting: " + sessionData.coachTalent);
        budgetLabel.setForeground(DesktopTheme.textPrimary());
        recruitedLabel.setText("  Recruited: " + sessionData.playersRecruited.size());
        recruitedLabel.setForeground(DesktopTheme.textSecondary());

        RecruitingSessionData.PositionNeeds currentNeeds =
                sessionData.calculateNeeds();
        int sel = filterBox.getSelectedIndex();
        ArrayList<String> newLabels = RecruitingPresentation.buildPositionLabels(sessionData, currentNeeds);
        positionLabels.clear();
        positionLabels.addAll(newLabels);
        updatingFilterItems = true;
        filterBox.removeAllItems();
        for (String l : newLabels) {
            filterBox.addItem(l);
        }
        if (sel >= 0 && sel < filterBox.getItemCount()) {
            filterBox.setSelectedIndex(sel);
        }
        updatingFilterItems = false;
    }

    private void updateRoster() {
        RecruitingSessionData.PositionNeeds currentNeeds =
                sessionData.calculateNeeds();
        rosterArea.setText(RecruitingPresentation.buildRosterText(sessionData, currentNeeds));
        rosterArea.setCaretPosition(0);
    }

    private void showRecruitDetail() {
        int viewRow = boardTable.getSelectedRow();
        if (viewRow < 0 || currentList == null) {
            detailArea.setText("Select a recruit to see scouting, cost, and roster fit.");
            if (scoutButton != null) {
                scoutButton.setEnabled(true);
                scoutButton.setText(SCOUT_LABEL);
            }
            return;
        }
        int modelRow = boardTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= currentList.size()) {
            return;
        }

        RecruitingPlayerRecord recruit = currentList.get(modelRow);
        String pos = recruit.position();
        boolean scouted = sessionData.isScouted(recruit);
        if (scoutButton != null) {
            scoutButton.setEnabled(!scouted);
            scoutButton.setText(scouted ? "Scouted" : SCOUT_LABEL);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(recruit.name()).append("  (").append(pos).append(")\n");
        sb.append(RecruitingPresentation.getPlayerListRightLabel(recruit)).append("\n");
        sb.append("Cost: $").append(recruit.cost()).append("\n");
        sb.append("Overall: ").append(recruit.recruitOverall()).append("\n\n");
        sb.append(RecruitingPresentation.buildRecruitBoardDetails(recruit, pos)).append("\n\n");
        sb.append(RecruitingPresentation.buildPotentialDetails(recruit, scouted));
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
                    "Not enough budget to scout this player.\nScouting costs about 10% of the recruit price (minimum $5), reduced slightly by head coach recruiting skill."),
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

        if (!sessionData.canRecruitMore()) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                            "Roster is full (" + sessionData.projectedRosterSize()
                                    + "/" + RosterRules.MAX_PLAYERS + ")."),
                    "Cannot Recruit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (recruit.cost() > sessionData.recruitingBudget) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "Not enough budget ($" + sessionData.recruitingBudget + ") to recruit "
                            + recruit.name() + " ($" + recruit.cost() + ")."),
                    "Cannot Recruit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String msg = RecruitingPresentation.buildRecruitConfirmMessage(
                sessionData, RosterRules.MAX_PLAYERS, recruit);
        int choice = JOptionPane.showConfirmDialog(this,
                DesktopTheme.messageForDialog(msg), "Confirm Recruit", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            controller.recruitPlayer(recruit, false);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(ex.getMessage()),
                    "Cannot Recruit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PlatformLog.i(TAG, "Recruited " + recruit.position() + " " + recruit.name()
                + " for $" + recruit.cost());

        loadBoard(filterBox.getSelectedIndex());
    }

    private void finishRecruiting() {
        String exitMsg = finishDialogMessage != null
                ? finishDialogMessage
                : RecruitingPresentation.buildExitConfirmMessage(positionLabels);
        int choice = JOptionPane.showConfirmDialog(this,
                DesktopTheme.messageForDialog(exitMsg),
                finishDialogTitle, JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String data = sessionData.buildRecruitsSaveData();
        onFinish.accept(data);
    }
}
