package desktop;

import simulation.League;
import simulation.Team;
import simulation.TeamColors;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Top sports broadcast header component for {@link LeagueHomeView}.
 * Renders team crest logo, school title, script mascot accent, season week tracker, coach card, and notification badge.
 */
public class DesktopHeaderBar extends JPanel {

    private final League league;

    public DesktopHeaderBar(League league) {
        super(new BorderLayout(16, 0));
        this.league = league;
        setOpaque(false);
        setPreferredSize(new Dimension(1200, 80));
        setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Left Brand Group
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        leftGroup.setOpaque(false);

        Team userTeam = league != null ? league.userTeam : null;

        // Precompute the crest monogram so the paint closure can capture it.
        final String mono = userTeam != null && userTeam.getAbbr() != null
                && userTeam.getAbbr().length() >= 2
                ? userTeam.getAbbr().substring(0, 2).toUpperCase() : "--";

        // Logo Shield Icon Component
        JPanel logoCrest = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Shield background
                g2.setColor(new Color(15, 41, 30));
                g2.fillOval(2, 2, w - 5, h - 5);
                g2.setColor(DesktopTheme.warningText());
                g2.drawOval(2, 2, w - 5, h - 5);

                // Inner circle fill
                g2.setColor(new Color(27, 77, 62));
                g2.fillOval(8, 8, w - 17, h - 17);

                // Monogram Text (team abbreviation; was hardcoded "PV").
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 18));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(mono);
                g2.drawString(mono, (w - textW) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);

                g2.dispose();
            }
        };
        logoCrest.setPreferredSize(new Dimension(50, 50));
        logoCrest.setOpaque(false);
        leftGroup.add(logoCrest);

        // Team Name & Season Week Titles
        JPanel titlePanel = new JPanel(new BorderLayout(0, 2));
        titlePanel.setOpaque(false);

        String schoolName = userTeam != null ? userTeam.getName().toUpperCase() : "PINE VALLEY STATE";
        String mascotName = userTeam != null && userTeam.nickname != null ? userTeam.nickname : "Owls";

        JLabel schoolLabel = new JLabel(schoolName + "  " + mascotName);
        schoolLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        schoolLabel.setForeground(Color.WHITE);

        int currentWeek = league != null ? league.currentWeek : 8;
        int currentYear = league != null ? league.getYear() : 2026;
        JLabel seasonLabel = new JLabel(currentYear + " SEASON  \u2022  WEEK " + currentWeek);
        seasonLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        seasonLabel.setForeground(DesktopTheme.textSecondary());

        titlePanel.add(schoolLabel, BorderLayout.NORTH);
        titlePanel.add(seasonLabel, BorderLayout.SOUTH);
        leftGroup.add(titlePanel);

        add(leftGroup, BorderLayout.WEST);

        // Right Coach Info Group
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 8));
        rightGroup.setOpaque(false);

        JPanel coachMeta = new JPanel(new BorderLayout(0, 2));
        coachMeta.setOpaque(false);

        // Bind real coach data; neutral fallback (was a fake "HC ELIJAH CARTER").
        HeadCoach hc = userTeam != null ? userTeam.getHeadCoach() : null;
        String coachName;
        String coachRecord;
        Color chipColor;
        String chipLabel;
        if (hc != null) {
            coachName = "HC " + hc.name.toUpperCase();
            coachRecord = "Career: " + hc.getWins() + "-" + hc.getLosses() + "  \u2022  Yr " + hc.year;
            // Job-security state from real contract/firing data.
            if (userTeam.fired) {
                chipColor = DesktopTheme.dangerRed();
                chipLabel = "HOT SEAT";
            } else if (hc.contractLength - hc.contractYear <= 2) {
                chipColor = DesktopTheme.warningText();
                chipLabel = "ON WATCH";
            } else {
                chipColor = DesktopTheme.successGreen();
                chipLabel = "SECURE";
            }
        } else {
            coachName = "HC \u2014";
            coachRecord = "No coach hired";
            chipColor = DesktopTheme.textSecondary();
            chipLabel = "VACANT";
        }

        JLabel hcLabel = new JLabel(coachName, JLabel.RIGHT);
        hcLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        hcLabel.setForeground(Color.WHITE);

        JLabel recordLabel = new JLabel(coachRecord, JLabel.RIGHT);
        recordLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        recordLabel.setForeground(DesktopTheme.textSecondary());

        coachMeta.add(hcLabel, BorderLayout.NORTH);
        coachMeta.add(recordLabel, BorderLayout.SOUTH);
        rightGroup.add(coachMeta);

        // Job-security pill — reuses the notification-pill paint idiom.
        final Color pillColor = chipColor;
        JPanel securityChip = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(17, 28, 46));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(pillColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        securityChip.setOpaque(false);
        JLabel chipText = new JLabel(chipLabel);
        chipText.setFont(new Font("SansSerif", Font.BOLD, 9));
        chipText.setForeground(pillColor);
        securityChip.add(chipText);
        rightGroup.add(securityChip);

        // Notification Mail Icon Pill
        JPanel notifPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(17, 28, 46));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        notifPill.setOpaque(false);
        notifPill.setPreferredSize(new Dimension(36, 32));

        // Notification count from real news headlines (was hardcoded "3").
        int newsCount = league != null && league.getNewsHeadlines() != null
                ? league.getNewsHeadlines().size() : 0;
        JLabel notifIcon = new JLabel("\u2709 " + newsCount);
        notifIcon.setFont(new Font("SansSerif", Font.BOLD, 11));
        notifIcon.setForeground(newsCount > 0
                ? DesktopTheme.warningText() : DesktopTheme.textSecondary());
        notifPill.add(notifIcon);

        rightGroup.add(notifPill);
        add(rightGroup, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dark Deep Navy Slate Header Gradient Background (#09121F)
        g2.setColor(new Color(9, 18, 31));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(DesktopTheme.borderSubtle());
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

        g2.dispose();
        super.paintComponent(g);
    }
}
