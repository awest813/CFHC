package desktop;

import simulation.Game;
import simulation.League;
import simulation.SeasonPresentation;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ScoreboardPanel implements LeagueScreen {

    private int currentWeek;

    @Override
    public String title() {
        return "Scoreboard";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        currentWeek = Math.max(0, ctx.league().currentWeek);
        return buildContent(ctx);
    }

    @Override
    public void refresh(LeagueScreenContext ctx) {
        currentWeek = Math.max(0, ctx.league().currentWeek);
    }

    private JPanel buildContent(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(new String[]{"Matchup", "Result", "Type"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        StripedRowRenderer.install(table);

        final String userTeamName = ctx.league().userTeam != null ? ctx.league().userTeam.getName() : null;
        final Color userTeamTint = DesktopTheme.userTeamRowTint();
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                java.awt.Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (!isSelected && c instanceof javax.swing.JLabel jl && userTeamName != null && col == 0 && value != null) {
                    String matchup = value.toString();
                    if (matchup.contains(userTeamName)) {
                        c.setBackground(userTeamTint);
                        jl.setFont(jl.getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(DesktopTheme.buildScreenHeader("Scoreboard", "Browse completed weeks and open box scores."), BorderLayout.NORTH);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton prevBtn = new JButton("\u25C0 Previous");
        JButton nextBtn = new JButton("Next \u25B6");
        JButton currentBtn = new JButton("Current Week");
        JLabel weekLabel = new JLabel();
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        weekLabel.setForeground(DesktopTheme.textPrimary());

        JLabel weekTypeLabel = new JLabel();
        weekTypeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        weekTypeLabel.setForeground(DesktopTheme.textSecondary());

        Runnable updateScoreboard = () -> {
            weekLabel.setText(currentWeek <= 0
                    ? SeasonPresentation.getSeasonCycleLabel(ctx.league())
                    : "Week " + currentWeek);
            weekTypeLabel.setText(getWeekType(currentWeek, ctx.league()));
            model.setRowCount(0);
            List<List<String>> scores = ctx.league().getWeeklyScores();
            if (scores != null && currentWeek >= 0 && currentWeek < scores.size()) {
                for (String s : scores.get(currentWeek)) {
                    if (s == null) continue;
                    String[] parts = s.split(",");
                    if (parts.length >= 3) model.addRow(new Object[]{parts[0], parts[1], parts[2]});
                    else if (parts.length == 1) model.addRow(new Object[]{parts[0], "", "Game"});
                }
            }
            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{"No recorded games for this week.", "", ""});
            }
            prevBtn.setEnabled(currentWeek > 0);
            nextBtn.setEnabled(currentWeek < ctx.league().currentWeek);
            currentBtn.setEnabled(currentWeek != ctx.league().currentWeek);
        };

        prevBtn.addActionListener(e -> { if(currentWeek > 0) { currentWeek--; updateScoreboard.run(); } });
        nextBtn.addActionListener(e -> { if(currentWeek < ctx.league().currentWeek) { currentWeek++; updateScoreboard.run(); } });
        currentBtn.addActionListener(e -> { currentWeek = Math.max(0, ctx.league().currentWeek); updateScoreboard.run(); });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String matchup = String.valueOf(table.getValueAt(row, 0));
                        if (matchup.contains(" at ")) {
                            showBoxScoreFromMatchup(matchup, ctx);
                        }
                    }
                }
            }
        });

        navPanel.add(prevBtn);
        navPanel.add(weekLabel);
        navPanel.add(weekTypeLabel);
        navPanel.add(nextBtn);
        navPanel.add(currentBtn);

        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if(currentWeek > 0) { currentWeek--; updateScoreboard.run(); }
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    if(currentWeek < ctx.league().currentWeek) { currentWeek++; updateScoreboard.run(); }
                } else if (e.getKeyCode() == KeyEvent.VK_HOME) {
                    currentWeek = Math.max(0, ctx.league().currentWeek);
                    updateScoreboard.run();
                }
            }
        });
        panel.setFocusTraversalKeysEnabled(false);

        DesktopTheme.styleToolbar(navPanel);
        updateScoreboard.run();

        topPanel.add(navPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);
        JScrollPane scoreScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(scoreScroll, table, "Scoreboard");
        panel.add(scoreScroll, BorderLayout.CENTER);
        JLabel scoreHint = new JLabel("Double-click any game to view the box score.");
        scoreHint.setForeground(DesktopTheme.textSecondary());
        panel.add(scoreHint, BorderLayout.SOUTH);
        return panel;
    }

    private static String getWeekType(int week, League league) {
        return SeasonPresentation.getScoreboardWeekType(week, league.regSeasonWeeks);
    }

    private static void showBoxScoreFromMatchup(String matchup, LeagueScreenContext ctx) {
        if (matchup == null) return;
        int atIdx = matchup.lastIndexOf(" at ");
        if (atIdx < 0) return;
        String teamA = matchup.substring(0, atIdx).replaceFirst("\\s+\\d+$", "").trim();
        String homePart = matchup.substring(atIdx + 4);
        String teamH = homePart.replaceFirst("\\s+\\d+.*", "").trim();

        Team away = ctx.teamMap().get(teamA);
        Team home = ctx.teamMap().get(teamH);

        if (away != null && home != null) {
            for (Game g : away.getGameSchedule()) {
                if (g.homeTeam == home || g.awayTeam == home) {
                    GameBoxScoreView.show(ctx.parent(), g, ctx.league().userTeam);
                    return;
                }
            }
        }
    }
}