package desktop;

import simulation.Game;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Swing dashboard card component for NEXT GAME MATCHUP.
 * Displays home and away team matchup banners, AT badge, kickoff date/time, and stadium details.
 */
public class NextGameMatchupCard extends CustomCardPanel {

    public NextGameMatchupCard(Team team) {
        this(team, null);
    }

    public NextGameMatchupCard(Team team, Consumer<Team> onSelectTeam) {
        super("Next Game");
        JPanel content = getContentArea();

        // Resolve the real upcoming game + opponent (was entirely hardcoded
        // "REDWOOD UNIVERSITY / 4-3 / SAT OCT 25 / REDWOOD STADIUM").
        Game upcoming = DesktopWeekResult.findUpcomingGame(team);
        Team opp = null;
        boolean userIsHome = false;
        if (upcoming != null && team != null) {
            userIsHome = DesktopWeekResult.userIsHome(upcoming, team);
            opp = upcoming.homeTeam == team ? upcoming.awayTeam : upcoming.homeTeam;
        }

        // Home side is the user team (the "AT/vs" badge communicates direction).
        Team homeTeam = team;
        Team awayTeam = opp;

        String homeName = homeTeam != null ? homeTeam.getName().toUpperCase() : "\u2014";
        String homeMascot = homeTeam != null && homeTeam.nickname != null ? homeTeam.nickname.toUpperCase() : "";
        String homeRecord = homeTeam != null ? homeTeam.getWins() + "-" + homeTeam.getLosses() : "\u2014";

        String awayName = awayTeam != null ? awayTeam.getName().toUpperCase() : "TBD";
        String awayMascot = awayTeam != null && awayTeam.nickname != null ? awayTeam.nickname.toUpperCase() : "";
        String awayRecord = awayTeam != null ? awayTeam.getWins() + "-" + awayTeam.getLosses() : "\u2014";
        String atBadge = awayTeam == null ? "\u2014" : (userIsHome ? "VS" : "AT");

        // Week + game name from the real schedule.
        int weekNum = 0;
        if (upcoming != null) {
            if (upcoming.week > 0) {
                weekNum = upcoming.week;
            } else if (team != null && team.getGameSchedule() != null) {
                int idx = team.getGameSchedule().indexOf(upcoming);
                if (idx >= 0) weekNum = idx + 1;
            }
        }
        String weekInfo = upcoming != null
                ? "Week " + (weekNum > 0 ? weekNum : (team != null && team.league != null ? team.league.currentWeek + 1 : 1))
                    + (upcoming.gameName != null && !upcoming.gameName.isEmpty()
                    && !upcoming.gameName.equals("BYE WEEK") ? "  \u2022  " + upcoming.gameName : "")
                : "No upcoming game";

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);

        // Split Banner
        JPanel banner = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DesktopTheme.tableStripe());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Home Team Side
        JPanel homeSide = new JPanel(new GridLayout(3, 1, 0, 1));
        homeSide.setOpaque(false);
        JLabel hName = new JLabel(homeName);
        hName.setFont(new Font("SansSerif", Font.BOLD, homeName.length() > 11 ? 9 : 10));
        hName.setForeground(Color.WHITE);

        JLabel hMascot = new JLabel(homeMascot);
        hMascot.setFont(new Font("SansSerif", Font.BOLD, homeMascot.length() > 10 ? 12 : 13));
        hMascot.setForeground(DesktopTheme.successGreen());

        JLabel hRec = new JLabel(homeRecord);
        hRec.setFont(new Font("SansSerif", Font.PLAIN, 9));
        hRec.setForeground(DesktopTheme.textSecondary());

        homeSide.add(hName);
        homeSide.add(hMascot);
        homeSide.add(hRec);
        if (homeTeam != null && onSelectTeam != null) {
            homeSide.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            homeSide.setToolTipText("Click to view " + homeTeam.getName() + " details");
            homeSide.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelectTeam.accept(homeTeam);
                }
            });
        }

        // Center AT Pill Badge with directly centered text
        final String badgeText = atBadge;
        JPanel atPill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(DesktopTheme.tableBase());
                g2.fillOval(cx - 13, cy - 13, 26, 26);
                g2.setColor(DesktopTheme.warningText());
                g2.drawOval(cx - 13, cy - 13, 26, 26);

                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(badgeText);
                int th = fm.getAscent() - fm.getDescent();
                g2.drawString(badgeText, cx - tw / 2, cy + th / 2);
                g2.dispose();
            }
        };
        atPill.setOpaque(false);
        atPill.setPreferredSize(new Dimension(32, 50));

        // Away Team Side
        JPanel awaySide = new JPanel(new GridLayout(3, 1, 0, 1));
        awaySide.setOpaque(false);
        JLabel aName = new JLabel(awayName, JLabel.RIGHT);
        aName.setFont(new Font("SansSerif", Font.BOLD, awayName.length() > 11 ? 9 : 10));
        aName.setForeground(Color.WHITE);

        JLabel aMascot = new JLabel(awayMascot, JLabel.RIGHT);
        aMascot.setFont(new Font("SansSerif", Font.BOLD, awayMascot.length() > 10 ? 12 : 13));
        aMascot.setForeground(DesktopTheme.dangerRed());

        JLabel aRec = new JLabel(awayRecord, JLabel.RIGHT);
        aRec.setFont(new Font("SansSerif", Font.PLAIN, 9));
        aRec.setForeground(DesktopTheme.textSecondary());

        awaySide.add(aName);
        awaySide.add(aMascot);
        awaySide.add(aRec);
        if (awayTeam != null && onSelectTeam != null) {
            awaySide.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            awaySide.setToolTipText("Click to view " + awayTeam.getName() + " details");
            awaySide.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelectTeam.accept(awayTeam);
                }
            });
        }

        banner.add(homeSide, BorderLayout.WEST);
        banner.add(atPill, BorderLayout.CENTER);
        banner.add(awaySide, BorderLayout.EAST);

        body.add(banner, BorderLayout.CENTER);

        // Details Row — 6px row gap + 10/11pt fonts for legibility (was a
        // tight 4px gap that read as crowded at 10pt).
        JPanel details = new JPanel(new GridLayout(2, 1, 0, 6));
        details.setOpaque(false);
        details.setBorder(BorderFactory.createEmptyBorder(6, 4, 0, 4));

        JLabel dateTime = new JLabel("\uD83D\uDCC5  " + weekInfo);
        dateTime.setFont(new Font("SansSerif", Font.BOLD, 11));
        dateTime.setForeground(DesktopTheme.textSecondary());

        JLabel stadium = new JLabel(awayTeam != null
                ? "\uD83D\uDCCD  " + (userIsHome ? "HOME" : "AT " + awayTeam.getName())
                : "\uD83D\uDCCD  Schedule TBD");
        stadium.setFont(new Font("SansSerif", Font.PLAIN, 11));
        stadium.setForeground(DesktopTheme.textSecondary());

        details.add(dateTime);
        details.add(stadium);

        body.add(details, BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
    }
}
