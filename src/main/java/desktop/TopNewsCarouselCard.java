package desktop;

import simulation.League;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing dashboard card component for TOP NEWS CAROUSEL.
 * Binds to the real league news headlines (was hardcoded "OWLS CLIMB TO #24...").
 * Shows the latest headlines from {@link League#getNewsHeadlines()} with the
 * engine's "headline>story" format split into title + snippet.
 */
public class TopNewsCarouselCard extends CustomCardPanel {

    public TopNewsCarouselCard(League league) {
        super("Top News");
        JPanel content = getContentArea();

        // Pull real headlines. Engine entries may use "headline>story body";
        // some are headline-only. Take up to 5 most-recent.
        List<String> headlines = new ArrayList<>();
        if (league != null && league.getNewsHeadlines() != null) {
            for (String h : league.getNewsHeadlines()) {
                if (h != null && !h.trim().isEmpty()) {
                    headlines.add(h);
                    if (headlines.size() >= 5) break;
                }
            }
        }

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

        // Headline & Snippet Box — bind the latest headline.
        JPanel newsText = new JPanel(new BorderLayout(0, 4));
        newsText.setOpaque(false);

        String titleText;
        String snippetText;
        if (headlines.isEmpty()) {
            titleText = "No news this week";
            snippetText = "Play a week to generate league news.";
        } else {
            String latest = headlines.get(0);
            int gt = latest.indexOf('>');
            if (gt > 0 && gt < latest.length() - 1) {
                titleText = latest.substring(0, gt).trim();
                snippetText = latest.substring(gt + 1).trim();
            } else {
                titleText = latest.trim();
                snippetText = headlines.size() > 1 ? "+ " + (headlines.size() - 1) + " more stories" : "";
            }
        }

        JLabel title = new JLabel("<html><b>" + escapeHtml(titleText) + "</b></html>");
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(Color.WHITE);

        JLabel snippet = new JLabel("<html><body style='width: 180px;'>" + escapeHtml(snippetText) + "</body></html>");
        snippet.setFont(new Font("SansSerif", Font.PLAIN, 10));
        snippet.setForeground(DesktopTheme.textSecondary());

        newsText.add(title, BorderLayout.NORTH);
        newsText.add(snippet, BorderLayout.CENTER);

        // Dots Pagination — one dot per available headline (capped at 5).
        StringBuilder dotStr = new StringBuilder();
        int dotCount = Math.max(1, headlines.size());
        for (int i = 0; i < dotCount; i++) {
            if (i > 0) dotStr.append("  ");
            dotStr.append(i == 0 ? '\u25CF' : '\u25CB');
        }
        JLabel dots = new JLabel(dotStr.toString(), JLabel.CENTER);
        dots.setFont(new Font("SansSerif", Font.BOLD, 8));
        dots.setForeground(DesktopTheme.successGreen());
        newsText.add(dots, BorderLayout.SOUTH);

        body.add(newsText, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
