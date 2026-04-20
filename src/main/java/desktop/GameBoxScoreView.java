package desktop;

import simulation.Game;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Dialog that displays the box score for a single game.
 * Includes quarter-by-quarter scoring, team stats, and game details.
 */
public class GameBoxScoreView extends JDialog {

    private static final Font MONO = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 16);

    private final Game game;
    private final Team viewerTeam;

    public GameBoxScoreView(JFrame owner, Game game, Team viewerTeam) {
        super(owner, buildTitle(game), true);
        this.game = game;
        this.viewerTeam = viewerTeam;
        setSize(720, 520);
        setLayout(new BorderLayout());

        if (!game.hasPlayed || "BYE WEEK".equals(game.gameName)) {
            add(buildNotPlayedPanel(), BorderLayout.CENTER);
        } else {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Box Score", buildBoxScorePanel());
            tabs.addTab("Detailed Stats", buildDetailedStatsPanel());
            tabs.addTab("Play-by-Play", buildPlayByPlayPanel());
            add(tabs, BorderLayout.CENTER);
        }
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private static String buildTitle(Game game) {
        if ("BYE WEEK".equals(game.gameName)) return "BYE WEEK";
        if (!game.hasPlayed) {
            return game.awayTeam.getName() + " at " + game.homeTeam.getName() + " (Upcoming)";
        }
        return game.awayTeam.getName() + " " + game.awayScore
                + " - " + game.homeTeam.getName() + " " + game.homeScore;
    }

    private JPanel buildNotPlayedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        String text;
        if ("BYE WEEK".equals(game.gameName)) {
            text = "BYE WEEK\n\nNo game scheduled this week.";
        } else {
            text = "Upcoming Game\n\n"
                    + game.awayTeam.getName() + "  at  " + game.homeTeam.getName() + "\n\n"
                    + "Game type: " + game.gameName + "\n"
                    + "This game has not been played yet.";
        }
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(area, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBoxScorePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Scoreboard header
        panel.add(buildScoreboard(), BorderLayout.NORTH);

        // Game summary using the engine's built-in formatter
        String[] summary = game.getGameSummaryStr();
        StringBuilder sb = new StringBuilder();
        for (String s : summary) {
            if (s != null && !s.isEmpty()) {
                sb.append(s).append("\n");
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(MONO);
        area.setCaretPosition(0);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildScoreboard() {
        JPanel scoreboard = new JPanel(new GridLayout(3, 6, 8, 4));
        scoreboard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 70, 80), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        scoreboard.setBackground(new Color(28, 32, 38));

        // Header row
        addScoreCell(scoreboard, "TEAM", true);
        addScoreCell(scoreboard, "1ST", true);
        addScoreCell(scoreboard, "2ND", true);
        addScoreCell(scoreboard, "3RD", true);
        addScoreCell(scoreboard, "4TH", true);
        addScoreCell(scoreboard, game.numOT > 0 ? "OT" : "FINAL", true);

        // Away team row
        addScoreCell(scoreboard, game.awayTeam.getAbbr(), false);
        for (int q = 0; q < 4; q++) {
            addScoreCell(scoreboard, String.valueOf(game.awayQScore[q]), false);
        }
        addScoreCell(scoreboard, String.valueOf(game.awayScore), true);

        // Home team row
        addScoreCell(scoreboard, game.homeTeam.getAbbr(), false);
        for (int q = 0; q < 4; q++) {
            addScoreCell(scoreboard, String.valueOf(game.homeQScore[q]), false);
        }
        addScoreCell(scoreboard, String.valueOf(game.homeScore), true);

        return scoreboard;
    }

    private void addScoreCell(JPanel panel, String text, boolean highlighted) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setForeground(highlighted ? new Color(100, 200, 255) : Color.WHITE);
        label.setFont(new Font("SansSerif", highlighted ? Font.BOLD : Font.PLAIN, 14));
        panel.add(label);
    }

    private JPanel buildDetailedStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] summary = game.getGameSummaryStrV2();
        StringBuilder sb = new StringBuilder();
        for (String s : summary) {
            if (s != null && !s.isEmpty()) {
                sb.append(s).append("\n\n");
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(MONO);
        area.setCaretPosition(0);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPlayByPlayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String pbp = game.getPlayByPlayLog();
        JTextArea area = new JTextArea(pbp);
        area.setEditable(false);
        area.setFont(MONO);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);

        JLabel hint = new JLabel("Full play-by-play event log for this game.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel();
        footer.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        String type = game.gameName != null ? game.gameName : "Regular Season";
        footer.add(new JLabel("Game type: " + type));
        if (game.numOT > 0) {
            footer.add(new JLabel("  \u2022  " + game.numOT + " overtime(s)"));
        }
        return footer;
    }

    public static void show(JFrame owner, Game game, Team viewerTeam) {
        GameBoxScoreView view = new GameBoxScoreView(owner, game, viewerTeam);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);
    }
}
