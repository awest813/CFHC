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
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Transfer portal registry plus Android-parity transfer summaries
 * ({@link League#userTransfers} / {@link League#sumTransfers}).
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
        super(owner, league != null ? league.getYear() + " Transfer Portal" : "Transfer Portal", true);
        this.league = league;
        setSize(900, 680);
        setLayout(new BorderLayout());
        DesktopTheme.styleDialogContentPane(getContentPane());
        DesktopTheme.applyWindowIcon(this);

        collectPlayers();
        buildContent();
    }

    /** Android-parity "your program" transfer blurb. */
    static String userTransferSummary(League league) {
        if (league == null) {
            return "No transfer data.";
        }
        String text = league.userTransfers;
        if (text == null || text.trim().isEmpty()) {
            return "No transfers involving your program this cycle.";
        }
        return text.trim();
    }

    /** Android-parity league-wide transfer dump. */
    static String leagueTransferSummary(League league) {
        if (league == null) {
            return "No transfer data.";
        }
        String text = league.sumTransfers;
        if (text == null || text.trim().isEmpty()) {
            return "No league-wide transfer summary available yet.";
        }
        return text.trim();
    }

    private void collectPlayers() {
        allPlayers = new ArrayList<>();
        if (league == null) {
            return;
        }
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
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(true);
        north.setBackground(DesktopTheme.tableBase());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DesktopTheme.borderSubtle());
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        topBar.setBackground(DesktopTheme.tableBase());

        JLabel posLabel = new JLabel("PORTAL FILTER:");
        posLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        posLabel.setForeground(DesktopTheme.accentBlue());
        topBar.add(posLabel);

        JComboBox<String> filterBox = new JComboBox<>(POS_FILTERS);
        filterBox.setFont(new Font("SansSerif", Font.BOLD, 13));
        filterBox.setBackground(DesktopTheme.windowBackground());
        filterBox.setForeground(DesktopTheme.textPrimary());
        filterBox.setPreferredSize(new Dimension(180, 30));
        topBar.add(filterBox);

        JLabel countLabel = new JLabel();
        countLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        countLabel.setForeground(DesktopTheme.textSecondary());
        topBar.add(countLabel);

        JButton myTransfersBtn = new JButton("My Transfers");
        DesktopTheme.styleSecondaryButton(myTransfersBtn);
        myTransfersBtn.addActionListener(e -> DesktopTheme.showScrollableText(
                this, league.getYear() + " Your Transfers", userTransferSummary(league)));
        topBar.add(myTransfersBtn);

        JButton allTransfersBtn = new JButton("All Transfers");
        DesktopTheme.styleSecondaryButton(allTransfersBtn);
        allTransfersBtn.addActionListener(e -> DesktopTheme.showScrollableText(
                this, league.getYear() + " All Transfers", leagueTransferSummary(league)));
        topBar.add(allTransfersBtn);

        north.add(topBar, BorderLayout.NORTH);

        JTextArea summary = new JTextArea(userTransferSummary(league));
        summary.setEditable(false);
        summary.setLineWrap(true);
        summary.setWrapStyleWord(true);
        summary.setRows(4);
        DesktopTheme.styleTextContent(summary);
        JScrollPane summaryScroll = new JScrollPane(summary);
        summaryScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()),
                "Your program this cycle"));
        summaryScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        summaryScroll.setPreferredSize(new Dimension(0, 110));
        north.add(summaryScroll, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 2 || col == 3 ? Integer.class : String.class;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(35);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setBackground(DesktopTheme.windowBackground());
        table.setForeground(DesktopTheme.textPrimary());
        table.setGridColor(DesktopTheme.borderSubtle());
        table.setShowVerticalLines(false);
        table.setSelectionBackground(DesktopTheme.selectionAccent());
        StripedRowRenderer.install(table);

        table.getTableHeader().setBackground(DesktopTheme.tableBase());
        table.getTableHeader().setForeground(DesktopTheme.textSecondary());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.borderSubtle()));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(DesktopTheme.windowBackground());
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(DesktopTheme.tableBase());
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()));
        bottom.setPreferredSize(new Dimension(0, 80));

        JLabel hintLabel = new JLabel("Portal registry lists available prospects. Use My/All Transfers for signed deals this cycle.");
        hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hintLabel.setForeground(DesktopTheme.textSecondary());
        hintLabel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        bottom.add(hintLabel, BorderLayout.WEST);

        JButton closeBtn = DesktopTheme.createGlassButton("CLOSE PORTAL", DesktopTheme.accentBlue());
        closeBtn.addActionListener(e -> dispose());

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        closePanel.setOpaque(false);
        closePanel.add(closeBtn);
        bottom.add(closePanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

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
            countLabel.setText("  " + count + " PROSPECTS IN PORTAL");
        };

        filterBox.addActionListener(e -> applyFilter.run());
        applyFilter.run();

        if (allPlayers.isEmpty()) {
            tableModel.setRowCount(0);
            countLabel.setText("  PORTAL REGISTRY IS CURRENTLY EMPTY.");
        }
    }

    private static Object[] playerRow(Player p) {
        String teamName = p.team != null ? p.team.getName().toUpperCase(Locale.ROOT) : "UNKNOWN";
        return new Object[]{p.position, p.name, p.ratOvr, p.year, teamName};
    }

    public static void show(JFrame owner, League league) {
        TransferPortalDialog dlg = new TransferPortalDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
