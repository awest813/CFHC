package desktop;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public class CoachDatabasePanel implements LeagueScreen {

    private static final String[] COACH_DATABASE_CATEGORIES = {
            "National Champs", "Conf. Champs", "Bowl Wins", "Total Wins", "Win %",
            "Coach of the Year", "Conf COTY", "All-Americans", "All-Conference",
            "Career Score", "Career Prestige"
    };

    @Override
    public String title() {
        return "Coaches";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(DesktopTheme.buildScreenHeader("Coaches Database",
                "Browse all-time coaching leaderboards across the league."), BorderLayout.NORTH);

        categoryBox = new JComboBox<>(COACH_DATABASE_CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        categoryBox.getAccessibleContext().setAccessibleName("Coach database category");

        String[] columns = {"Rank", "Coach", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        StripedRowRenderer.install(table);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String coachStr = String.valueOf(table.getValueAt(row, 1));
                        int openParen = coachStr.lastIndexOf('(');
                        int closeParen = coachStr.lastIndexOf(')');
                        if (openParen >= 0 && closeParen > openParen) {
                            String teamAbbr = coachStr.substring(openParen + 1, closeParen).trim();
                            if (!teamAbbr.equalsIgnoreCase("RET")) {
                                for (simulation.Team t : ctx.league().getTeamList()) {
                                    if (t.getAbbr() != null && t.getAbbr().equalsIgnoreCase(teamAbbr)) {
                                        ctx.nav().openTeamDetail(t);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        JScrollPane coachDbScroll = new JScrollPane(table);

        javax.swing.JLabel emptyLabel = new javax.swing.JLabel(
                "No coaching history yet — leaderboards fill in after games are played.",
                javax.swing.JLabel.CENTER);
        emptyLabel.setForeground(DesktopTheme.textSecondary());

        Runnable loadCoaches = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            boolean allZero = false;
            try {
                java.util.List<String> rankings = ctx.league().getCoachDatabase(sel);
                allZero = LeagueScreenContext.isLeaderboardAllZero(rankings);
                if (rankings != null && !allZero) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                simulation.PlatformLog.e("CoachDatabasePanel", "Error loading coach database", ex);
            }
            coachDbScroll.setViewportView(allZero ? emptyLabel : table);
        };

        categoryBox.addActionListener(e -> loadCoaches.run());
        loadCoaches.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Sort by: "));
        topBar.add(categoryBox);
        DesktopTheme.styleToolbar(topBar);
        topPanel.add(topBar, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);
        DesktopTheme.styleDataTableInScroll(coachDbScroll, table, "Coach database");
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(table.getBackground());
        panel.add(coachDbScroll, BorderLayout.CENTER);

        javax.swing.JLabel coachHint = new javax.swing.JLabel("Double-click any active coach to view team details.");
        coachHint.setForeground(DesktopTheme.textSecondary());
        panel.add(coachHint, BorderLayout.SOUTH);

        return panel;
    }

    /** Filter control focused by Ctrl+F (LeagueScreen.searchTarget). */
    private javax.swing.JComboBox<String> categoryBox;

    @Override
    public javax.swing.JComponent searchTarget() {
        return categoryBox;
    }
}