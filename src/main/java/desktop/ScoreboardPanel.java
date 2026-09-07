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
        table.getColumnModel().getColumn(0).setPreferredWidth(340);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
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
                    parseAndAddScoreRow(s, model);
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
                            showBoxScoreFromMatchup(matchup, currentWeek, ctx);
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

    private static void parseAndAddScoreRow(String s, DefaultTableModel model) {
        if (s == null || s.trim().isEmpty()) return;

        String header = "Game";
        String body = s;
        int gt = s.indexOf('>');
        if (gt >= 0) {
            header = s.substring(0, gt).trim();
            body = s.substring(gt + 1).trim();
        }

        if (body.isEmpty() || body.startsWith("No games")) {
            model.addRow(new Object[]{body.isEmpty() ? "No game data" : body, "", header});
            return;
        }

        String[] lines = body.split("\n");
        if (lines.length >= 2) {
            String awayLine = lines[0].trim();
            String homeLine = lines[1].trim();

            int lastSpaceA = awayLine.lastIndexOf(' ');
            int lastSpaceH = homeLine.lastIndexOf(' ');

            String awayName = awayLine;
            String awayScoreStr = "";
            if (lastSpaceA > 0) {
                String potScore = awayLine.substring(lastSpaceA + 1).trim();
                if (potScore.matches("\\d+")) {
                    awayScoreStr = potScore;
                    awayName = awayLine.substring(0, lastSpaceA).trim();
                }
            }

            String homeName = homeLine;
            String homeScoreStr = "";
            if (lastSpaceH > 0) {
                String potScore = homeLine.substring(lastSpaceH + 1).trim();
                if (potScore.matches("\\d+")) {
                    homeScoreStr = potScore;
                    homeName = homeLine.substring(0, lastSpaceH).trim();
                }
            }

            String matchup = awayName + " at " + homeName;
            String result = "";
            if (!awayScoreStr.isEmpty() && !homeScoreStr.isEmpty()) {
                int scoreA = Integer.parseInt(awayScoreStr);
                int scoreH = Integer.parseInt(homeScoreStr);
                result = scoreA > scoreH
                        ? awayName + " " + scoreA + ", " + homeName + " " + scoreH
                        : homeName + " " + scoreH + ", " + awayName + " " + scoreA;
            } else {
                result = awayScoreStr.isEmpty() ? "Scheduled" : awayScoreStr + " - " + homeScoreStr;
            }

            model.addRow(new Object[]{matchup, result, header});
        } else {
            String[] parts = s.split(",");
            if (parts.length >= 3) {
                model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
            } else {
                model.addRow(new Object[]{body, "", header});
            }
        }
    }

    private static void showBoxScoreFromMatchup(String matchup, int week, LeagueScreenContext ctx) {
        if (matchup == null) return;
        int atIdx = matchup.lastIndexOf(" at ");
        if (atIdx < 0) return;
        String teamA = matchup.substring(0, atIdx).replaceAll("^#\\d+\\s*", "").replaceFirst("\\s+\\d+$", "").trim();
        String homePart = matchup.substring(atIdx + 4);
        String teamH = homePart.replaceAll("^#\\d+\\s*", "").replaceFirst("\\s+\\d+.*", "").trim();

        Team away = ctx.teamMap() != null ? ctx.teamMap().get(teamA) : null;
        Team home = ctx.teamMap() != null ? ctx.teamMap().get(teamH) : null;

        if (away != null && home != null && away.getGameSchedule() != null) {
            Game fallback = null;
            for (Game g : away.getGameSchedule()) {
                if ((g.homeTeam == home && g.awayTeam == away) || (g.homeTeam == away && g.awayTeam == home)) {
                    if (g.hasPlayed) {
                        int gWeek = g.week > 0 ? g.week : away.getGameSchedule().indexOf(g) + 1;
                        if (gWeek == week) {
                            GameBoxScoreView.show(ctx.parent(), g, ctx.league().userTeam);
                            return;
                        }
                        fallback = g;
                    }
                }
            }
            if (fallback != null) {
                GameBoxScoreView.show(ctx.parent(), fallback, ctx.league().userTeam);
            }
        }
    }
}