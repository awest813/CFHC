package desktop;

import simulation.Game;
import simulation.Team;

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
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Swing dashboard card component for NEXT GAME MATCHUP.
 * Displays home and away team matchup banners, AT badge, kickoff date/time, and stadium details.
 */
public class NextGameMatchupCard extends CustomCardPanel {

    public NextGameMatchupCard(Team team) {
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
        String weekInfo = upcoming != null
                ? "Week " + upcoming.week + (upcoming.gameName != null && !upcoming.gameName.isEmpty()
                    && !upcoming.gameName.equals("BYE WEEK") ? "  \u2022  " + upcoming.gameName : "")
                : "No upcoming game";

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);

        // Split Banner
        JPanel banner = new JPanel(new GridLayout(1, 3, 4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(6, 12, 20));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Home Team Side
        JPanel homeSide = new JPanel(new GridLayout(3, 1, 0, 1));
        homeSide.setOpaque(false);
        JLabel hName = new JLabel(homeName);
        hName.setFont(new Font("SansSerif", Font.BOLD, 10));
        hName.setForeground(Color.WHITE);

        JLabel hMascot = new JLabel(homeMascot);
        hMascot.setFont(new Font("SansSerif", Font.BOLD, 14));
        hMascot.setForeground(DesktopTheme.successGreen());

        JLabel hRec = new JLabel(homeRecord);
        hRec.setFont(new Font("SansSerif", Font.PLAIN, 9));
        hRec.setForeground(DesktopTheme.textSecondary());

        homeSide.add(hName);
        homeSide.add(hMascot);
        homeSide.add(hRec);

        // Center AT Pill Badge
        JPanel atPill = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.setColor(new Color(17, 28, 46));
                g2.fillOval(cx - 14, cy - 14, 28, 28);
                g2.setColor(DesktopTheme.warningText());
                g2.drawOval(cx - 14, cy - 14, 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        atPill.setOpaque(false);
        JLabel atText = new JLabel(atBadge, JLabel.CENTER);
        atText.setFont(new Font("SansSerif", Font.BOLD, 10));
        atText.setForeground(DesktopTheme.warningText());
        atPill.add(atText);

        // Away Team Side
        JPanel awaySide = new JPanel(new GridLayout(3, 1, 0, 1));
        awaySide.setOpaque(false);
        JLabel aName = new JLabel(awayName, JLabel.RIGHT);
        aName.setFont(new Font("SansSerif", Font.BOLD, 10));
        aName.setForeground(Color.WHITE);

        JLabel aMascot = new JLabel(awayMascot, JLabel.RIGHT);
        aMascot.setFont(new Font("SansSerif", Font.BOLD, 14));
        aMascot.setForeground(DesktopTheme.dangerRed());

        JLabel aRec = new JLabel(awayRecord, JLabel.RIGHT);
        aRec.setFont(new Font("SansSerif", Font.PLAIN, 9));
        aRec.setForeground(DesktopTheme.textSecondary());

        awaySide.add(aName);
        awaySide.add(aMascot);
        awaySide.add(aRec);

        banner.add(homeSide);
        banner.add(atPill);
        banner.add(awaySide);

        body.add(banner, BorderLayout.CENTER);

        // Details Row
        JPanel details = new JPanel(new GridLayout(2, 1, 0, 4));
        details.setOpaque(false);
        details.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JLabel dateTime = new JLabel("\uD83D\uDCC5  " + weekInfo);
        dateTime.setFont(new Font("SansSerif", Font.BOLD, 10));
        dateTime.setForeground(DesktopTheme.textSecondary());

        JLabel stadium = new JLabel(awayTeam != null
                ? "\uD83D\uDCCD  " + (userIsHome ? "HOME" : "AT " + awayTeam.getName())
                : "\uD83D\uDCCD  Schedule TBD");
        stadium.setFont(new Font("SansSerif", Font.PLAIN, 10));
        stadium.setForeground(DesktopTheme.textSecondary());

        details.add(dateTime);
        details.add(stadium);

        body.add(details, BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
    }
}
