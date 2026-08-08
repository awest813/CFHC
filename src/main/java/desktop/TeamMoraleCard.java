package desktop;

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
 * Swing dashboard card component for TEAM MORALE.
 * Displays green smiley gauge, key factors checklist, and progress bars for Chemistry, Leadership, and Buy-In.
 */
public class TeamMoraleCard extends CustomCardPanel {

    public TeamMoraleCard() {
        super("Team Morale");
        JPanel content = getContentArea();

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);

        // Top Row: Smiley Gauge + Checklist
        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        // Smiley Gauge
        JPanel gaugeCol = new JPanel(new BorderLayout(0, 4));
        gaugeCol.setOpaque(false);

        JPanel smiley = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0, 230, 118, 30));
                g2.fillOval(2, 2, getWidth() - 5, getHeight() - 5);
                g2.setColor(DesktopTheme.successGreen());
                g2.drawOval(2, 2, getWidth() - 5, getHeight() - 5);

                // Eyes & Smile
                g2.fillOval(12, 14, 4, 4);
                g2.fillOval(24, 14, 4, 4);
                g2.drawArc(12, 18, 16, 12, 180, 180);

                g2.dispose();
            }
        };
        smiley.setPreferredSize(new Dimension(40, 40));
        smiley.setOpaque(false);

        JLabel statusTxt = new JLabel("High", JLabel.CENTER);
        statusTxt.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusTxt.setForeground(DesktopTheme.successGreen());

        gaugeCol.add(smiley, BorderLayout.CENTER);
        gaugeCol.add(statusTxt, BorderLayout.SOUTH);
        topRow.add(gaugeCol, BorderLayout.WEST);

        // Factors Checklist
        JPanel checklist = new JPanel(new GridLayout(4, 1, 0, 2));
        checklist.setOpaque(false);

        checklist.add(buildFactorRow("\u2713  2 Game Win Streak", true));
        checklist.add(buildFactorRow("\u2713  Close Locker Room", true));
        checklist.add(buildFactorRow("\u2713  Players Confident", true));
        checklist.add(buildFactorRow("\u2193  Road Game", false));

        topRow.add(checklist, BorderLayout.CENTER);
        body.add(topRow, BorderLayout.NORTH);

        // Sliders Grid
        JPanel sliders = new JPanel(new GridLayout(3, 1, 0, 4));
        sliders.setOpaque(false);

        sliders.add(buildSliderRow("Chemistry", 82));
        sliders.add(buildSliderRow("Leadership", 78));
        sliders.add(buildSliderRow("Buy-In", 85));

        body.add(sliders, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
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
        bar.setBackground(new Color(6, 12, 20));
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
