package desktop;

import simulation.Team;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Swing dashboard card: HEAD COACH profile. Binds to real coach data
 * (name, career record, overall + off/def/talent/discipline ratings,
 * contract year, age). Fills the 12th dashboard grid cell and replaces
 * static demo content with live state.
 */
public class HeadCoachCard extends CustomCardPanel {

    public HeadCoachCard(Team team) {
        super("Head Coach");
        JPanel content = getContentArea();

        HeadCoach hc = team != null ? team.getHeadCoach() : null;
        boolean hasCoach = hc != null;

        String coachName = hasCoach ? hc.name : "Vacant";
        String position = hasCoach && hc.position != null ? hc.position : "Head Coach";

        // Name + title row
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 2));
        top.setOpaque(false);
        JLabel nameLabel = new JLabel(coachName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setForeground(DesktopTheme.textPrimary());
        JLabel titleLabel = new JLabel(position);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        titleLabel.setForeground(DesktopTheme.textSecondary());
        top.add(nameLabel);
        top.add(titleLabel);

        // Big overall rating
        JPanel ovrPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        ovrPanel.setOpaque(false);
        JLabel ovr = new JLabel(hasCoach ? String.valueOf(hc.ratOvr) : "\u2014");
        ovr.setFont(new Font("SansSerif", Font.BOLD, 36));
        ovr.setForeground(DesktopTheme.successGreen());
        ovrPanel.add(ovr);

        JLabel ovrCaption = new JLabel("OVERALL");
        ovrCaption.setFont(new Font("SansSerif", Font.PLAIN, 8));
        ovrCaption.setForeground(DesktopTheme.textSecondary());

        JPanel ovrWrapper = new JPanel(new GridLayout(0, 1, 0, 0));
        ovrWrapper.setOpaque(false);
        ovrWrapper.add(ovrPanel);
        ovrWrapper.add(ovrCaption);

        // Rating sub-stats grid (off/def/talent/discipline) — real values.
        JPanel stats = new JPanel(new GridLayout(2, 2, 6, 4));
        stats.setOpaque(false);
        if (hasCoach) {
            stats.add(buildStat("OFF", hc.ratOff));
            stats.add(buildStat("DEF", hc.ratDef));
            stats.add(buildStat("TALENT", hc.ratTalent));
            stats.add(buildStat("DISC", hc.ratDiscipline));
        } else {
            stats.add(buildStat("OFF", 0));
            stats.add(buildStat("DEF", 0));
            stats.add(buildStat("TALENT", 0));
            stats.add(buildStat("DISC", 0));
        }

        // Career record (career wins/losses from the staff record) + contract line.
        String record = hasCoach ? hc.getWins() + "-" + hc.getLosses() : "\u2014";
        String contract = hasCoach
                ? "Yr " + (hc.contractYear + 1) + " / " + hc.contractLength + "  \u2022  Age " + hc.age
                : "\u2014";

        JLabel recordLabel = new JLabel("Record: " + record);
        recordLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        recordLabel.setForeground(DesktopTheme.textPrimary());
        JLabel contractLabel = new JLabel(contract);
        contractLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        contractLabel.setForeground(DesktopTheme.textSecondary());

        JPanel footer = new JPanel(new GridLayout(0, 1, 0, 1));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        footer.add(recordLabel);
        footer.add(contractLabel);

        // Layout: name top, big OVR + sub-stats middle, record/contract bottom.
        JPanel body = new JPanel(new BorderLayout(8, 4));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(8, 0));
        middle.setOpaque(false);
        middle.add(ovrWrapper, BorderLayout.WEST);
        middle.add(stats, BorderLayout.CENTER);
        body.add(middle, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);

        content.add(body, BorderLayout.CENTER);
    }

    private JLabel buildStat(String label, int value) {
        JLabel l = new JLabel(label + " " + value);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(DesktopTheme.textPrimary());
        // Color-code: green for strong, gold for mid, muted for weak.
        if (value >= 85) {
            l.setForeground(DesktopTheme.successGreen());
        } else if (value >= 70) {
            l.setForeground(DesktopTheme.warningText());
        } else {
            l.setForeground(DesktopTheme.textSecondary());
        }
        return l;
    }
}
