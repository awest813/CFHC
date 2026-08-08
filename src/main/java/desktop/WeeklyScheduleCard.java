package desktop;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Swing dashboard card component for WEEKLY SCHEDULE.
 * Displays day-by-day vertical timeline for Week 8 activities (MON OCT 20 to SUN OCT 26).
 */
public class WeeklyScheduleCard extends CustomCardPanel {

    public WeeklyScheduleCard() {
        super("Weekly Schedule");
        JPanel content = getContentArea();

        JPanel list = new JPanel(new GridLayout(7, 1, 0, 3));
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        list.add(buildScheduleItem("MON", "OCT 20", "\uD83D\uDC9A", "Recovery Day", "", false));
        list.add(buildScheduleItem("TUE", "OCT 21", "\uD83C\uDFC8", "Practice", "3:30 PM", false));
        list.add(buildScheduleItem("WED", "OCT 22", "\uD83C\uDFC8", "Practice", "3:30 PM", false));
        list.add(buildScheduleItem("THU", "OCT 23", "\uD83D\uDCCB", "Walk-Through", "11:00 AM", false));
        list.add(buildScheduleItem("FRI", "OCT 24", "\uD83D\uDE8C", "Travel Day", "10:00 AM", false));
        list.add(buildScheduleItem("SAT", "OCT 25", "R", "AT Redwood University", "3:30 PM", true));
        list.add(buildScheduleItem("SUN", "OCT 26", "\u26C5", "Off Day", "", false));

        wrapper.add(list, BorderLayout.CENTER);
        content.add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildScheduleItem(String day, String date, String icon, String desc, String time, boolean isGameDay) {
        JPanel item = new JPanel(new BorderLayout(6, 0));
        item.setOpaque(true);
        if (isGameDay) {
            item.setBackground(new Color(136, 19, 55, 60)); // Crimson maroon highlight
            item.setBorder(BorderFactory.createLineBorder(DesktopTheme.dangerRed(), 1));
        } else {
            item.setBackground(new Color(6, 12, 20));
            item.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));
        }

        JPanel left = new JPanel(new BorderLayout(4, 0));
        left.setOpaque(false);

        JLabel d = new JLabel(day);
        d.setFont(new Font("SansSerif", Font.BOLD, 9));
        d.setForeground(isGameDay ? Color.WHITE : DesktopTheme.textSecondary());

        JLabel dt = new JLabel(date);
        dt.setFont(new Font("SansSerif", Font.PLAIN, 8));
        dt.setForeground(DesktopTheme.textSecondary());

        left.add(d, BorderLayout.WEST);
        left.add(dt, BorderLayout.EAST);

        JLabel center = new JLabel(icon + "  " + desc);
        center.setFont(new Font("SansSerif", isGameDay ? Font.BOLD : Font.PLAIN, 9));
        center.setForeground(isGameDay ? Color.WHITE : DesktopTheme.textSecondary());

        JLabel t = new JLabel(time, JLabel.RIGHT);
        t.setFont(new Font("SansSerif", Font.PLAIN, 8));
        t.setForeground(DesktopTheme.textSecondary());

        item.add(left, BorderLayout.WEST);
        item.add(center, BorderLayout.CENTER);
        item.add(t, BorderLayout.EAST);
        return item;
    }
}
