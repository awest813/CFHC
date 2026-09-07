package desktop;

import simulation.League;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        this(league, null);
    }

    public TopNewsCarouselCard(League league, Runnable onOpenNews) {
        super("Top News");
        JPanel content = getContentArea();

        if (onOpenNews != null) {
            JPanel headerRight = new JPanel();
            headerRight.setOpaque(false);
            JLabel viewNews = new JLabel("ALL NEWS \u25B8");
            viewNews.setFont(new Font("SansSerif", Font.BOLD, 9));
            viewNews.setForeground(DesktopTheme.successGreen());
            viewNews.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            viewNews.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onOpenNews.run();
                }
            });
            headerRight.add(viewNews);
            if (getHeaderBar() != null) {
                getHeaderBar().add(headerRight, BorderLayout.EAST);
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to view all News stories");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onOpenNews.run();
                }
            });
        }

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

        // Hero Banner Graphic — styled sports wire broadcast banner
        JPanel hero = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(15, 28, 48), getWidth(), getHeight(), new Color(24, 46, 78)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.setColor(DesktopTheme.successGreen());
                g2.fillRoundRect(0, 0, 4, getHeight() - 1, 4, 4);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setPreferredSize(new Dimension(200, 42));
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));

        JPanel heroTop = new JPanel(new BorderLayout());
        heroTop.setOpaque(false);
        JLabel pill = new JLabel("\u25CF HEADLINE STORY");
        pill.setFont(new Font("SansSerif", Font.BOLD, 9));
        pill.setForeground(DesktopTheme.successGreen());
        JLabel wire = new JLabel("\uD83D\uDCF0 WIRE");
        wire.setFont(new Font("SansSerif", Font.BOLD, 9));
        wire.setForeground(new Color(148, 163, 184));
        heroTop.add(pill, BorderLayout.WEST);
        heroTop.add(wire, BorderLayout.EAST);

        JLabel sub = new JLabel("COLLEGE FOOTBALL BULLETIN");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 8));
        sub.setForeground(DesktopTheme.textSecondary());

        hero.add(heroTop, BorderLayout.NORTH);
        hero.add(sub, BorderLayout.SOUTH);
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
