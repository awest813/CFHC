package desktop;

import simulation.TeamMoraleSnapshot;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Swing dashboard card component for TEAM MORALE.
 * Renders the live {@link TeamMoraleSnapshot} from the engine: chemistry comes
 * from season-long chemistry (the same value the game sim feeds into its
 * coaching advantage), leadership from the top players' character, and buy-in
 * from staff discipline, the active win streak, and the coach's culture skill.
 */
public class TeamMoraleCard extends CustomCardPanel {

    private final simulation.Team team;
    private final JLabel statusTxt = new JLabel("-", JLabel.CENTER);
    private final JPanel smiley = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int overall = overall();

            g2.setColor(new Color(0, 230, 118, 30));
            g2.fillOval(2, 2, getWidth() - 5, getHeight() - 5);
            g2.setColor(moraleColor(overall));
            g2.drawOval(2, 2, getWidth() - 5, getHeight() - 5);

            // Eyes & Mouth — the smile flattens as morale drops
            g2.fillOval(12, 14, 4, 4);
            g2.fillOval(24, 14, 4, 4);
            if (overall >= 55) {
                g2.drawArc(12, 18, 16, 12, 180, 180);
            } else if (overall >= 40) {
                g2.drawLine(12, 24, 28, 24);
            } else {
                g2.drawArc(12, 26, 16, 10, 0, 180);
            }

            g2.dispose();
        }
    };
    private final JPanel checklist = new JPanel(new GridLayout(4, 1, 0, 2));
    private final JPanel sliders = new JPanel(new GridLayout(3, 1, 0, 4));

    public TeamMoraleCard(simulation.Team team) {
        super("Team Morale");
        this.team = team;
        JPanel content = getContentArea();

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);

        // Top Row: Smiley Gauge + Checklist
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        JPanel gaugeCol = new JPanel(new BorderLayout(0, 4));
        gaugeCol.setOpaque(false);

        smiley.setPreferredSize(new Dimension(40, 40));
        smiley.setOpaque(false);

        statusTxt.setFont(new Font("SansSerif", Font.BOLD, 14));

        gaugeCol.add(smiley, BorderLayout.CENTER);
        gaugeCol.add(statusTxt, BorderLayout.SOUTH);
        topRow.add(gaugeCol, BorderLayout.WEST);

        checklist.setOpaque(false);
        topRow.add(checklist, BorderLayout.CENTER);
        body.add(topRow, BorderLayout.NORTH);

        sliders.setOpaque(false);
        body.add(sliders, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);

        render(snapshot());
    }

    private TeamMoraleSnapshot snapshot() {
        return team != null ? team.getTeamMoraleSnapshot() : new TeamMoraleSnapshot(50, 50, 50);
    }

    private int overall() {
        return snapshot().overall();
    }

    private void render(TeamMoraleSnapshot snap) {
        int overall = snap.overall();

        String label = overall >= 75 ? "High" : (overall >= 55 ? "Steady" : (overall >= 40 ? "Uneasy" : "Low"));
        statusTxt.setText(label);
        statusTxt.setForeground(moraleColor(overall));

        int streak = team != null && team.getWinStreak() != null ? team.getWinStreak().getStreakLength() : 0;
        checklist.removeAll();
        checklist.add(buildFactorRow(streak >= 2 ? "\u2713  " + streak + " Game Win Streak"
                : (streak <= -2 ? "\u2193  " + (-streak) + " Game Slump" : "\u2713  Season On Track"), streak >= -1));
        checklist.add(buildFactorRow(snap.leadership() >= 70 ? "\u2713  Strong Locker Room"
                : "\u2193  Leadership Questions", snap.leadership() >= 70));
        checklist.add(buildFactorRow(snap.buyIn() >= 70 ? "\u2713  Players Bought In"
                : "\u2193  Buy-In Slipping", snap.buyIn() >= 70));
        checklist.add(buildFactorRow(snap.chemistry() >= 65 ? "\u2713  Tight Chemistry"
                : "\u2193  Chemistry Building", snap.chemistry() >= 65));

        sliders.removeAll();
        sliders.add(buildSliderRow("Chemistry", snap.chemistry()));
        sliders.add(buildSliderRow("Leadership", snap.leadership()));
        sliders.add(buildSliderRow("Buy-In", snap.buyIn()));

        checklist.revalidate();
        checklist.repaint();
        sliders.revalidate();
        sliders.repaint();
        smiley.repaint();
    }

    private Color moraleColor(int overall) {
        if (overall >= 55) {
            return DesktopTheme.successGreen();
        }
        if (overall >= 40) {
            return DesktopTheme.gold();
        }
        return DesktopTheme.dangerRed();
    }

    private JPanel buildFactorRow(String text, boolean isPositive) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(isPositive ? DesktopTheme.textSecondary() : DesktopTheme.dangerRed());
        r.add(l, BorderLayout.WEST);
        return r;
    }

    private JPanel buildSliderRow(String label, int val) {
        JPanel r = new JPanel(new BorderLayout(8, 0));
        r.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        l.setForeground(DesktopTheme.textSecondary());
        l.setPreferredSize(new Dimension(60, 14));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(val);
        bar.setForeground(DesktopTheme.successGreen());
        bar.setBackground(DesktopTheme.windowBackground());
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(100, 6));

        JLabel v = new JLabel(String.valueOf(val), JLabel.RIGHT);
        v.setFont(new Font("Monospaced", Font.BOLD, 10));
        v.setForeground(Color.WHITE);
        v.setPreferredSize(new Dimension(20, 14));

        r.add(l, BorderLayout.WEST);
        r.add(bar, BorderLayout.CENTER);
        r.add(v, BorderLayout.EAST);
        return r;
    }
}
