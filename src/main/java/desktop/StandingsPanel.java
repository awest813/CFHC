package desktop;

import simulation.Conference;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StandingsPanel implements LeagueScreen {

    @Override
    public String title() {
        return "Standings";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setOpaque(true);
        splitPane.setBackground(DesktopTheme.windowBackground());
        splitPane.setLeftComponent(buildTopTeamsSidebar(ctx));
        JScrollPane gridScroll = new JScrollPane(buildConferenceGrid(ctx));
        gridScroll.getViewport().setBackground(DesktopTheme.windowBackground());
        gridScroll.setOpaque(true);
        splitPane.setRightComponent(gridScroll);

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        DesktopTheme.styleTabRoot(wrapper);
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        wrapper.add(DesktopTheme.buildScreenHeader("Standings",
                "Conference grids and top-25 poll snapshot. Double-click a team for details."), BorderLayout.NORTH);
        wrapper.add(splitPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildTopTeamsSidebar(LeagueScreenContext ctx) {
        JPanel sidebar = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(sidebar);
        sidebar.setBorder(DesktopTheme.titledBorder("Top 25 (Poll)"));

        // A real table (same renderer as every other grid) instead of a JList of
        // space-padded strings: a proportional font made those columns zig-zag.
        DefaultTableModel pollModel = new DefaultTableModel(new String[]{"#", "Team", "W-L", "Pres"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 0, 3 -> Integer.class;
                    default -> String.class;
                };
            }
        };
        List<Team> top = new ArrayList<>();
        ctx.league().getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(25)
                .forEach(t -> {
                    top.add(t);
                    pollModel.addRow(new Object[]{
                            t.getRankTeamPollScore(),
                            t.getName(),
                            t.getWins() + "-" + t.getLosses(),
                            t.getTeamPrestige()
                    });
                });

        JTable pollTable = new JTable(pollModel);
        pollTable.setRowHeight(22);
        pollTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pollTable.setFillsViewportHeight(true);
        pollTable.setShowVerticalLines(false);
        pollTable.getTableHeader().setReorderingAllowed(false);
        pollTable.getColumnModel().getColumn(0).setPreferredWidth(36);
        pollTable.getColumnModel().getColumn(0).setMaxWidth(44);
        pollTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        pollTable.getColumnModel().getColumn(2).setPreferredWidth(52);
        pollTable.getColumnModel().getColumn(2).setMaxWidth(64);
        pollTable.getColumnModel().getColumn(3).setPreferredWidth(48);
        pollTable.getColumnModel().getColumn(3).setMaxWidth(56);
        StripedRowRenderer.installWithTeamColors(pollTable, ctx.teamMap(), 1);
        StripedRowRenderer.setNumericColumns(pollTable, 2);
        Team userTeam = ctx.league().userTeam;
        StripedRowRenderer.setRowEmphasis(pollTable, modelRow -> top.get(modelRow) == userTeam);
        pollTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = pollTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        Team t = top.get(pollTable.convertRowIndexToModel(row));
                        ctx.nav().openTeamDetail(t);
                    }
                }
            }
        });

        JScrollPane teamScroll = new JScrollPane(pollTable);
        DesktopTheme.styleDataTableInScroll(teamScroll, pollTable, "Top 25 poll");
        sidebar.add(teamScroll, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildConferenceGrid(LeagueScreenContext ctx) {
        JPanel content = new JPanel(new GridLayout(0, 2, 10, 10));
        content.setOpaque(true);
        content.setBackground(DesktopTheme.windowBackground());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Conference conf : ctx.league().getConferences()) {
            content.add(buildConferencePanel(conf, ctx));
        }
        return content;
    }

    private JPanel buildConferencePanel(Conference conf, LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));

        String headerText = conf.confName;
        if (conf.confTV) {
            headerText += "  (" + conf.getTVName() + ")";
        }
        JLabel label = new JLabel(headerText);
        label.setOpaque(true);
        label.setBackground(DesktopTheme.conferenceHeaderBackground());
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        panel.add(label, BorderLayout.NORTH);

        List<Team> sorted = new ArrayList<>(conf.getTeams());
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.getConfWins(), a.getConfWins());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(a.getConfLosses(), b.getConfLosses());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getWins(), a.getWins());
        });

        String[] cols = {"#", "Team", "Record", "Conf", "Pres"};
        DefaultTableModel confModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 0, 4 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        for (Team t : sorted) {
            confModel.addRow(new Object[]{
                    t.getRankTeamPollScore() <= 25 ? t.getRankTeamPollScore() : null,
                    t.getName(),
                    t.getWins() + "-" + t.getLosses(),
                    t.getConfWins() + "-" + t.getConfLosses(),
                    t.getTeamPrestige()
            });
        }

        JTable confTable = new JTable(confModel);
        confTable.setAutoCreateRowSorter(true);
        confTable.setRowHeight(20);
        confTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        confTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        confTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        confTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        confTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        confTable.getColumnModel().getColumn(4).setPreferredWidth(40);

        StripedRowRenderer.installWithTeamColors(confTable, ctx.teamMap(), 1);
        StripedRowRenderer.setNumericColumns(confTable, 2, 3);
        Team userTeam = ctx.league().userTeam;
        StripedRowRenderer.setRowEmphasis(confTable, modelRow -> userTeam != null
                && userTeam.getName().equals(confModel.getValueAt(modelRow, 1)));

        confTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = confTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) confTable.getValueAt(row, 1);
                        Team t = ctx.teamMap().get(name);
                        if (t != null) ctx.nav().openTeamDetail(t);
                    }
                }
            }
        });
        JScrollPane confScroll = new JScrollPane(confTable);
        DesktopTheme.styleDataTableInScroll(confScroll, confTable, "Conference standings");
        panel.add(confScroll, BorderLayout.CENTER);
        return panel;
    }
}