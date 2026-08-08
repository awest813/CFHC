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
 * Swing dashboard card component for PROGRAM PRESTIGE.
 * Displays 3D metallic shield badge with score #78 RISING, bullet highlights, and progress fill bar.
 */
public class ProgramPrestigeCard extends CustomCardPanel {

    public ProgramPrestigeCard() {
        super("Program Prestige");
        JPanel content = getContentArea();

        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);

        // Metallic 3D Shield Badge Component
        JPanel shield = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                int[] px = {w / 2, w - 2, w - 2, w / 2, 2, 2};
                int[] py = {2, h / 4, (h * 3) / 4, h - 2, (h * 3) / 4, h / 4};

                g2.setColor(new Color(217, 119, 6));
                g2.fillPolygon(px, py, px.length);
                g2.setColor(DesktopTheme.warningText());
                g2.drawPolygon(px, py, px.length);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                g2.drawString("78", 12, 28);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
                g2.drawString("\u2605\u2605\u2605", 14, 38);

                g2.dispose();
            }
        };
        shield.setPreferredSize(new Dimension(46, 50));
        shield.setOpaque(false);

        body.add(shield, BorderLayout.WEST);

        // Right Info Block
        JPanel meta = new JPanel(new BorderLayout(0, 4));
        meta.setOpaque(false);

        JLabel status = new JLabel("\u25B2  RISING");
        status.setFont(new Font("SansSerif", Font.BOLD, 11));
        status.setForeground(DesktopTheme.successGreen());

        JPanel bullets = new JPanel(new GridLayout(3, 1, 0, 1));
        bullets.setOpaque(false);
        bullets.add(buildBullet("Winning %: .667"));
        bullets.add(buildBullet("Recent Success"));
        bullets.add(buildBullet("Facilities Upgrade"));

        meta.add(status, BorderLayout.NORTH);
        meta.add(bullets, BorderLayout.CENTER);

        body.add(meta, BorderLayout.CENTER);

        // Progress Bar Footer
        JProgressBar pBar = new JProgressBar(0, 100);
        pBar.setValue(78);
        pBar.setForeground(DesktopTheme.successGreen());
        pBar.setBackground(new Color(6, 12, 20));
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
