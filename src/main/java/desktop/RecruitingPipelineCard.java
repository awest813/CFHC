package desktop;

import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Swing dashboard card component for RECRUITING PIPELINE.
 * Renders USA map vector graphic, region pins, commits badge (bound to the real
 * freshman count), and VIEW RECRUITING BOARD action button.
 */
public class RecruitingPipelineCard extends CustomCardPanel {

    public RecruitingPipelineCard(Team team, Runnable onViewRecruiting) {
        super("Recruiting Pipeline");
        JPanel content = getContentArea();

        // Count this season's incoming class (year-1, non-redshirt freshmen).
        int commits = 0;
        if (team != null && team.getAllPlayers() != null) {
            for (positions.Player p : team.getAllPlayers()) {
                if (p.year == 1 && !p.wasRedshirt) commits++;
            }
        }
        float classRat = team != null ? team.getRecruitingClassRat() : 0f;

        // Header badge — real commit count + class rating (was hardcoded "14 Commits").
        JPanel headerRight = new JPanel();
        headerRight.setOpaque(false);
        JLabel commitsBadge = new JLabel("  " + commits + " Commits  \u2022  Class " + String.format("%.0f", classRat) + "  ");
        commitsBadge.setOpaque(true);
        commitsBadge.setBackground(new Color(0, 230, 118, 30));
        commitsBadge.setForeground(DesktopTheme.successGreen());
        commitsBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        commitsBadge.setBorder(BorderFactory.createLineBorder(DesktopTheme.successGreen(), 1));
        headerRight.add(commitsBadge);
        getHeaderBar().add(headerRight, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setOpaque(false);

        // US Map Diagram Panel
        JPanel mapStage = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Map Outline backdrop polygon fill (#111C2E)
                g2.setColor(new Color(17, 28, 46));
                int[] px = {20, 60, 110, 160, 200, 220, 210, 170, 130, 90, 40, 15};
                int[] py = {20, 15, 18, 10, 15, 60, 100, 110, 95, 105, 75, 45};
                g2.fillPolygon(px, py, px.length);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawPolygon(px, py, px.length);

                // Region Pin Markers
                drawPin(g2, 45, 55, "4", DesktopTheme.warningText());
                drawPin(g2, 110, 45, "2", DesktopTheme.successGreen());
                drawPin(g2, 130, 85, "3", DesktopTheme.successGreen());
                drawPin(g2, 180, 75, "3", DesktopTheme.successGreen());

                g2.dispose();
            }

            private void drawPin(Graphics2D g2, int x, int y, String count, Color color) {
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
                g2.fillOval(x - 10, y - 10, 20, 20);
                g2.setColor(color);
                g2.fillOval(x - 6, y - 6, 12, 12);
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2.drawString(count, x - 3, y + 3);
            }
        };
        mapStage.setPreferredSize(new Dimension(130, 110));
        mapStage.setOpaque(false);

        body.add(mapStage, BorderLayout.WEST);

        // Region Breakdown Legend
        JPanel legend = new JPanel(new GridLayout(5, 1, 0, 2));
        legend.setOpaque(false);

        legend.add(buildLegendRow("West", "\u2605\u2605\u2605\u2605\u2606", "4", DesktopTheme.warningText()));
        legend.add(buildLegendRow("Midwest", "\u2605\u2605\u2605\u2606\u2606", "2", DesktopTheme.successGreen()));
        legend.add(buildLegendRow("South", "\u2605\u2605\u2605\u2605\u2606", "3", DesktopTheme.successGreen()));
        legend.add(buildLegendRow("Southeast", "\u2605\u2605\u2605\u2605\u2606", "3", DesktopTheme.successGreen()));
        legend.add(buildLegendRow("Northeast", "\u2605\u2605\u2605\u2606\u2606", "2", DesktopTheme.warningText()));

        body.add(legend, BorderLayout.CENTER);

        // Action Button
        JButton btn = new JButton("VIEW RECRUITING BOARD");
        btn.setFont(new Font("SansSerif", Font.BOLD, 10));
        btn.setBackground(DesktopTheme.successGreen());
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.addActionListener(e -> {
            if (onViewRecruiting != null) onViewRecruiting.run();
        });

        body.add(btn, BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
    }

    private JPanel buildLegendRow(String name, String stars, String count, Color dotColor) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);

        JLabel n = new JLabel("\u2022  " + name);
        n.setFont(new Font("SansSerif", Font.BOLD, 9));
        n.setForeground(DesktopTheme.textSecondary());

        JLabel s = new JLabel(stars + "  " + count);
        s.setFont(new Font("SansSerif", Font.PLAIN, 9));
        s.setForeground(DesktopTheme.warningText());

        r.add(n, BorderLayout.WEST);
        r.add(s, BorderLayout.EAST);
        return r;
    }
}
