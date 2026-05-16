package desktop;

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

public class TeamRankingsPanel implements LeagueScreen {

    private static final String TAG = "TeamRankingsPanel";

    private static final String[] CATEGORIES = {
            "Poll Score", "Prestige", "RPI", "Strength of Schedule", "Strength of Wins",
            "Points/Game", "Opp Points/Game", "Yards/Game", "Opp Yards/Game",
            "Pass Yards/Game", "Rush Yards/Game", "Opp Pass YPG", "Opp Rush YPG",
            "Turnover Diff", "Off. Talent", "Def. Talent", "Chemistry",
            "Recruiting Class", "Discipline", "Budget", "Facilities",
            "Coach Overall", "Coach Score"
    };

    @Override
    public String title() {
        return "Team Rankings";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Team", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String cellVal = String.valueOf(table.getValueAt(row, 1));
                        String teamName = cellVal.split("\n")[0].trim();
                        Team t = ctx.teamMap().get(teamName);
                        if (t != null) ctx.nav().openTeamDetail(t);
                    }
                }
            }
        });

        Runnable loadRankings = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                java.util.List<String> rankings = ctx.league().getTeamRankingsStr(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading team rankings", ex);
            }
        };

        categoryBox.addActionListener(e -> loadRankings.run());
        loadRankings.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Category: "));
        topBar.add(categoryBox);
        DesktopTheme.styleToolbar(topBar);
        panel.add(topBar, BorderLayout.NORTH);
        JScrollPane teamRankScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(teamRankScroll, table);
        panel.add(teamRankScroll, BorderLayout.CENTER);
        return panel;
    }
}