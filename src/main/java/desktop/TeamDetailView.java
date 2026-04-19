package desktop;

import simulation.DataRecord;
import simulation.Game;
import simulation.LeagueRecord;
import simulation.PlayerRecord;
import simulation.Team;
import simulation.TeamHistoryRecord;
import simulation.TeamRecords;

import javax.swing.BorderFactory;
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
import java.util.Comparator;
import java.util.List;

/**
 * Detailed dialog for a single team. Combines roster, schedule, coaching-staff,
 * season history and team records in a tabbed layout.
 */
public class TeamDetailView extends JDialog {

    private static final String[] ROSTER_COLUMNS   = {"Pos", "Name", "Yr", "OVR", "Pot", "IQ"};
    private static final String[] SCHEDULE_COLUMNS = {"Wk", "Opponent", "Home/Away", "Result"};
    private static final String[] HISTORY_COLUMNS  = {"Season", "Record", "Final Rank", "Pts For – Pts Against"};
    private static final String[] RECORDS_COLUMNS  = {"Record", "Value", "Holder", "Year"};

    public TeamDetailView(JFrame owner, LeagueRecord.TeamRecord team, Team liveTeam) {
        super(owner, dialogTitle(team, liveTeam), true);
        setSize(860, 600);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Roster",  buildRosterTab(team));
        if (liveTeam != null) {
            tabs.addTab("Schedule", buildScheduleTab(liveTeam));
        }
        tabs.addTab("Coaches", buildCoachesTab(team));
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
            return team.name() + "  \u2014  " + live.wins + "-" + live.losses;
        }
        return team.name() + " Roster";
    }

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
        table.setRowHeight(22);
        return new JScrollPane(table);
    }

    private JScrollPane buildScheduleTab(Team liveTeam) {
        DefaultTableModel model = new DefaultTableModel(SCHEDULE_COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int i = 0; i < liveTeam.gameSchedule.size(); i++) {
            Game g = liveTeam.gameSchedule.get(i);
            String opponent;
            String site;
            if ("BYE WEEK".equals(g.gameName)) {
                opponent = "BYE";
                site = "";
            } else if (g.homeTeam == liveTeam) {
                opponent = g.awayTeam.name;
                site = "Home";
            } else {
                opponent = g.homeTeam.name;
                site = "Away";
            }
            String result = formatResult(g, liveTeam);
            model.addRow(new Object[]{i + 1, opponent, site, result});
        }

        JTable table = new JTable(model);
        table.setRowHeight(22);
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
    // History tab
    // -------------------------------------------------------------------------

    private JScrollPane buildHistoryTab(Team liveTeam) {
        DefaultTableModel model = new DefaultTableModel(HISTORY_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        List<TeamHistoryRecord> history = liveTeam.getTeamHistory();
        // Show most-recent season first
        for (int i = history.size() - 1; i >= 0; i--) {
            TeamHistoryRecord hr = history.get(i);
            String rankStr = hr.rank() > 0 ? "#" + hr.rank() : "—";
            String pts = hr.pointsFor() + " – " + hr.pointsAgainst();
            model.addRow(new Object[]{hr.year(), hr.wins() + "-" + hr.losses(), rankStr, pts});
        }

        JTable table = new JTable(model);
        table.setRowHeight(22);
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
        table.setRowHeight(22);
        return new JScrollPane(table);
    }

    // -------------------------------------------------------------------------
    // Footer
    // -------------------------------------------------------------------------

    private JPanel buildFooter(LeagueRecord.TeamRecord team, Team live) {
        JPanel footer = new JPanel();
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        String recordText = live != null
                ? String.format("%s (%s)  \u2022  Record %d-%d  \u2022  Prestige %d  \u2022  Roster %d",
                        team.name(), team.abbr(), live.wins, live.losses, team.prestige(), team.roster().size())
                : String.format("%s (%s)  \u2022  Prestige %d  \u2022  Roster %d",
                        team.name(), team.abbr(), team.prestige(), team.roster().size());
        footer.add(new JLabel(recordText));
        return footer;
    }

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

    public static void show(JFrame owner, LeagueRecord.TeamRecord team, Team live) {
        TeamDetailView view = new TeamDetailView(owner, team, live);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);
    }
}
