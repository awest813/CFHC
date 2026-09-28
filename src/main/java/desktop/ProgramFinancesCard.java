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

    private JLabel recruitValue;

    public ProgramFinancesCard(Team team) {
        this(team, DesktopRecruitingBudget.forTeam(team, null));
    }

    /**
     * @param recruitBudget from {@link DesktopRecruitingBudget#forTeam}; the
     *                      dashboard resolves it once so Program Health shows
     *                      the same number
     */
    public ProgramFinancesCard(Team team, int recruitBudget) {
        super("Program Finances");
        JPanel content = getContentArea();

        int budget = team != null ? team.getTeamBudget() : 0;
        int nilTier = team != null ? team.getNilCollectiveLevel() : 0;
        int facilities = team != null ? team.teamFacilities : 0;

        JPanel list = new JPanel(new GridLayout(5, 1, 0, 4));
        list.setOpaque(false);

        list.add(buildFinRow("Annual Budget", formatMoney(budget), DesktopTheme.textPrimary()));
        JPanel recruitRow = buildFinRow("Recruiting Budget", "", DesktopTheme.successGreen());
        recruitValue = (JLabel) recruitRow.getComponent(1);
        setRecruitingBudget(recruitBudget);
        list.add(recruitRow);
        list.add(buildFinRow("NIL Collective", "Tier " + nilTier, DesktopTheme.warningText()));
        list.add(buildFinRow("Facilities", "Level " + facilities, DesktopTheme.textPrimary()));
        list.add(buildFinRow("Discipline", team != null ? team.teamDisciplineScore + "%" : "\u2014", DesktopTheme.textPrimary()));

        content.add(list, BorderLayout.CENTER);
    }

    /** Updates the recruiting row in place (budget is spent on the board between rebuilds). */
    void setRecruitingBudget(int recruitBudget) {
        recruitValue.setText(recruitBudget < 0 ? "\u2014" : formatMoney(recruitBudget));
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
