package desktop;

import positions.Player;
import simulation.League;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Interactive redshirt management dialog.
 *
 * <p>Shows two tables side-by-side:
 * <ul>
 *   <li><b>Currently redshirted</b> — all players whose {@code isRedshirt} flag is set.
 *       The user can select a player and click "Remove Redshirt" to reinstate them.</li>
 *   <li><b>Eligible freshmen</b> — year-1 players on the user's team who have not yet
 *       been redshirted and are eligible (haven't used a redshirt year yet).
 *       The user can select a player and click "Grant Redshirt" to set the flag.</li>
 * </ul>
 *
 * <p>Changes take effect immediately on the live {@link Player} objects and will be
 * persisted the next time the league is saved.
 */
public class RedshirtDialog extends JDialog {

    private static final String[] COLUMNS = {"Pos", "Name", "Yr", "OVR", "Team"};
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 13);

    private final League league;
    private DefaultTableModel currentModel;
    private DefaultTableModel eligibleModel;
    private List<Player> currentList;
    private List<Player> eligibleList;

    public RedshirtDialog(JFrame owner, League league) {
        super(owner, "Redshirt Management", true);
        this.league = league;
        setSize(900, 520);
        setLayout(new BorderLayout(0, 6));

        buildContent();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton doneBtn = new JButton("Done");
        doneBtn.addActionListener(e -> dispose());
        bottom.add(doneBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void buildContent() {
        // Left — currently redshirted players
        currentModel = createModel();
        JTable currentTable = new JTable(currentModel);
        currentTable.setRowHeight(22);
        currentTable.setFont(LABEL_FONT);
        currentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 4));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Currently Redshirted"));
        leftPanel.add(new JScrollPane(currentTable), BorderLayout.CENTER);

        JButton removeBtn = new JButton("Remove Redshirt ▶");
        removeBtn.setToolTipText("Remove the selected player's redshirt status");
        removeBtn.addActionListener(e -> {
            int row = currentTable.getSelectedRow();
            if (row < 0 || row >= currentList.size()) return;
            Player p = currentList.get(row);
            p.isRedshirt = false;
            refresh();
        });
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        leftBottom.add(removeBtn);
        leftPanel.add(leftBottom, BorderLayout.SOUTH);

        // Right — freshmen eligible for redshirt (user team only)
        eligibleModel = createModel();
        JTable eligibleTable = new JTable(eligibleModel);
        eligibleTable.setRowHeight(22);
        eligibleTable.setFont(LABEL_FONT);
        eligibleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 4));
        String rightTitle = league.userTeam != null
                ? "Eligible Freshmen (" + league.userTeam.getName() + ")"
                : "Eligible Freshmen (no user team)";
        rightPanel.setBorder(BorderFactory.createTitledBorder(rightTitle));
        rightPanel.add(new JScrollPane(eligibleTable), BorderLayout.CENTER);

        JButton grantBtn = new JButton("◀ Grant Redshirt");
        grantBtn.setToolTipText("Grant redshirt status to the selected freshman");
        grantBtn.addActionListener(e -> {
            int row = eligibleTable.getSelectedRow();
            if (row < 0 || row >= eligibleList.size()) return;
            Player p = eligibleList.get(row);
            p.isRedshirt = true;
            refresh();
        });
        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rightBottom.add(grantBtn);
        rightPanel.add(rightBottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(440);
        add(split, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "  Redshirts preserve a year of eligibility for players who appear in ≤4 games.",
                JLabel.LEFT);
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        add(hint, BorderLayout.NORTH);

        populateTables();
    }

    private DefaultTableModel createModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 3 ? Integer.class : String.class;
            }
        };
    }

    private void populateTables() {
        // Collect all currently redshirted players across the league
        currentList = new ArrayList<>(league.getRedshirts());
        currentList.sort(Comparator.comparing((Player p) -> p.position)
                .thenComparing(p -> p.name));

        currentModel.setRowCount(0);
        for (Player p : currentList) {
            currentModel.addRow(playerRow(p));
        }

        // Collect eligible freshmen from the user's team
        eligibleList = new ArrayList<>();
        if (league.userTeam != null) {
            for (Player p : league.userTeam.getAllPlayers()) {
                if (isRedshirtEligible(p)) {
                    eligibleList.add(p);
                }
            }
            eligibleList.sort(Comparator.comparing((Player p) -> p.position)
                    .thenComparing(p -> p.name));
        }

        eligibleModel.setRowCount(0);
        for (Player p : eligibleList) {
            eligibleModel.addRow(playerRow(p));
        }
    }

    private void refresh() {
        populateTables();
    }

    private static Object[] playerRow(Player p) {
        String teamName = p.team != null ? p.team.getName() : "";
        return new Object[]{p.position, p.name, yearLabel(p.year), p.ratOvr, teamName};
    }

    /**
     * A freshman (year 1) is eligible for a redshirt if they haven't used one yet,
     * are not already redshirted, and haven't used a medical redshirt year.
     */
    private static boolean isRedshirtEligible(Player p) {
        return p.year == 1 && !p.isRedshirt && !p.wasRedshirt && !p.isMedicalRS;
    }

    private static String yearLabel(int year) {
        return switch (year) {
            case 0 -> "RS";
            case 1 -> "FR";
            case 2 -> "SO";
            case 3 -> "JR";
            case 4 -> "SR";
            case 5 -> "5SR";
            default -> String.valueOf(year);
        };
    }

    /**
     * Shows the redshirt management dialog and blocks until the user closes it.
     *
     * @param owner  parent frame
     * @param league the active league
     */
    public static void show(JFrame owner, League league) {
        RedshirtDialog dlg = new RedshirtDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
