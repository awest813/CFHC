package desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class SettingsPanel implements LeagueScreen {

    @Override
    public String title() {
        return "Settings";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("League Settings");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(DesktopTheme.textPrimary());
        JLabel subtitle = new JLabel("Review active universe rules and open the settings dialog for changes.");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(DesktopTheme.textSecondary());
        JPanel titleBlock = new JPanel(new GridLayout(0, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.CENTER);
        JButton editTop = new JButton("Edit Settings...");
        editTop.addActionListener(e -> SettingsDialog.show(ctx.parent(), ctx.league()));
        header.add(editTop, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridLayout(0, 2, 10, 8));
        summary.setOpaque(false);
        summary.setBorder(DesktopTheme.titledBorder("Active Options"));
        addOptionRow(summary, "Desktop theme", DesktopTheme.isDark() ? "Dark" : "Light");
        addOptionRow(summary, "Player potential", enabledLabel(ctx.league().showPotential));
        addOptionRow(summary, "Full game logs", enabledLabel(ctx.league().fullGameLog));
        addOptionRow(summary, "Game mode", ctx.league().careerMode ? "Career" : "Sandbox");
        addOptionRow(summary, "Never retire", enabledLabel(ctx.league().neverRetire));
        addOptionRow(summary, "TV contracts", enabledLabel(ctx.league().enableTV));
        addOptionRow(summary, "Expanded playoffs", enabledLabel(ctx.league().expPlayoffs));
        addOptionRow(summary, "Conference realignment", enabledLabel(ctx.league().confRealignment));
        addOptionRow(summary, "Advanced realignment", enabledLabel(ctx.league().advancedRealignment));
        addOptionRow(summary, "Promotion/relegation", enabledLabel(ctx.league().enableUnivProRel));
        panel.add(summary, BorderLayout.CENTER);

        JLabel note = new JLabel("<html><div style='width:620px'>Expanded playoffs lock once the regular season is underway. Promotion/relegation conversion is only available in Week 0. Save after applying changes to persist them.</div></html>");
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        note.setForeground(DesktopTheme.textSecondary());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(note);
        panel.add(actions, BorderLayout.SOUTH);

        DesktopTheme.styleLeagueSettingsPanel(panel);
        return panel;
    }

    private static void addOptionRow(JPanel panel, String label, String value) {
        JLabel left = new JLabel(label + ":");
        JLabel right = new JLabel(value);
        right.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        panel.add(left);
        panel.add(right);
    }

    private static String enabledLabel(boolean enabled) {
        return enabled ? "Enabled" : "Disabled";
    }
}