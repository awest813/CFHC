package desktop;

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
 * Swing dashboard card component for TEAM OVERALL.
 * Renders big green overall rating digit, grade pill badge, star rating, Off/Def/ST ratings, and National/Conf ranks.
 */
public class TeamOverallCard extends CustomCardPanel {

    public TeamOverallCard(Team team) {
        super("Team Overall");
        JPanel content = getContentArea();

        int off = team != null ? (int) team.teamOffTalent : 84;
        int def = team != null ? (int) team.teamDefTalent : 81;
        int ovr = team != null ? (off + def) / 2 : 82;
        int st = team != null ? 76 : 76;
        int natRank = 24;
        int confRank = 3;

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);

        // Top Row: Big OVR Digit + Grade & Stars
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        topRow.setOpaque(false);

        JLabel ovrDigit = new JLabel(String.valueOf(ovr));
        ovrDigit.setFont(new Font("SansSerif", Font.BOLD, 54));
        ovrDigit.setForeground(DesktopTheme.successGreen());

        JPanel gradeBox = new JPanel(new GridLayout(2, 1, 0, 2));
        gradeBox.setOpaque(false);

        JLabel gradePill = new JLabel(" B+ ", JLabel.CENTER);
        gradePill.setOpaque(true);
        gradePill.setBackground(new Color(17, 28, 46));
        gradePill.setForeground(DesktopTheme.successGreen());
        gradePill.setFont(new Font("SansSerif", Font.BOLD, 12));
        gradePill.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));

        JLabel stars = new JLabel("\u2605\u2605\u2605\u2605\u2606");
        stars.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stars.setForeground(DesktopTheme.warningText());

        gradeBox.add(gradePill);
        gradeBox.add(stars);

        topRow.add(ovrDigit);
        topRow.add(gradeBox);
        body.add(topRow, BorderLayout.NORTH);

        // Center Column: Offense / Defense / Special Teams breakdown
        JPanel subCol = new JPanel(new GridLayout(3, 1, 0, 4));
        subCol.setOpaque(false);

        subCol.add(buildSubItem("\u2694", "OFFENSE", String.valueOf(off)));
        subCol.add(buildSubItem("\u26E8", "DEFENSE", String.valueOf(def)));
        subCol.add(buildSubItem("\u26BD", "SPECIAL TEAMS", String.valueOf(st)));

        body.add(subCol, BorderLayout.CENTER);

        // Footer: Ranks
        JPanel footerRanks = new JPanel(new GridLayout(1, 2, 8, 0));
        footerRanks.setOpaque(false);
        footerRanks.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(6, 0, 0, 0)));

        JPanel natBox = new JPanel(new BorderLayout());
        natBox.setOpaque(false);
        JLabel natLbl = new JLabel("NATIONAL RANK");
        natLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        natLbl.setForeground(DesktopTheme.textSecondary());
        JLabel natVal = new JLabel(String.valueOf(natRank), JLabel.RIGHT);
        natVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        natVal.setForeground(DesktopTheme.warningText());
        natBox.add(natLbl, BorderLayout.WEST);
        natBox.add(natVal, BorderLayout.EAST);

        JPanel confBox = new JPanel(new BorderLayout());
        confBox.setOpaque(false);
        JLabel confLbl = new JLabel("CONF. RANK");
        confLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        confLbl.setForeground(DesktopTheme.textSecondary());
        JLabel confVal = new JLabel(String.valueOf(confRank), JLabel.RIGHT);
        confVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        confVal.setForeground(DesktopTheme.successGreen());
        confBox.add(confLbl, BorderLayout.WEST);
        confBox.add(confVal, BorderLayout.EAST);

        footerRanks.add(natBox);
        footerRanks.add(confBox);

        body.add(footerRanks, BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
    }

    private JPanel buildSubItem(String icon, String label, String val) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(true);
        p.setBackground(new Color(6, 12, 20));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        JLabel lbl = new JLabel(icon + "  " + label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(DesktopTheme.textSecondary());

        JLabel v = new JLabel(val);
        v.setFont(new Font("Monospaced", Font.BOLD, 13));
        v.setForeground(DesktopTheme.successGreen());

        p.add(lbl, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }
}
