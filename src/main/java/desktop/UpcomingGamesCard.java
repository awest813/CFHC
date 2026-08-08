package desktop;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Swing dashboard card component for UPCOMING GAMES.
 * Displays upcoming games preview list for Weeks 9 to 13 with difficulty stars.
 */
public class UpcomingGamesCard extends CustomCardPanel {

    public UpcomingGamesCard() {
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

        JPanel list = new JPanel(new GridLayout(5, 1, 0, 3));
        list.setOpaque(false);

        list.add(buildGameRow("Wk 9", "R", "at Redwood University", "Oct 25", "\u2605\u2605\u2605\u2605\u2606", DesktopTheme.dangerRed()));
        list.add(buildGameRow("Wk 10", "S", "vs Stonebridge", "Nov 1", "\u2605\u2605\u2605\u2606\u2606", DesktopTheme.warningText()));
        list.add(buildGameRow("Wk 11", "L", "vs Lakeside College", "Nov 8", "\u2605\u2605\u2606\u2606\u2606", new Color(59, 130, 246)));
        list.add(buildGameRow("Wk 12", "NR", "at North Ridge", "Nov 15", "\u2605\u2605\u2605\u2605\u2605", new Color(30, 58, 138)));
        list.add(buildGameRow("Wk 13", "SU", "vs Summit U", "Nov 22", "\u2605\u2605\u2606\u2606\u2606", new Color(107, 33, 168)));

        content.add(list, BorderLayout.CENTER);
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
