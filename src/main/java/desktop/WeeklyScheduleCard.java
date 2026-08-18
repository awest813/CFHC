package desktop;

import simulation.Game;
import simulation.League;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Swing dashboard card component for WEEKLY SCHEDULE.
 * Shows the conceptual weekly routine with the real upcoming game highlighted
 * on Saturday (was a fixed "AT Redwood University" placeholder).
 */
public class WeeklyScheduleCard extends CustomCardPanel {

    public WeeklyScheduleCard(League league, Team team) {
        super("Weekly Schedule" + (league != null && league.currentWeek > 0 ? " \u2022 Week " + league.currentWeek : ""));
        JPanel content = getContentArea();

        // Resolve the real next game for the Saturday highlight.
        Game next = DesktopWeekResult.findUpcomingGame(team);
        String gameDesc = "No game scheduled";
        boolean hasGame = next != null && team != null;
        if (hasGame) {
            Team opp = next.homeTeam == team ? next.awayTeam : next.homeTeam;
            boolean isHome = next.homeTeam == team;
            gameDesc = (isHome ? "vs " : "AT ") + (opp != null ? opp.getName() : "TBD");
        }

        JPanel list = new JPanel(new GridLayout(7, 1, 0, 3));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        list.add(buildScheduleItem("MON", "\uD83D\uDC9A", "Recovery Day", "", false));
        list.add(buildScheduleItem("TUE", "\uD83C\uDFC8", "Practice", "3:30 PM", false));
        list.add(buildScheduleItem("WED", "\uD83C\uDFC8", "Practice", "3:30 PM", false));
        list.add(buildScheduleItem("THU", "\uD83D\uDCCB", "Walk-Through", "11:00 AM", false));
        list.add(buildScheduleItem("FRI", "\uD83D\uDE8C", "Travel Day", "10:00 AM", false));
        list.add(buildScheduleItem("SAT", "\uD83C\uDFC8", gameDesc, hasGame ? "Game Day" : "", hasGame));
        list.add(buildScheduleItem("SUN", "\u26C5", "Off Day", "", false));

        wrapper.add(list, BorderLayout.CENTER);
        content.add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildScheduleItem(String day, String icon, String desc, String time, boolean isGameDay) {
        JPanel item = new JPanel(new BorderLayout(6, 0));
        item.setOpaque(true);
        if (isGameDay) {
            item.setBackground(new Color(136, 19, 55, 60));
            item.setBorder(BorderFactory.createLineBorder(DesktopTheme.dangerRed(), 1));
        } else {
            item.setBackground(new Color(6, 12, 20));
            item.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));
        }

        JLabel d = new JLabel(day);
        d.setFont(new Font("SansSerif", Font.BOLD, 9));
        d.setForeground(isGameDay ? Color.WHITE : DesktopTheme.textSecondary());

        JLabel center = new JLabel(icon + "  " + desc);
        center.setFont(new Font("SansSerif", isGameDay ? Font.BOLD : Font.PLAIN, 9));
        center.setForeground(isGameDay ? Color.WHITE : DesktopTheme.textSecondary());

        JLabel t = new JLabel(time, JLabel.RIGHT);
        t.setFont(new Font("SansSerif", Font.PLAIN, 9));
        t.setForeground(DesktopTheme.textSecondary());

        item.add(d, BorderLayout.WEST);
        item.add(center, BorderLayout.CENTER);
        item.add(t, BorderLayout.EAST);
        return item;
    }
}
