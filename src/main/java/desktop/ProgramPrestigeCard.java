package desktop;

import simulation.Team;

import javax.swing.BorderFactory;
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
 * Swing dashboard card component for PROGRAM PRESTIGE.
 * Binds to real team prestige + trend (was hardcoded shield "78" / "RISING").
 */
public class ProgramPrestigeCard extends CustomCardPanel {
    private static final int BADGE_W = 52;
    private static final int BADGE_H = 58;


    public ProgramPrestigeCard(Team team) {
        super("Program Prestige");
        JPanel content = getContentArea();

        final int prestige = team != null ? team.getTeamPrestige() : 0;
        int prestigeStart = team != null ? team.getTeamPrestigeStart() : 0;
        int delta = prestige - prestigeStart;
        // Trend label + color from the real season delta.
        String trend;
        Color trendColor;
        if (delta > 5) { trend = "\u25B2  RISING"; trendColor = DesktopTheme.successGreen(); }
        else if (delta < -2) { trend = "\u25BC  DECLINING"; trendColor = DesktopTheme.dangerRed(); }
        else { trend = "\u25CF  STABLE"; trendColor = DesktopTheme.textSecondary(); }
        // Winning percentage from real wins/losses.
        int w = team != null ? team.getWins() : 0;
        int l = team != null ? team.getLosses() : 0;
        int gp = w + l;
        String winPct = gp > 0 ? String.format("%.3f", (double) w / gp) : "\u2014";

        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);

        // Metallic 3D Shield Badge Component
        JPanel shield = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fixed-size badge centred vertically: BorderLayout.WEST stretches
                // this panel to the card height, which used to put the number on
                // the shield's narrow tip.
                int w = Math.min(getWidth(), BADGE_W);
                int h = Math.min(getHeight(), BADGE_H);
                int x0 = (getWidth() - w) / 2;
                int y0 = (getHeight() - h) / 2;

                int[] px = {x0 + w / 2, x0 + w - 2, x0 + w - 2, x0 + w / 2, x0 + 2, x0 + 2};
                int[] py = {y0 + 2, y0 + h / 4, y0 + (h * 3) / 4, y0 + h - 2, y0 + (h * 3) / 4, y0 + h / 4};

                g2.setColor(new Color(217, 119, 6));
                g2.fillPolygon(px, py, px.length);
                g2.setColor(DesktopTheme.warningText());
                g2.drawPolygon(px, py, px.length);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                String num = String.valueOf(prestige);
                int numBase = y0 + h / 2 + fm.getAscent() / 2 - 3;
                g2.drawString(num, x0 + (w - fm.stringWidth(num)) / 2, numBase);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 7));
                fm = g2.getFontMetrics();
                int starCount = prestige >= 90 ? 5 : prestige >= 75 ? 4 : prestige >= 60 ? 3 : prestige >= 45 ? 2 : prestige > 0 ? 1 : 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 5; i++) sb.append(i < starCount ? '\u2605' : '\u2606');
                String stars = sb.toString();
                g2.drawString(stars, x0 + (w - fm.stringWidth(stars)) / 2, numBase + fm.getAscent() + 2);

                g2.dispose();
            }
        };
        shield.setPreferredSize(new Dimension(BADGE_W, BADGE_H));
        shield.setOpaque(false);

        body.add(shield, BorderLayout.WEST);

        // Right Info Block
        JPanel meta = new JPanel(new BorderLayout(0, 4));
        meta.setOpaque(false);

        JLabel status = new JLabel(trend);
        status.setFont(new Font("SansSerif", Font.BOLD, 11));
        status.setForeground(trendColor);

        JPanel bullets = new JPanel(new GridLayout(3, 1, 0, 1));
        bullets.setOpaque(false);
        bullets.add(buildBullet("Winning %: " + winPct));
        bullets.add(buildBullet("Season Delta: " + (delta >= 0 ? "+" : "") + delta));
        bullets.add(buildBullet("National Rank: " + (team != null && team.rankTeamPollScore > 0 ? "#" + team.rankTeamPollScore : "\u2014")));

        meta.add(status, BorderLayout.NORTH);
        meta.add(bullets, BorderLayout.CENTER);

        body.add(meta, BorderLayout.CENTER);

        // Progress Bar Footer
        JProgressBar pBar = new JProgressBar(0, 100);
        pBar.setValue(Math.min(100, (prestige * 100) / Math.max(1, Team.PRESTIGE_SOFT_MAX)));
        pBar.setForeground(DesktopTheme.successGreen());
        pBar.setBackground(DesktopTheme.windowBackground());
        pBar.setBorderPainted(false);
        pBar.setPreferredSize(new Dimension(180, 4));

        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        wrapper.add(body, BorderLayout.CENTER);
        wrapper.add(pBar, BorderLayout.SOUTH);

        content.add(wrapper, BorderLayout.CENTER);
    }

    private JLabel buildBullet(String text) {
        JLabel l = new JLabel("\u2022  " + text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 9));
        l.setForeground(DesktopTheme.textSecondary());
        return l;
    }
}
