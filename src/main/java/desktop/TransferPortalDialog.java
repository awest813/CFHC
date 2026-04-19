package desktop;

import positions.Player;
import simulation.League;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only table view of the transfer portal.
 *
 * <p>Displays all players currently in the portal grouped by position, with
 * sortable columns.  The user can switch between position filters using the
 * combo box at the top.
 *
 * <p>Actual transfer decisions (which CPU teams receive each player) are still
 * handled automatically by {@link League#transferPlayers(simulation.GameUiBridge)}.
 * This dialog is purely informational, letting the user see who entered the portal
 * this off-season before the automatic assignment runs.
 */
public class TransferPortalDialog extends JDialog {

    private static final String[] POS_FILTERS = {
            "All Positions", "QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"
    };

    private static final String[] COLUMNS = {"Pos", "Name", "OVR", "Yr", "Original Team"};

    private final League league;
    private DefaultTableModel tableModel;
    private List<Player> allPlayers;

    public TransferPortalDialog(JFrame owner, League league) {
        super(owner, "Transfer Portal", true);
        this.league = league;
        setSize(720, 500);
        setLayout(new BorderLayout(0, 6));

        collectPlayers();
        buildContent();
    }

    private void collectPlayers() {
        allPlayers = new ArrayList<>();
        addAll(allPlayers, league.getTransferQBs());
        addAll(allPlayers, league.getTransferRBs());
        addAll(allPlayers, league.getTransferWRs());
        addAll(allPlayers, league.getTransferTEs());
        addAll(allPlayers, league.getTransferOLs());
        addAll(allPlayers, league.getTransferKs());
        addAll(allPlayers, league.getTransferDLs());
        addAll(allPlayers, league.getTransferLBs());
        addAll(allPlayers, league.getTransferCBs());
        addAll(allPlayers, league.getTransferSs());
    }

    private <T extends Player> void addAll(List<Player> dest, List<T> src) {
        if (src != null) dest.addAll(src);
    }

    private void buildContent() {
        // Filter bar
        JComboBox<String> filterBox = new JComboBox<>(POS_FILTERS);
        filterBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel countLabel = new JLabel();
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.add(new JLabel("Position:"));
        topBar.add(filterBox);
        topBar.add(countLabel);
        add(topBar, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 2 || col == 3 ? Integer.class : String.class;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        bottom.add(new JLabel("Transfer decisions are handled automatically for CPU teams."),
                BorderLayout.WEST);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closePanel.add(closeBtn);
        bottom.add(closePanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Load and filter
        Runnable applyFilter = () -> {
            String posFilter = (String) filterBox.getSelectedItem();
            tableModel.setRowCount(0);
            int count = 0;
            for (Player p : allPlayers) {
                if (posFilter == null || posFilter.equals("All Positions")
                        || posFilter.equals(p.position)) {
                    tableModel.addRow(playerRow(p));
                    count++;
                }
            }
            countLabel.setText("  " + count + " player" + (count == 1 ? "" : "s"));
        };

        filterBox.addActionListener(e -> applyFilter.run());
        applyFilter.run();

        if (allPlayers.isEmpty()) {
            tableModel.setRowCount(0);
            countLabel.setText("  No players currently in the transfer portal.");
        }
    }

    private static Object[] playerRow(Player p) {
        String teamName = p.team != null ? p.team.getName() : "";
        return new Object[]{p.position, p.name, p.ratOvr, p.year, teamName};
    }

    /**
     * Shows the transfer portal dialog and blocks until the user closes it.
     *
     * @param owner  parent frame
     * @param league the active league
     */
    public static void show(JFrame owner, League league) {
        TransferPortalDialog dlg = new TransferPortalDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
