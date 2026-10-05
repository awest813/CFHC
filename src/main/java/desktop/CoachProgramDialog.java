package desktop;

import staff.Staff;
import simulation.CoachSkills;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * User head coach skill tree, NIL collective tier, and training facility summary.
 */
public final class CoachProgramDialog {

    private CoachProgramDialog() {
    }

    public static void show(JFrame owner, Team userTeam) {
        show(owner, userTeam, null);
    }

    public static void show(JFrame owner, Team userTeam, Runnable onChanged) {
        Runnable changed = onChanged != null ? onChanged : () -> {};
        if (userTeam == null || userTeam.getHeadCoach() == null) {
            JOptionPane.showMessageDialog(owner,
                    DesktopTheme.messageForDialog(
                            "Coach Program requires a user team with a head coach."),
                    "Coach Program",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        final Staff hc = userTeam.getHeadCoach();

        JDialog d = new JDialog(owner, CoachSkills.PROGRAM_DIALOG_TITLE, true);
        d.setSize(560, 700);
        d.setLocationRelativeTo(owner);
        d.setLayout(new BorderLayout(0, 8));
        DesktopTheme.styleDialogContentPane(d.getContentPane());
        DesktopTheme.applyWindowIcon(d);

        JTextArea area = new JTextArea(CoachSkills.buildProgramSummary(userTeam, hc));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleTextContent(area);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        d.add(buildNilCard(userTeam), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        d.add(scroll, BorderLayout.CENTER);

        JLabel xpLabel = new JLabel();
        xpLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        xpLabel.setForeground(DesktopTheme.textPrimary());
        Runnable refreshXp = () -> xpLabel.setText("Skill XP: " + hc.coachSkillXp);
        refreshXp.run();

        JComboBox<Integer> branchBox = new JComboBox<>();
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            branchBox.addItem(b);
        }
        branchBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Integer bi) {
                    int r = CoachSkills.getRank(hc.coachSkillRanksBits, bi);
                    setText(CoachSkills.branchPickerLabel(hc.coachSkillRanksBits, bi));
                }
                return this;
            }
        });

        JButton upgrade = new JButton(CoachSkills.UPGRADE_BRANCH_BUTTON_LABEL);
        DesktopTheme.stylePrimaryButton(upgrade);
        upgrade.addActionListener(e -> {
            int b = (Integer) branchBox.getSelectedItem();
            int cur = CoachSkills.getRank(hc.coachSkillRanksBits, b);
            int cost = CoachSkills.costForNextRank(cur);
            if (cur >= 3) {
                JOptionPane.showMessageDialog(d,
                        DesktopTheme.messageForDialog("This branch is maxed."),
                        "Coach Program",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (hc.coachSkillXp < cost) {
                JOptionPane.showMessageDialog(d,
                        DesktopTheme.messageForDialog(
                                "Need " + cost + " XP (you have " + hc.coachSkillXp + ").\n"
                                        + "XP builds each week you sim."),
                        "Coach Program",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (hc.tryPurchaseCoachSkillRank(b)) {
                area.setText(CoachSkills.buildProgramSummary(userTeam, hc));
                branchBox.repaint();
                refreshXp.run();
                changed.run();
            }
        });

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        south.add(xpLabel, BorderLayout.NORTH);

        JLabel hint = new JLabel("<html><i>" + CoachSkills.PROGRAM_DIALOG_FOOTER_HINT + "</i></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(DesktopTheme.textSecondary());
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        footer.add(hint, BorderLayout.CENTER);
        south.add(footer, BorderLayout.SOUTH);

        // BorderLayout, not FlowLayout: in a 560px dialog the flow row wrapped
        // the upgrade button onto a clipped second line with no visible label.
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel branchLabel = new JLabel("Branch:");
        branchLabel.setForeground(DesktopTheme.textPrimary());
        row.add(branchLabel, BorderLayout.WEST);
        row.add(branchBox, BorderLayout.CENTER);
        row.add(upgrade, BorderLayout.EAST);
        south.add(row, BorderLayout.CENTER);

        d.add(south, BorderLayout.SOUTH);

        JButton close = new JButton("Close");
        DesktopTheme.styleSecondaryButton(close);
        close.addActionListener(e -> d.dispose());
        footer.add(close, BorderLayout.EAST);

        d.setVisible(true);
    }

    /**
     * NIL collective card: gold tier label, progress toward the next tier's
     * cost, the effects the sim actually applies, and the offseason rule.
     */
    private static JPanel buildNilCard(simulation.Team team) {
        simulation.NilCollectiveStatus nil = simulation.NilCollectiveStatus.of(team);
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setOpaque(true);
        card.setBackground(DesktopTheme.tableBase());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("NIL COLLECTIVE");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        title.setForeground(DesktopTheme.textSecondary());
        JLabel tier = new JLabel(nil.tierLabel());
        tier.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        tier.setForeground(DesktopTheme.warningText());
        top.add(title, BorderLayout.WEST);
        top.add(tier, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        javax.swing.JProgressBar bar = new javax.swing.JProgressBar(0, 100);
        bar.setValue(nil.progressPercent());
        bar.setStringPainted(false);
        bar.setPreferredSize(new java.awt.Dimension(10, 8));
        bar.setForeground(nil.canAffordNextTier() || nil.isMaxed()
                ? DesktopTheme.successGreen() : DesktopTheme.warningText());
        bar.setBackground(DesktopTheme.windowBackground());
        bar.setBorderPainted(false);
        bar.getAccessibleContext().setAccessibleName("Budget progress toward the next NIL tier");
        card.add(bar, BorderLayout.CENTER);

        JLabel text = new JLabel("<html>" + escape(nil.effectsLine()) + "<br><span style='font-size:9pt;'>"
                + escape(nil.nextTierLine()) + "</span></html>");
        text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        text.setForeground(DesktopTheme.textPrimary());
        card.add(text, BorderLayout.SOUTH);

        // Transparent margin wrapper so the card's fill doesn't bleed into the gutter.
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
