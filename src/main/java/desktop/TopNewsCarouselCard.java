package desktop;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Swing dashboard card component for TOP NEWS CAROUSEL.
 * Displays hero banner background, bold headline, news snippet, and dot pagination indicators.
 */
public class TopNewsCarouselCard extends CustomCardPanel {

    public TopNewsCarouselCard() {
        super("Top News");
        JPanel content = getContentArea();

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);

        // Hero Banner Graphic
        JPanel hero = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(27, 77, 62));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2.setColor(new Color(0, 230, 118, 40));
                g2.fillOval(getWidth() / 4, -20, getWidth() / 2, getHeight() + 40);

                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                g2.dispose();
            }
        };
        hero.setPreferredSize(new Dimension(200, 70));
        hero.setOpaque(false);
        body.add(hero, BorderLayout.NORTH);

        // Headline & Snippet Box
        JPanel newsText = new JPanel(new BorderLayout(0, 4));
        newsText.setOpaque(false);

        JLabel title = new JLabel("<html><b>OWLS CLIMB TO #24 IN LATEST POLL</b></html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(Color.WHITE);

        JLabel snippet = new JLabel("<html><body style='width: 180px;'>Back-to-back road wins have the Owls ranked #24 nationally. Coach Carter credits \"buy-in and belief.\"</body></html>");
        snippet.setFont(new Font("SansSerif", Font.PLAIN, 10));
        snippet.setForeground(DesktopTheme.textSecondary());

        newsText.add(title, BorderLayout.NORTH);
        newsText.add(snippet, BorderLayout.CENTER);

        // Dots Pagination
        JLabel dots = new JLabel("\u25CF  \u25CB  \u25CB  \u25CB  \u25CB  \u25CB", JLabel.CENTER);
        dots.setFont(new Font("SansSerif", Font.BOLD, 8));
        dots.setForeground(DesktopTheme.successGreen());
        newsText.add(dots, BorderLayout.SOUTH);

        body.add(newsText, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
    }
}
