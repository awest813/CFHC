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
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read-only table view of the transfer portal.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class TransferPortalDialog extends JDialog {

    private static final String[] POS_FILTERS = {
            "All Positions", "QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"
    };

    private static final String[] COLUMNS = {"Pos", "Name", "OVR", "Yr", "Original Team"};

    private static final Color ACCENT_BLUE = new Color(52, 152, 219);

    private final League league;
    private DefaultTableModel tableModel;
    private List<Player> allPlayers;

    public TransferPortalDialog(JFrame owner, League league) {
        super(owner, "TRANSFER PORTAL REGISTRY", true);
        this.league = league;
        setSize(850, 600);
        setLayout(new BorderLayout());
        getContentPane().setBackground(DesktopTheme.windowBackground());

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
        // Top Filter Bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15)) {
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
        posLabel.setForeground(ACCENT_BLUE);
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
        
        add(topBar, BorderLayout.NORTH);

        // Table
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

        // Bottom bar
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(DesktopTheme.tableBase());
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()));
        bottom.setPreferredSize(new Dimension(0, 80));
        
        JLabel hintLabel = new JLabel("NOTICE: Portal assignments are finalized during the next off-season cycle.");
        hintLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hintLabel.setForeground(DesktopTheme.textSecondary());
        hintLabel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        bottom.add(hintLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("CLOSE PORTAL") {
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
        closeBtn.setBackground(ACCENT_BLUE);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        closeBtn.addActionListener(e -> dispose());
        
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        closePanel.setOpaque(false);
        closePanel.add(closeBtn);
        bottom.add(closePanel, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Filter Logic
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
