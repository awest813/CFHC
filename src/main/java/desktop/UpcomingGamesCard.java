package desktop;

import simulation.Game;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing dashboard card component for UPCOMING GAMES.
 * Binds to the real remaining schedule (was hardcoded "Wk 9 at Redwood..." etc.).
 * Shows up to 5 unplayed games with opponent, home/away, and difficulty stars
 * derived from the opponent's prestige vs the user team.
 */
public class UpcomingGamesCard extends CustomCardPanel {

    public UpcomingGamesCard(Team team) {
        super("Upcoming Games");
        JPanel content = getContentArea();

        // Header subtitle
        JPanel headerRight = new JPanel();
        headerRight.setOpaque(false);
        JLabel diffLbl = new JLabel("DIFFICULTY");
        diffLbl.setFont(new Font("SansSerif", Font.BOLD, 8));
        diffLbl.setForeground(DesktopTheme.textSecondary());
        headerRight.add(diffLbl);
        getHeaderBar().add(headerRight, BorderLayout.EAST);

        // Collect up to 5 unplayed, non-bye games from the real schedule.
        List<Game> upcoming = new ArrayList<>();
        if (team != null && team.getGameSchedule() != null) {
            for (Game g : team.getGameSchedule()) {
                if (!g.hasPlayed && !g.isByeWeek()) {
                    upcoming.add(g);
                    if (upcoming.size() >= 5) break;
                }
            }
        }

        int rows = Math.max(1, upcoming.size());
        JPanel list = new JPanel(new GridLayout(rows, 1, 0, 3));
        list.setOpaque(false);

        if (upcoming.isEmpty()) {
            JLabel empty = new JLabel("No upcoming games scheduled", JLabel.CENTER);
            empty.setFont(new Font("SansSerif", Font.PLAIN, 10));
            empty.setForeground(DesktopTheme.textSecondary());
            list.add(empty);
        } else {
            int userPrestige = team.getTeamPrestige();
            for (Game g : upcoming) {
                Team opp = g.homeTeam == team ? g.awayTeam : g.homeTeam;
                boolean isHome = g.homeTeam == team;
                String oppName = (isHome ? "vs " : "at ") + (opp != null ? opp.getName() : "TBD");
                String oppAbbr = opp != null && opp.getAbbr() != null && opp.getAbbr().length() >= 1
                        ? opp.getAbbr().substring(0, Math.min(2, opp.getAbbr().length())).toUpperCase() : "?";
                int oppPrestige = opp != null ? opp.getTeamPrestige() : 50;
                Color diffColor = difficultyColor(oppPrestige, userPrestige);
                String stars = difficultyStars(oppPrestige, userPrestige);
                list.add(buildGameRow("Wk " + g.week, oppAbbr, oppName,
                        g.gameName != null && !g.gameName.equals("BYE WEEK") && !g.gameName.isEmpty()
                                ? g.gameName : "", stars, diffColor));
            }
        }

        content.add(list, BorderLayout.CENTER);
    }

    /** Difficulty color: red if opponent clearly stronger, gold if even, blue/muted if weaker. */
    private static Color difficultyColor(int oppPrestige, int userPrestige) {
        int diff = oppPrestige - userPrestige;
        if (diff > 15) return DesktopTheme.dangerRed();
        if (diff > -5) return DesktopTheme.warningText();
        return new Color(59, 130, 246);
    }

    /** 1-5 difficulty stars based on prestige gap (5 = hardest). */
    private static String difficultyStars(int oppPrestige, int userPrestige) {
        int diff = oppPrestige - userPrestige;
        int filled = diff > 25 ? 5 : diff > 10 ? 4 : diff > -5 ? 3 : diff > -20 ? 2 : 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < filled ? '\u2605' : '\u2606');
        return sb.toString();
    }

    private JPanel buildGameRow(String week, String oppLogo, String oppName, String date, String stars, Color logoBg) {
        JPanel r = new JPanel(new BorderLayout(6, 0));
        r.setOpaque(true);
        r.setBackground(new Color(6, 12, 20));
        r.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        JPanel left = new JPanel(new BorderLayout(6, 0));
        left.setOpaque(false);

        JLabel w = new JLabel(week);
        w.setFont(new Font("SansSerif", Font.BOLD, 9));
        w.setForeground(DesktopTheme.textSecondary());

        JLabel badge = new JLabel(" " + oppLogo + " ", JLabel.CENTER);
        badge.setOpaque(true);
        badge.setBackground(logoBg);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("SansSerif", Font.BOLD, 8));

        JLabel name = new JLabel(oppName);
        name.setFont(new Font("SansSerif", Font.PLAIN, 9));
        name.setForeground(Color.WHITE);

        left.add(w, BorderLayout.WEST);
        left.add(badge, BorderLayout.CENTER);
        left.add(name, BorderLayout.EAST);

        JPanel right = new JPanel(new BorderLayout(4, 0));
        right.setOpaque(false);

        JLabel d = new JLabel(date);
        d.setFont(new Font("SansSerif", Font.PLAIN, 8));
        d.setForeground(DesktopTheme.textSecondary());

        JLabel s = new JLabel(stars);
        s.setFont(new Font("SansSerif", Font.PLAIN, 8));
        s.setForeground(DesktopTheme.warningText());

        right.add(d, BorderLayout.WEST);
        right.add(s, BorderLayout.EAST);

        r.add(left, BorderLayout.WEST);
        r.add(right, BorderLayout.EAST);
        return r;
    }
}
