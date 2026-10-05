package desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

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
        editTop.addActionListener(e -> {
            simulation.AudioManager sounds =
                    ctx.parent() instanceof LeagueHomeView home ? home.uiSounds() : null;
            if (sounds != null) {
                sounds.play(simulation.AudioEvent.UI_CLICK);
            }
            boolean applied = SettingsDialog.show(ctx.parent(), ctx.league(), sounds);
            // Mark the league dirty + rebuild this summary when changes were
            // applied from the screen's own button (previously the dialog's
            // return was ignored, so edits here skipped the unsaved-changes
            // prompt and left this panel showing stale values).
            if (applied && sounds != null) {
                sounds.play(simulation.AudioEvent.CONFIRM);
            }
            if (applied && ctx.parent() instanceof LeagueHomeView) {
                ((LeagueHomeView) ctx.parent()).onSettingsAppliedExternally();
            }
        });
        header.add(editTop, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Compact label/value list on a slate card, anchored top-left. The old
        // GridLayout(0, 2) filled the whole window, leaving ~70px between rows
        // and pushing values half a screen away from their labels.
        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 0));
        rows.setOpaque(false);
        addOptionRow(rows, "Desktop theme", DesktopTheme.isDark()
                ? (DesktopTheme.isHighContrast() ? "Dark (high contrast)" : "Dark")
                : (DesktopTheme.isHighContrast() ? "Light (high contrast)" : "Light"));
        addOptionRow(rows, "Player potential", enabledLabel(ctx.league().showPotential));
        addOptionRow(rows, "Full game logs", enabledLabel(ctx.league().fullGameLog));
        addOptionRow(rows, "Game mode", ctx.league().careerMode ? "Career" : "Sandbox");
        addOptionRow(rows, "Never retire", enabledLabel(ctx.league().neverRetire));
        addOptionRow(rows, "TV contracts", enabledLabel(ctx.league().enableTV));
        addOptionRow(rows, "Expanded playoffs", enabledLabel(ctx.league().expPlayoffs));
        addOptionRow(rows, "Conference realignment", enabledLabel(ctx.league().confRealignment));
        addOptionRow(rows, "Advanced realignment", enabledLabel(ctx.league().advancedRealignment));
        addOptionRow(rows, "FCS promotions", ctx.league().fcsPromotionSummary());
        addOptionRow(rows, "Promotion/relegation", enabledLabel(ctx.league().enableUnivProRel));

        JPanel summary = new JPanel(new BorderLayout(0, 8));
        summary.setOpaque(true);
        summary.setBackground(DesktopTheme.tableBase());
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(12, 14, 8, 14)));
        JLabel cardTitle = new JLabel("ACTIVE OPTIONS");
        cardTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        summary.add(cardTitle, BorderLayout.NORTH);
        summary.add(rows, BorderLayout.CENTER);

        JLabel note = new JLabel("<html><div style='width:460px'>Expanded playoffs lock once the regular season is underway. Promotion/relegation conversion is only available in Week 0. Save after applying changes to persist them.</div></html>");
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        note.setBorder(BorderFactory.createEmptyBorder(10, 2, 0, 0));

        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(summary, BorderLayout.NORTH);
        column.add(note, BorderLayout.CENTER);

        // WEST + NORTH anchoring keeps the card at its preferred size.
        JPanel anchor = new JPanel(new BorderLayout());
        anchor.setOpaque(false);
        anchor.add(column, BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(anchor, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        DesktopTheme.styleLeagueSettingsPanel(panel);
        // styleLeagueSettingsPanel paints every label textPrimary; restore the
        // muted/positive accents afterwards.
        subtitle.setForeground(DesktopTheme.textSecondary());
        cardTitle.setForeground(DesktopTheme.textSecondary());
        note.setForeground(DesktopTheme.textSecondary());
        for (java.awt.Component row : rows.getComponents()) {
            if (row instanceof JPanel rp && rp.getComponentCount() == 2
                    && rp.getComponent(1) instanceof JLabel value) {
                value.setForeground(valueColor(value.getText()));
            }
        }
        return panel;
    }

    private static void addOptionRow(JPanel rows, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(48, 0));
        row.setOpaque(false);
        boolean first = rows.getComponentCount() == 0;
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(first ? 0 : 1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        JLabel left = new JLabel(label);
        left.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JLabel right = new JLabel(value, JLabel.RIGHT);
        right.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        rows.add(row);
    }

    private static java.awt.Color valueColor(String value) {
        if ("Enabled".equals(value)) {
            return DesktopTheme.isDark() ? DesktopTheme.successGreen() : DesktopTheme.successGreen().darker();
        }
        if ("Disabled".equals(value)) {
            return DesktopTheme.textSecondary();
        }
        return DesktopTheme.textPrimary();
    }
    private static String enabledLabel(boolean enabled) {
        return enabled ? "Enabled" : "Disabled";
    }
}