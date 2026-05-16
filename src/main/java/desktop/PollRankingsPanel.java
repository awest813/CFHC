package desktop;

import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

public class PollRankingsPanel implements LeagueScreen {

    @Override
    public String title() {
        return "Poll Rankings";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"Rank", "Team", "Record", "Conf", "Poll Score", "Prestige", "Off Tal", "Def Tal"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 0, 5 -> Integer.class;
                    case 4, 6, 7 -> Float.class;
                    default -> String.class;
                };
            }
        };

        ctx.league().getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .forEach(t -> model.addRow(new Object[]{
                        t.getRankTeamPollScore(),
                        t.getName(),
                        t.getWins() + "-" + t.getLosses(),
                        t.getConfWins() + "-" + t.getConfLosses(),
                        t.getTeamPollScore(),
                        t.getTeamPrestige(),
                        t.getOffTalent(),
                        t.getDefTalent()
                }));

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) table.getValueAt(row, 1);
                        Team t = ctx.teamMap().get(name);
                        if (t != null) ctx.nav().openTeamDetail(t);
                    }
                }
            }
        });

        JScrollPane pollScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(pollScroll, table);
        panel.add(pollScroll, BorderLayout.CENTER);
        JLabel pollFoot = new JLabel("Double-click a team for details. Teams are sortable by column.");
        pollFoot.setForeground(DesktopTheme.textSecondary());
        panel.add(pollFoot, BorderLayout.SOUTH);
        return panel;
    }
}