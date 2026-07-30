package desktop;

import positions.Player;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

/**
 * High-fidelity, crisp Player Profile dialog matching the sports broadcast UI theme.
 * Displays primary identity header, circular OVR gauge, star ratings, attributes
 * progress bars, detailed ratings matrix, stats log, and career history.
 */
public class PlayerDetailView extends JDialog {

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 12);
    private static final Font VALUE_FONT = new Font("Monospaced", Font.BOLD, 13);

    private final Player player;

    public PlayerDetailView(JFrame owner, Player player) {
        super(owner, player.position + " " + player.name + " — " + (player.team != null ? player.team.getName() : "Free Agent"), true);
        this.player = player;
        setSize(860, 620);
        setLayout(new BorderLayout());
        DesktopTheme.styleDialogContentPane(getContentPane());

        add(buildHeaderBanner(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(true);
        tabs.setBackground(DesktopTheme.windowBackground());
        tabs.setForeground(DesktopTheme.textPrimary());
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));

        tabs.addTab("OVERVIEW", buildOverviewTab());
        tabs.addTab("RATINGS MATRIX", buildRatingsTab());
        tabs.addTab("STATS", buildStatsTab());
        tabs.addTab("HISTORY & MILESTONES", buildHistoryTab());

        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    /**
     * Top Sports Broadcast Banner with Jersey #, Name, Stars, Archetype Tags, & OVR Gauge.
     */
    private JPanel buildHeaderBanner() {
        JPanel header = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color teamColor = (player.team != null) ? simulation.TeamColors.primary(player.team.getAbbr()) : DesktopTheme.selectionAccent();
                DesktopTheme.paintHeaderGradient(g2, getWidth(), getHeight(), teamColor);
                g2.dispose();
            }
        };
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, DesktopTheme.borderSubtle()),
                new EmptyBorder(14, 20, 14, 20)
        ));

        // Left: Jersey Badge
        JPanel jerseyBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 140));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(DesktopTheme.warningText());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        jerseyBox.setOpaque(false);
        jerseyBox.setPreferredSize(new Dimension(64, 64));
        JLabel numLbl = new JLabel("#" + (Math.abs(player.name.hashCode()) % 99 + 1), SwingConstants.CENTER);
        numLbl.setFont(new Font("Monospaced", Font.BOLD, 22));
        numLbl.setForeground(Color.WHITE);
        jerseyBox.add(numLbl, BorderLayout.CENTER);

        // Center: Name & Badges
        JPanel metaPanel = new JPanel();
        metaPanel.setOpaque(false);
        metaPanel.setLayout(new javax.swing.BoxLayout(metaPanel, javax.swing.BoxLayout.Y_AXIS));

        JPanel nameStarsRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        nameStarsRow.setOpaque(false);

        JLabel nameLbl = new JLabel(player.name.toUpperCase());
        nameLbl.setFont(TITLE_FONT);
        nameLbl.setForeground(Color.WHITE);
        nameStarsRow.add(nameLbl);

        JLabel starsLbl = new JLabel(buildStarString(player.recruitRating));
        starsLbl.setFont(new Font("SansSerif", Font.PLAIN, 16));
        starsLbl.setForeground(new Color(251, 191, 36));
        nameStarsRow.add(starsLbl);

        metaPanel.add(nameStarsRow);
        metaPanel.add(javax.swing.Box.createVerticalStrut(4));

        JPanel tagsRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        tagsRow.setOpaque(false);

        tagsRow.add(createChip(player.position + " \u2022 " + player.getArchetypeDisplayName(), new Color(6, 182, 212), new Color(6, 182, 212, 40)));
        tagsRow.add(createChip(player.getYrStr(), DesktopTheme.textSecondary(), new Color(255, 255, 255, 25)));
        if (player.team != null) {
            tagsRow.add(createChip(player.team.getName().toUpperCase(), DesktopTheme.warningText(), new Color(251, 191, 36, 40)));
        }

        metaPanel.add(tagsRow);

        // Right: Circular Gauge Component
        OvrGaugeComponent gauge = new OvrGaugeComponent(player.ratOvr);

        header.add(jerseyBox, BorderLayout.WEST);
        header.add(metaPanel, BorderLayout.CENTER);
        header.add(gauge, BorderLayout.EAST);

        return header;
    }

    private JPanel buildOverviewTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 14, 14));
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Left Column: Key Attributes with Progress Bars
        JPanel attrPanel = new JPanel(new BorderLayout());
        attrPanel.setOpaque(true);
        attrPanel.setBackground(DesktopTheme.windowBackground());
        attrPanel.setBorder(DesktopTheme.titledBorder("KEY ATTRIBUTES"));

        JPanel attrList = new JPanel(new GridLayout(0, 1, 6, 8));
        attrList.setOpaque(false);
        attrList.setBorder(new EmptyBorder(10, 10, 10, 10));

        addAttrProgressRow(attrList, "Overall Rating", player.ratOvr);
        addAttrProgressRow(attrList, "Potential", player.ratPot);
        addAttrProgressRow(attrList, "Intelligence", player.ratIntelligence);
        addAttrProgressRow(attrList, "Durability", player.ratDurability);

        String ratings = player.getPlayerRatings();
        if (ratings != null && !ratings.isEmpty()) {
            String[] parts = ratings.split(",");
            for (int i = 0; i + 1 < parts.length; i += 2) {
                String label = parts[i].trim();
                String valueStr = parts[i + 1].trim();
                if (!label.isEmpty() && !label.equals("ATTR1")) {
                    try {
                        int val = Integer.parseInt(valueStr);
                        addAttrProgressRow(attrList, label, val);
                    } catch (NumberFormatException ignored) {
                        // fallback string
                    }
                }
            }
        }
        attrPanel.add(new JScrollPane(attrList), BorderLayout.CENTER);

        // Right Column: Quick Bio & Status
        JPanel bioPanel = new JPanel(new BorderLayout());
        bioPanel.setOpaque(true);
        bioPanel.setBackground(DesktopTheme.windowBackground());
        bioPanel.setBorder(DesktopTheme.titledBorder("PROFILE & BIO"));

        JPanel bioGrid = new JPanel(new GridLayout(0, 2, 10, 10));
        bioGrid.setOpaque(false);
        bioGrid.setBorder(new EmptyBorder(12, 12, 12, 12));

        addBioField(bioGrid, "Height", formatHeight(player.height));
        addBioField(bioGrid, "Weight", player.weight + " lbs");
        addBioField(bioGrid, "Position", player.position);
        addBioField(bioGrid, "Class", player.getYrStr());
        addBioField(bioGrid, "Hometown", player.getHomeState(player.homeState));
        addBioField(bioGrid, "Work Ethic", player.ratIntelligence >= 85 ? "ELITE" : (player.ratIntelligence >= 70 ? "HIGH" : "NORMAL"));
        addBioField(bioGrid, "Dev Plan", player.getArchetypeDisplayName());

        String statusFlags = buildStatusFlags();
        if (!statusFlags.isEmpty()) {
            addBioField(bioGrid, "Status", statusFlags);
        }

        bioPanel.add(bioGrid, BorderLayout.NORTH);

        panel.add(attrPanel);
        panel.add(bioPanel);

        return panel;
    }

    private JPanel buildRatingsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel grid = new JPanel(new GridLayout(0, 3, 10, 10));
        grid.setOpaque(false);

        addRatingBadge(grid, "Overall (OVR)", player.ratOvr);
        addRatingBadge(grid, "Potential (POT)", player.ratPot);
        addRatingBadge(grid, "Awareness", player.ratIntelligence);
        addRatingBadge(grid, "Durability", player.ratDurability);

        String ratings = player.getPlayerRatings();
        if (ratings != null && !ratings.isEmpty()) {
            String[] parts = ratings.split(",");
            for (int i = 0; i + 1 < parts.length; i += 2) {
                String label = parts[i].trim();
                String valueStr = parts[i + 1].trim();
                if (!label.isEmpty() && !label.equals("ATTR1")) {
                    try {
                        int val = Integer.parseInt(valueStr);
                        addRatingBadge(grid, label, val);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.getViewport().setBackground(DesktopTheme.windowBackground());
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildStatsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        ArrayList<String> statLines = player.getPlayerStats();
        if (statLines == null || statLines.isEmpty()) {
            JLabel empty = new JLabel("No career stats recorded for this player yet.", SwingConstants.CENTER);
            empty.setFont(LABEL_FONT);
            empty.setForeground(DesktopTheme.textSecondary());
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

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

    private JPanel buildHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel container = new JPanel(new GridLayout(0, 1, 8, 8));
        container.setOpaque(false);

        addHistoryItem(container, "Recruiting Stars", buildStarString(player.recruitRating) + " (" + player.recruitRating + "-Star Recruit)");
        addHistoryItem(container, "Current Status", buildStatusFlags().isEmpty() ? "Active Roster (Healthy)" : buildStatusFlags());
        addHistoryItem(container, "Development Archetype", player.getArchetypeDisplayName());
        addHistoryItem(container, "Team Affiliation", player.team != null ? player.team.getName() + " (" + player.team.conference + ")" : "Unassigned");

        JScrollPane scroll = new JScrollPane(container);
        scroll.getViewport().setBackground(DesktopTheme.windowBackground());
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildStatTable(List<String> lines) {
        if (lines.isEmpty()) return new JPanel();

        String[] headers = lines.get(0).split(",", -1);
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (int i = 1; i < lines.size(); i++) {
            model.addRow(lines.get(i).split(",", -1));
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        StripedRowRenderer.install(table);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(true);
        wrapper.setBackground(DesktopTheme.windowBackground());
        JScrollPane tableScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(tableScroll, table, "Player season stats");
        wrapper.add(tableScroll, BorderLayout.CENTER);
        wrapper.setPreferredSize(new Dimension(600, 30 + lines.size() * 24));
        return wrapper;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(true);
        footer.setBackground(DesktopTheme.statusBackground());
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                new EmptyBorder(8, 16, 8, 16)
        ));

        JLabel infoLabel = new JLabel(player.position + " " + player.name + "  \u2022  " + player.getYrStr() + "  \u2022  OVR " + player.ratOvr);
        infoLabel.setFont(LABEL_FONT);
        infoLabel.setForeground(DesktopTheme.textPrimary());

        JLabel hintLabel = new JLabel("CFHC UI REDESIGN • 2026 EDITION");
        hintLabel.setFont(new Font("Monospaced", Font.BOLD, 10));
        hintLabel.setForeground(DesktopTheme.textSecondary());

        footer.add(infoLabel, BorderLayout.WEST);
        footer.add(hintLabel, BorderLayout.EAST);

        return footer;
    }

    private void addAttrProgressRow(JPanel parent, String label, int value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(DesktopTheme.textPrimary());
        lbl.setPreferredSize(new Dimension(140, 20));

        JProgressBar bar = new JProgressBar(50, 99);
        bar.setValue(value);
        bar.setPreferredSize(new Dimension(120, 14));
        bar.setForeground(getTierColor(value));
        bar.setBackground(new Color(255, 255, 255, 20));

        JLabel valLbl = new JLabel(String.valueOf(value), SwingConstants.RIGHT);
        valLbl.setFont(VALUE_FONT);
        valLbl.setForeground(getTierColor(value));
        valLbl.setPreferredSize(new Dimension(32, 20));

        row.add(lbl, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        row.add(valLbl, BorderLayout.EAST);

        parent.add(row);
    }

    private void addBioField(JPanel parent, String label, String value) {
        JPanel field = new JPanel(new BorderLayout(4, 0));
        field.setOpaque(false);

        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(DesktopTheme.textSecondary());

        JLabel val = new JLabel(value);
        val.setFont(VALUE_FONT);
        val.setForeground(DesktopTheme.textPrimary());

        field.add(lbl, BorderLayout.WEST);
        field.add(val, BorderLayout.CENTER);

        parent.add(field);
    }

    private void addRatingBadge(JPanel parent, String label, int value) {
        JPanel badge = new JPanel(new BorderLayout(4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel valLbl = new JLabel(String.valueOf(value), SwingConstants.CENTER);
        valLbl.setFont(new Font("Monospaced", Font.BOLD, 20));
        valLbl.setForeground(getTierColor(value));

        JLabel nameLbl = new JLabel(label.toUpperCase(), SwingConstants.CENTER);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        nameLbl.setForeground(DesktopTheme.textSecondary());

        badge.add(valLbl, BorderLayout.CENTER);
        badge.add(nameLbl, BorderLayout.SOUTH);

        parent.add(badge);
    }

    private void addHistoryItem(JPanel parent, String title, String desc) {
        JPanel item = new JPanel(new BorderLayout(4, 4));
        item.setOpaque(true);
        item.setBackground(DesktopTheme.textAreaEditorBackground());
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(LABEL_FONT);
        titleLbl.setForeground(DesktopTheme.warningText());

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(VALUE_FONT);
        descLbl.setForeground(DesktopTheme.textPrimary());

        item.add(titleLbl, BorderLayout.NORTH);
        item.add(descLbl, BorderLayout.SOUTH);

        parent.add(item);
    }

    private JLabel createChip(String text, Color fg, Color bg) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(fg);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(fg);
        label.setOpaque(false);
        label.setBorder(new EmptyBorder(3, 8, 3, 8));
        return label;
    }

    private static Color getTierColor(int val) {
        if (val >= 90) return new Color(251, 191, 36); // Gold
        if (val >= 80) return new Color(16, 185, 129); // Green
        if (val >= 70) return new Color(59, 130, 246); // Blue
        return new Color(239, 68, 68); // Red
    }

    private static String buildStarString(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(1, Math.min(5, count)); i++) {
            sb.append("\u2605");
        }
        return sb.toString();
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

    /**
     * Custom Java2D Arc Gauge Component for Rendering OVR in Header.
     */
    private static class OvrGaugeComponent extends JComponent {
        private final int ovr;

        public OvrGaugeComponent(int ovr) {
            this.ovr = ovr;
            setPreferredSize(new Dimension(72, 72));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int pad = 6;
            int diameter = Math.min(w, h) - pad * 2;

            // Background Ring
            g2.setColor(new Color(255, 255, 255, 20));
            g2.setStroke(new java.awt.BasicStroke(6));
            g2.drawOval(pad, pad, diameter, diameter);

            // Filled Arc Meter
            g2.setColor(getTierColor(ovr));
            double extent = -360.0 * (Math.min(ovr, 99) / 99.0);
            g2.draw(new Arc2D.Double(pad, pad, diameter, diameter, 90, extent, Arc2D.OPEN));

            // Numerical OVR
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            g2.setColor(Color.WHITE);
            String text = String.valueOf(ovr);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2 - 4;
            g2.drawString(text, tx, ty);

            // Label OVR
            g2.setFont(new Font("SansSerif", Font.BOLD, 8));
            g2.setColor(DesktopTheme.textSecondary());
            String lbl = "OVR";
            java.awt.FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(lbl, (w - fm2.stringWidth(lbl)) / 2, ty + 12);

            g2.dispose();
        }
    }
}
