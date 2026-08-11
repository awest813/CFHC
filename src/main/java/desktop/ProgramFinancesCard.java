package desktop;

import simulation.Team;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Swing dashboard card component for PROGRAM FINANCES.
 * Binds to real team budget + recruiting budget + NIL tier + facilities level
 * (was hardcoded "$34.2M / $5.8M / $642K").
 */
public class ProgramFinancesCard extends CustomCardPanel {

    public ProgramFinancesCard(Team team) {
        super("Program Finances");
        JPanel content = getContentArea();

        int budget = team != null ? team.getTeamBudget() : 0;
        int recruitBudget = team != null ? team.getTeamRecruitBudget() : 0;
        int nilTier = team != null ? team.getNilCollectiveLevel() : 0;
        int facilities = team != null ? team.teamFacilities : 0;

        JPanel list = new JPanel(new GridLayout(5, 1, 0, 4));
        list.setOpaque(false);

        list.add(buildFinRow("Annual Budget", formatMoney(budget), Color.WHITE));
        list.add(buildFinRow("Recruiting Budget", formatMoney(recruitBudget), DesktopTheme.successGreen()));
        list.add(buildFinRow("NIL Collective", "Tier " + nilTier, DesktopTheme.warningText()));
        list.add(buildFinRow("Facilities", "Level " + facilities, Color.WHITE));
        list.add(buildFinRow("Discipline", team != null ? team.teamDisciplineScore + "%" : "\u2014", Color.WHITE));

        content.add(list, BorderLayout.CENTER);
    }

    /** Format an integer budget as $X.XM or $XK depending on magnitude. */
    private static String formatMoney(int amount) {
        if (amount >= 1_000_000) return "$" + String.format("%.1fM", amount / 1_000_000.0);
        if (amount >= 1_000) return "$" + (amount / 1_000) + "K";
        return "$" + amount;
    }

    private JPanel buildFinRow(String label, String value, Color valColor) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        l.setForeground(DesktopTheme.textSecondary());

        JLabel v = new JLabel(value);
        v.setFont(new Font("Monospaced", Font.BOLD, 12));
        v.setForeground(valColor);

        r.add(l, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }
}
