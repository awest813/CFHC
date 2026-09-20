package desktop;

import positions.Player;
import simulation.PlatformLog;
import simulation.Team;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class PlayerStatsPanel implements LeagueScreen {

    private static final String TAG = "PlayerStatsPanel";

    private static final String[] CATEGORIES = {
            "QB Pass Rating", "QB Pass Yards", "QB Pass TDs", "QB INTs", "QB Completion %",
            "Rush Yards", "Rush TDs",
            "Receptions", "Receiving Yards", "Receiving TDs",
            "Tackles", "Sacks", "Fumbles Recovered", "Interceptions",
            "FG Made", "FG %",
            "KO Return Yards", "KO Return TDs", "Punt Return Yards", "Punt Return TDs",
            "Coach Overall", "Coach Score"
    };

    private static final String HINT = "Statistics update after each simulated week. Double-click any row to view details.";

    @Override
    public String title() {
        return "Player Stats";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(DesktopTheme.buildScreenHeader("Player Statistics",
                "League-wide individual stat leaderboards by category."), BorderLayout.NORTH);

        categoryBox = new JComboBox<>(CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        categoryBox.getAccessibleContext().setAccessibleName("Player stats category");

        String[] columns = {"Rank", "Player", "Team", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        StripedRowRenderer.install(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        int selCat = categoryBox.getSelectedIndex();
                        String name = (String) table.getValueAt(row, 1);
                        String teamAbbr = (String) table.getValueAt(row, 2);
                        if (selCat < 20) {
                            Player player = ctx.findPlayerInLeague(name, teamAbbr);
                            if (player != null) {
                                PlayerDetailView.show(ctx.parent(), player);
                            }
                        } else {
                            for (Team t : ctx.league().getTeamList()) {
                                if (t.getAbbr() != null && t.getAbbr().equalsIgnoreCase(teamAbbr)) {
                                    ctx.nav().openTeamDetail(t);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        Runnable loadPlayerRankings = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                ArrayList<String> rankings = ctx.league().getPlayerRankStr(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 4);
                        if (parts.length >= 4) {
                            model.addRow(new Object[]{
                                    parts[0].trim(), parts[1].trim(),
                                    parts[2].trim(), parts[3].trim()
                            });
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading player rankings", ex);
            }
        };

        categoryBox.addActionListener(e -> loadPlayerRankings.run());
        loadPlayerRankings.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Category: "));
        topBar.add(categoryBox);
        DesktopTheme.styleToolbar(topBar);
        topPanel.add(topBar, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);
        JScrollPane playerRankScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(playerRankScroll, table, "Player rankings");
        panel.add(playerRankScroll, BorderLayout.CENTER);
        JLabel prHint = new JLabel(HINT);
        prHint.setForeground(DesktopTheme.textSecondary());
        panel.add(prHint, BorderLayout.SOUTH);
        return panel;
    }

    /** Filter control focused by Ctrl+F (LeagueScreen.searchTarget). */
    private javax.swing.JComboBox<String> categoryBox;

    @Override
    public javax.swing.JComponent searchTarget() {
        return categoryBox;
    }
}