package desktop;

import positions.Player;
import simulation.DataRecord;
import simulation.Game;
import simulation.LeagueRecord;
import simulation.PlayerRecord;
import simulation.Team;
import simulation.TeamHistoryRecord;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

/**
 * Detailed dialog for a single team. Combines roster, schedule, coaching-staff,
 * season history, team records, and depth chart in a tabbed layout.
 *
 * <p>Double-click a roster row to view player details. Double-click a schedule
 * row to view the game box score.
 */
public class TeamDetailView extends JDialog {

    private static final String[] ROSTER_COLUMNS   = {"Pos", "Name", "Yr", "OVR", "Pot", "IQ"};
    private static final String[] SCHEDULE_COLUMNS = {"Wk", "Opponent", "Home/Away", "Result"};
    private static final String[] HISTORY_COLUMNS  = {"Season", "Record", "Final Rank", "Pts For \u2013 Pts Against"};
    private static final String[] RECORDS_COLUMNS  = {"Record", "Value", "Holder", "Year"};
    private static final String[] DEPTH_COLUMNS    = {"Depth", "Pos", "Name", "Yr", "OVR"};

    private final JFrame ownerFrame;
    private final Team liveTeam;

    public TeamDetailView(JFrame owner, LeagueRecord.TeamRecord team, Team liveTeam) {
        super(owner, dialogTitle(team, liveTeam), true);
        this.ownerFrame = owner;
        this.liveTeam = liveTeam;
        setSize(920, 640);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Roster",  buildRosterTab(team));
        if (liveTeam != null) {
            tabs.addTab("Schedule", buildScheduleTab(liveTeam));
            tabs.addTab("Depth Chart", buildDepthChartTab(liveTeam));
        }
        tabs.addTab("Coaches", buildCoachesTab(team));
        if (liveTeam != null) {
            tabs.addTab("Team Stats", buildTeamStatsTab(liveTeam));
            tabs.addTab("Budget & Facilities", buildBudgetFacilitiesTab(liveTeam));
        }
        if (liveTeam != null && !liveTeam.getTeamHistory().isEmpty()) {
            tabs.addTab("History", buildHistoryTab(liveTeam));
        }
        if (!team.records().isEmpty()) {
            tabs.addTab("Records", buildRecordsTab(team.records()));
        }

        add(tabs, BorderLayout.CENTER);
        add(buildFooter(team, liveTeam), BorderLayout.SOUTH);
    }

    private static String dialogTitle(LeagueRecord.TeamRecord team, Team live) {
        if (live != null) {
            return team.name() + "  \u2014  " + live.getWins() + "-" + live.getLosses()
                    + "  (Poll #" + live.getRankTeamPollScore() + ")";
        }
        return team.name() + " Roster";
    }

    // -------------------------------------------------------------------------
    // Roster tab (double-click for player detail)
    // -------------------------------------------------------------------------

    private JScrollPane buildRosterTab(LeagueRecord.TeamRecord team) {
        DefaultTableModel model = new DefaultTableModel(ROSTER_COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 3, 4, 5 -> Integer.class;
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        team.roster().stream()
                .sorted(Comparator.comparingInt(PlayerRecord::ratOvr).reversed())
                .forEach(p -> model.addRow(new Object[]{
                        p.position(),
                        p.name(),
                        yearLabel(p.year()),
                        p.ratOvr(),
                        p.ratPot(),
                        p.ratIntelligence()
                }));

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(24);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setDefaultRenderer(Integer.class, new StripedRowRenderer());

        // Double-click to show player detail (from live team)
        if (liveTeam != null) {
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = table.rowAtPoint(e.getPoint());
                        if (row >= 0) {
                            String name = (String) table.getValueAt(row, 1);
                            Player p = findPlayerByName(liveTeam, name);
                            if (p != null) {
                                PlayerDetailView.show(ownerFrame, p);
                            }
                        }
                    }
                }
            });
        }

        return new JScrollPane(table);
    }

    // -------------------------------------------------------------------------
    // Schedule tab (double-click for box score)
    // -------------------------------------------------------------------------

    private JScrollPane buildScheduleTab(Team team) {
        DefaultTableModel model = new DefaultTableModel(SCHEDULE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int i = 0; i < team.getGameSchedule().size(); i++) {
            Game g = team.getGameSchedule().get(i);
            String opponent;
            String site;
            if ("BYE WEEK".equals(g.gameName)) {
                opponent = "BYE";
                site = "";
            } else if (g.homeTeam == team) {
                opponent = g.awayTeam.getName();
                site = "Home";
            } else {
                opponent = g.homeTeam.getName();
                site = "Away";
            }
            String result = formatResult(g, team);
            model.addRow(new Object[]{i + 1, opponent, site, result});
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setDefaultRenderer(Integer.class, new StripedRowRenderer());

        // Double-click a game row to show box score
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < team.getGameSchedule().size()) {
                        Game g = team.getGameSchedule().get(row);
                        GameBoxScoreView.show(ownerFrame, g, team);
                    }
                }
            }
        });

        return new JScrollPane(table);
    }

    private String formatResult(Game g, Team viewer) {
        if ("BYE WEEK".equals(g.gameName) || !g.hasPlayed) {
            if ("BYE WEEK".equals(g.gameName)) return "\u2014";
            return "scheduled";
        }
        int forScore = g.homeTeam == viewer ? g.homeScore : g.awayScore;
        int againstScore = g.homeTeam == viewer ? g.awayScore : g.homeScore;
        String outcome = forScore > againstScore ? "W" : (forScore < againstScore ? "L" : "T");
        return outcome + " " + forScore + "-" + againstScore;
    }

    // -------------------------------------------------------------------------
    // Depth Chart tab (editable for user-controlled teams)
    // -------------------------------------------------------------------------

    /** All position groups in display order. */
    private static final String[] POSITION_ORDER = {"QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"};

    private JPanel buildDepthChartTab(Team team) {
        JPanel wrapper = new JPanel(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(DEPTH_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        Runnable reloadDepthChart = () -> {
            model.setRowCount(0);
            for (String pos : POSITION_ORDER) {
                addDepthRow(model, team.getAllPlayers(), pos);
            }
        };
        reloadDepthChart.run();

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setDefaultRenderer(Integer.class, new StripedRowRenderer());

        if (liveTeam != null) {
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = table.rowAtPoint(e.getPoint());
                        if (row >= 0) {
                            String name = (String) table.getValueAt(row, 2);
                            Player p = findPlayerByName(liveTeam, name);
                            if (p != null) PlayerDetailView.show(ownerFrame, p);
                        }
                    }
                }
            });
        }

        wrapper.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add move-up/move-down buttons only for the user's team
        if (team.isUserControlled()) {
            JPanel buttonPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
            JButton upBtn = new JButton("\u25B2 Move Up");
            upBtn.setToolTipText("Promote the selected player one depth-chart slot");
            JButton downBtn = new JButton("\u25BC Move Down");
            downBtn.setToolTipText("Demote the selected player one depth-chart slot");

            upBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) return;
                String pos = (String) table.getValueAt(row, 1);
                String name = (String) table.getValueAt(row, 2);
                moveDepthChart(team, pos, name, -1);
                reloadDepthChart.run();
                if (row > 0) table.setRowSelectionInterval(row - 1, row - 1);
            });
            downBtn.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (row < 0) return;
                String pos = (String) table.getValueAt(row, 1);
                String name = (String) table.getValueAt(row, 2);
                moveDepthChart(team, pos, name, 1);
                reloadDepthChart.run();
                if (row < table.getRowCount() - 1) table.setRowSelectionInterval(row + 1, row + 1);
            });

            buttonPanel.add(upBtn);
            buttonPanel.add(downBtn);
            buttonPanel.add(new JLabel("  Select a row and use the buttons to reorder the depth chart."));
            wrapper.add(buttonPanel, BorderLayout.SOUTH);
        }

        return wrapper;
    }

    /**
     * Moves a player up or down one slot in their position's depth chart.
     *
     * @param team      the team whose depth chart to modify
     * @param pos       the position group abbreviation
     * @param name      the player's name
     * @param direction -1 for up (promote), +1 for down (demote)
     */
    private void moveDepthChart(Team team, String pos, String name, int direction) {
        java.util.List<? extends Player> posList = team.getPositionList(pos);
        if (posList == null) return;
        for (int i = 0; i < posList.size(); i++) {
            if (posList.get(i).name.equals(name)) {
                int target = i + direction;
                if (target >= 0 && target < posList.size()) {
                    team.swapDepthChartOrder(pos, i, target);
                }
                return;
            }
        }
    }

    private void addDepthRow(DefaultTableModel model, List<Player> allPlayers, String pos) {
        int depth = 1;
        for (Player p : allPlayers) {
            if (p.position.equals(pos)) {
                model.addRow(new Object[]{
                        depth == 1 ? "Starter" : "Backup " + depth,
                        pos, p.name, yearLabel(p.year), p.ratOvr
                });
                depth++;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Coaches tab
    // -------------------------------------------------------------------------

    private JPanel buildCoachesTab(LeagueRecord.TeamRecord team) {
        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.GridLayout(0, 1, 6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        panel.add(coachLine("Head Coach",
                team.headCoach().name(), team.headCoach().ratOvr()));
        panel.add(coachLine("Offensive Coordinator",
                team.offenseCoach().name(), team.offenseCoach().ratOvr()));
        panel.add(coachLine("Defensive Coordinator",
                team.defenseCoach().name(), team.defenseCoach().ratOvr()));
        return panel;
    }

    private JLabel coachLine(String role, String name, int ovr) {
        JLabel label = new JLabel(String.format("%-25s %s  (Overall %d)", role + ":", name, ovr));
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        return label;
    }

    // -------------------------------------------------------------------------
    // Team Stats tab
    // -------------------------------------------------------------------------

    private JScrollPane buildTeamStatsTab(Team team) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Stat", "Value", "Rank"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int games = Math.max(1, team.numGames());
        model.addRow(new Object[]{"Record", team.getWins() + "-" + team.getLosses(), ""});
        model.addRow(new Object[]{"Conf Record", team.getConfWins() + "-" + team.getConfLosses(), ""});
        model.addRow(new Object[]{"Poll Rank", "#" + team.getRankTeamPollScore(), ""});
        model.addRow(new Object[]{"Prestige", team.getTeamPrestige(), "#" + team.getRankTeamPrestige()});
        model.addRow(new Object[]{"", "", ""});
        model.addRow(new Object[]{"Points/Game", String.format("%.1f", (float) team.getTeamPoints() / games), "#" + team.getRankTeamPoints()});
        model.addRow(new Object[]{"Opp Points/Game", String.format("%.1f", (float) team.getTeamOppPoints() / games), "#" + team.getRankTeamOppPoints()});
        model.addRow(new Object[]{"Total Yards/Game", String.format("%.1f", (float) team.getTeamYards() / games), "#" + team.getRankTeamYards()});
        model.addRow(new Object[]{"Opp Yards/Game", String.format("%.1f", (float) team.getTeamOppYards() / games), "#" + team.getRankTeamOppYards()});
        model.addRow(new Object[]{"Pass Yards/Game", String.format("%.1f", (float) team.getTeamPassYards() / games), "#" + team.getRankTeamPassYards()});
        model.addRow(new Object[]{"Rush Yards/Game", String.format("%.1f", (float) team.getTeamRushYards() / games), "#" + team.getRankTeamRushYards()});
        model.addRow(new Object[]{"", "", ""});
        model.addRow(new Object[]{"Offensive Talent", String.format("%.1f", team.getOffTalent()), "#" + team.getRankTeamOffTalent()});
        model.addRow(new Object[]{"Defensive Talent", String.format("%.1f", team.getDefTalent()), "#" + team.getRankTeamDefTalent()});
        model.addRow(new Object[]{"Chemistry", String.format("%.1f", team.getTeamChemistry()), "#" + team.getRankTeamChemistry()});
        model.addRow(new Object[]{"Discipline", team.getTeamDisciplineScore() + "%", "#" + team.getRankTeamDisciplineScore()});
        model.addRow(new Object[]{"", "", ""});
        model.addRow(new Object[]{"SOS", String.format("%.3f", team.getSOS()), "#" + team.getRankTeamSOS()});
        model.addRow(new Object[]{"RPI", String.format("%.3f", team.getRPI()), "#" + team.getRankTeamRPI()});

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        return new JScrollPane(table);
    }

    // -------------------------------------------------------------------------
    // Budget & Facilities tab
    // -------------------------------------------------------------------------

    private JPanel buildBudgetFacilitiesTab(Team team) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        DefaultTableModel model = new DefaultTableModel(new String[]{"Category", "Value"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        model.addRow(new Object[]{"Team Budget", "$" + String.format("%,d", team.getTeamBudget())});
        model.addRow(new Object[]{"Recruiting Budget", "$" + String.format("%,d", team.getTeamRecruitBudget())});
        model.addRow(new Object[]{"Discipline Budget", "$" + String.format("%,d", team.getTeamDisciplineBudget())});
        model.addRow(new Object[]{"", ""});
        model.addRow(new Object[]{"Facilities Level", team.getTeamFacilities()});
        model.addRow(new Object[]{"Discipline Score", team.getTeamDisciplineScore() + "%"});
        model.addRow(new Object[]{"", ""});
        model.addRow(new Object[]{"Prestige", team.getTeamPrestige()});
        model.addRow(new Object[]{"Prestige Rank", "#" + team.getRankTeamPrestige()});

        // Show facility upgrade cost info
        int baselineCost = 17_500;
        int nextUpgradeCost = baselineCost * (team.getTeamFacilities() + 1);
        model.addRow(new Object[]{"", ""});
        model.addRow(new Object[]{"Next Facility Upgrade Cost", "$" + String.format("%,d", nextUpgradeCost)});
        boolean canAfford = team.getTeamBudget() > nextUpgradeCost;
        model.addRow(new Object[]{"Can Afford Upgrade?", canAfford ? "Yes" : "No"});

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "Facility upgrades happen automatically at end of season if the team has enough budget.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    // -------------------------------------------------------------------------
    // History tab
    // -------------------------------------------------------------------------

    private JScrollPane buildHistoryTab(Team team) {
        DefaultTableModel model = new DefaultTableModel(HISTORY_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        List<TeamHistoryRecord> history = team.getTeamHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            TeamHistoryRecord hr = history.get(i);
            String rankStr = hr.rank() > 0 ? "#" + hr.rank() : "\u2014";
            String pts = hr.pointsFor() + " \u2013 " + hr.pointsAgainst();
            model.addRow(new Object[]{hr.year(), hr.wins() + "-" + hr.losses(), rankStr, pts});
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setDefaultRenderer(Integer.class, new StripedRowRenderer());
        return new JScrollPane(table);
    }

    // -------------------------------------------------------------------------
    // Team Records tab
    // -------------------------------------------------------------------------

    private JScrollPane buildRecordsTab(List<DataRecord> records) {
        DefaultTableModel model = new DefaultTableModel(RECORDS_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (DataRecord dr : records) {
            if (dr == null) continue;
            String holder = LeagueHomeView.formatHolder(dr.holder());
            String value = dr.value() == (int) dr.value()
                    ? String.valueOf((int) dr.value())
                    : String.format("%.2f", dr.value());
            model.addRow(new Object[]{dr.key(), value, holder, dr.year()});
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setDefaultRenderer(Object.class, new StripedRowRenderer());
        table.setDefaultRenderer(Integer.class, new StripedRowRenderer());
        return new JScrollPane(table);
    }

    // -------------------------------------------------------------------------
    // Footer
    // -------------------------------------------------------------------------

    private JPanel buildFooter(LeagueRecord.TeamRecord team, Team live) {
        JPanel footer = new JPanel();
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        String recordText;
        if (live != null) {
            recordText = String.format(
                    "%s (%s)  \u2022  Record %d-%d  \u2022  Conf %d-%d  \u2022  Prestige %d  \u2022  Roster %d  \u2022  Double-click rows for details",
                    team.name(), team.abbr(), live.getWins(), live.getLosses(),
                    live.getConfWins(), live.getConfLosses(), team.prestige(), team.roster().size());
        } else {
            recordText = String.format("%s (%s)  \u2022  Prestige %d  \u2022  Roster %d",
                    team.name(), team.abbr(), team.prestige(), team.roster().size());
        }
        footer.add(new JLabel(recordText));
        return footer;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static String yearLabel(int year) {
        return switch (year) {
            case 0 -> "RS";
            case 1 -> "FR";
            case 2 -> "SO";
            case 3 -> "JR";
            case 4 -> "SR";
            case 5 -> "5SR";
            default -> String.valueOf(year);
        };
    }

    private static Player findPlayerByName(Team team, String name) {
        if (team == null || name == null) return null;
        for (Player p : team.getAllPlayers()) {
            if (name.equals(p.name)) return p;
        }
        return null;
    }

    public static void show(JFrame owner, LeagueRecord.TeamRecord team, Team live) {
        TeamDetailView view = new TeamDetailView(owner, team, live);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);
    }
}
