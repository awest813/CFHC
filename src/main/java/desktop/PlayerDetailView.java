package desktop;

import positions.Player;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that displays detailed information about a single player.
 * Shows ratings, season/career stats, and profile basics.
 */
public class PlayerDetailView extends JDialog {

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 14);

    private final Player player;

    public PlayerDetailView(JFrame owner, Player player) {
        super(owner, player.position + " " + player.name + " — " + player.team.getName(), true);
        this.player = player;
        setSize(700, 500);
        setLayout(new BorderLayout());
        JPanel root = (JPanel) getContentPane();
        root.setOpaque(true);
        root.setBackground(DesktopTheme.windowBackground());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(true);
        tabs.setBackground(DesktopTheme.windowBackground());
        tabs.setForeground(DesktopTheme.textPrimary());
        tabs.addTab("Profile", buildProfileTab());
        tabs.addTab("Stats", buildStatsTab());
        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildProfileTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top: basic info grid
        JPanel info = new JPanel(new GridLayout(0, 2, 10, 4));
        info.setOpaque(true);
        info.setBackground(DesktopTheme.windowBackground());
        TitledBorder infoBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()), "Player Info");
        infoBorder.setTitleColor(DesktopTheme.textPrimary());
        info.setBorder(infoBorder);

        addField(info, "Name", player.name);
        addField(info, "Position", player.position);
        addField(info, "Year", player.getYrStr());
        addField(info, "Team", player.team.getName());
        addField(info, "Overall", String.valueOf(player.ratOvr));
        addField(info, "Potential", String.valueOf(player.ratPot));
        addField(info, "Intelligence", String.valueOf(player.ratIntelligence));
        addField(info, "Durability", String.valueOf(player.ratDurability));
        addField(info, "Height", formatHeight(player.height));
        addField(info, "Weight", player.weight + " lbs");

        String statusFlags = buildStatusFlags();
        if (!statusFlags.isEmpty()) {
            addField(info, "Status", statusFlags);
        }
        panel.add(info, BorderLayout.NORTH);

        // Bottom: position-specific ratings
        JPanel ratingsPanel = new JPanel(new GridLayout(0, 2, 10, 4));
        ratingsPanel.setOpaque(true);
        ratingsPanel.setBackground(DesktopTheme.windowBackground());
        TitledBorder ratingsBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()), "Position Ratings");
        ratingsBorder.setTitleColor(DesktopTheme.textPrimary());
        ratingsPanel.setBorder(ratingsBorder);

        String ratings = player.getPlayerRatings();
        if (ratings != null && !ratings.isEmpty()) {
            String[] parts = ratings.split(",");
            for (int i = 0; i + 1 < parts.length; i += 2) {
                String label = parts[i].trim();
                String value = parts[i + 1].trim();
                if (!label.isEmpty() && !label.equals("ATTR1")) {
                    addField(ratingsPanel, label, value);
                }
            }
        }
        panel.add(ratingsPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        ArrayList<String> statLines = player.getPlayerStats();
        if (statLines == null || statLines.isEmpty()) {
            JLabel empty = new JLabel("No stats available for this player yet.", JLabel.CENTER);
            empty.setForeground(DesktopTheme.textPrimary());
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        // Parse CSV stat blocks into tables
        JPanel tablesPanel = new JPanel();
        tablesPanel.setOpaque(true);
        tablesPanel.setBackground(DesktopTheme.windowBackground());
        tablesPanel.setLayout(new javax.swing.BoxLayout(tablesPanel, javax.swing.BoxLayout.Y_AXIS));

        List<String> currentBlock = new ArrayList<>();
        for (String line : statLines) {
            if (line.trim().replace(",", "").replace(" ", "").isEmpty()) {
                if (!currentBlock.isEmpty()) {
                    tablesPanel.add(buildStatTable(currentBlock));
                    tablesPanel.add(javax.swing.Box.createVerticalStrut(10));
                    currentBlock = new ArrayList<>();
                }
            } else {
                currentBlock.add(line);
            }
        }
        if (!currentBlock.isEmpty()) {
            tablesPanel.add(buildStatTable(currentBlock));
        }

        JScrollPane tablesScroll = new JScrollPane(tablesPanel);
        tablesScroll.getViewport().setBackground(DesktopTheme.windowBackground());
        panel.add(tablesScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatTable(List<String> lines) {
        if (lines.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setOpaque(true);
            empty.setBackground(DesktopTheme.windowBackground());
            return empty;
        }

        String[] headers = lines.get(0).split(",", -1);
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (int i = 1; i < lines.size(); i++) {
            String[] row = lines.get(i).split(",", -1);
            model.addRow(row);
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(false);
        StripedRowRenderer.install(table);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(DesktopTheme.windowBackground());
        JScrollPane tableScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(tableScroll, table);
        wrapper.add(tableScroll, BorderLayout.CENTER);
        wrapper.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30 + lines.size() * 22));
        wrapper.setPreferredSize(new java.awt.Dimension(600, 30 + lines.size() * 22));
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setOpaque(true);
        footer.setBackground(DesktopTheme.windowBackground());
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        StringBuilder sb = new StringBuilder();
        sb.append(player.position).append(" ").append(player.name);
        sb.append("  \u2022  ").append(player.getYrStr());
        sb.append("  \u2022  OVR ").append(player.ratOvr);
        if (player.isSuspended) {
            sb.append("  \u2022  SUSPENDED");
        }
        if (player.isInjured) {
            sb.append("  \u2022  INJURED");
        }
        JLabel line = new JLabel(sb.toString());
        line.setForeground(DesktopTheme.textPrimary());
        footer.add(line);
        return footer;
    }

    private void addField(JPanel panel, String label, String value) {
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(HEADER_FONT);
        lbl.setForeground(DesktopTheme.textSecondary());
        panel.add(lbl);

        JLabel val = new JLabel(value);
        val.setFont(LABEL_FONT);
        val.setForeground(DesktopTheme.textPrimary());
        panel.add(val);
    }

    private String buildStatusFlags() {
        List<String> flags = new ArrayList<>();
        if (player.isRedshirt) flags.add("Redshirt");
        if (player.wasRedshirt) flags.add("RS");
        if (player.isMedicalRS) flags.add("Medical RS");
        if (player.isTransfer) flags.add("Transfer");
        if (player.isGradTransfer) flags.add("Grad Transfer");
        if (player.isWalkOn) flags.add("Walk-On");
        if (player.isSuspended) flags.add("Suspended (" + player.weeksSuspended + " wks)");
        if (player.isInjured) flags.add("Injured");
        return String.join(", ", flags);
    }

    private static String formatHeight(int inches) {
        if (inches <= 0) return "—";
        return (inches / 12) + "'" + (inches % 12) + "\"";
    }

    public static void show(JFrame owner, Player player) {
        PlayerDetailView view = new PlayerDetailView(owner, player);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);
    }
}
