package desktop;

import simulation.Conference;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

        JPanel wrapper = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(wrapper);
        wrapper.add(splitPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildTopTeamsSidebar(LeagueScreenContext ctx) {
        JPanel sidebar = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(sidebar);
        sidebar.setBorder(DesktopTheme.titledBorder("Top 25 (Poll)"));

        DefaultListModel<Team> teamModel = new DefaultListModel<>();
        ctx.league().getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(25)
                .forEach(teamModel::addElement);

        JList<Team> teamList = new JList<>(teamModel);
        teamList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleListShell(teamList);
        teamList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Team t = (Team) value;
                String label = String.format(Locale.ROOT, "#%-3d %-22s (%d-%d)  Pres %d",
                        t.getRankTeamPollScore(), t.getName(), t.getWins(), t.getLosses(), t.getTeamPrestige());
                Component c = super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                if (!(c instanceof JLabel jl)) {
                    return c;
                }
                jl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    DesktopTheme.decorateListCellLabel(jl, index, true, null);
                } else if (t == ctx.league().userTeam) {
                    DesktopTheme.decorateListCellLabel(jl, index, false, DesktopTheme.userTeamRowTint());
                } else {
                    DesktopTheme.decorateListCellLabel(jl, index, false, null);
                }
                return jl;
            }
        });
        teamList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Team sel = teamList.getSelectedValue();
                    if (sel != null) ctx.nav().openTeamDetail(sel);
                }
            }
        });

        JScrollPane teamScroll = new JScrollPane(teamList);
        teamScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        teamScroll.setOpaque(true);
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

        String headerText = " " + conf.confName;
        if (conf.confTV) {
            headerText += "  (" + conf.getTVName() + ")";
        }
        JLabel label = new JLabel(headerText);
        label.setOpaque(true);
        label.setBackground(DesktopTheme.conferenceHeaderBackground());
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
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

        javax.swing.table.TableCellRenderer confRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!(c instanceof JLabel jl)) {
                    return c;
                }
                jl.setOpaque(true);
                jl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    c.setBackground(DesktopTheme.selectionAccent());
                    c.setForeground(Color.WHITE);
                    return c;
                }
                String name = (String) tbl.getValueAt(row, 1);
                if (ctx.league().userTeam != null && ctx.league().userTeam.getName().equals(name)) {
                    c.setBackground(DesktopTheme.userTeamRowTint());
                } else {
                    c.setBackground(row % 2 == 0 ? DesktopTheme.tableBase() : DesktopTheme.tableStripe());
                }
                c.setForeground(DesktopTheme.textPrimary());
                return c;
            }
        };
        confTable.setDefaultRenderer(Object.class, confRenderer);
        confTable.setDefaultRenderer(Integer.class, confRenderer);

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
        DesktopTheme.styleDataTableInScroll(confScroll, confTable);
        panel.add(confScroll, BorderLayout.CENTER);
        return panel;
    }
}