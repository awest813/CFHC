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
        if (userTeam == null || userTeam.getHeadCoach() == null) {
            return;
        }
        final Staff hc = userTeam.getHeadCoach();

        JDialog d = new JDialog(owner, CoachSkills.PROGRAM_DIALOG_TITLE, true);
        d.setSize(520, 580);
        d.setLocationRelativeTo(owner);
        d.setLayout(new BorderLayout(0, 8));
        d.getContentPane().setBackground(DesktopTheme.windowBackground());

        JTextArea area = new JTextArea(CoachSkills.buildProgramSummary(userTeam, hc));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleTextContent(area);
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

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
                JOptionPane.showMessageDialog(d, "This branch is maxed.");
                return;
            }
            if (hc.coachSkillXp < cost) {
                JOptionPane.showMessageDialog(d, "Need " + cost + " XP (you have " + hc.coachSkillXp + ").\nXP builds each week you sim.");
                return;
            }
            if (hc.tryPurchaseCoachSkillRank(b)) {
                area.setText(CoachSkills.buildProgramSummary(userTeam, hc));
                branchBox.repaint();
                refreshXp.run();
            }
        });

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        south.add(xpLabel, BorderLayout.NORTH);

        JLabel hint = new JLabel("<html><i>" + CoachSkills.PROGRAM_DIALOG_FOOTER_HINT + "</i></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setForeground(DesktopTheme.textSecondary());
        south.add(hint, BorderLayout.SOUTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.add(new JLabel("Branch:"));
        row.add(branchBox);
        row.add(upgrade);
        south.add(row, BorderLayout.CENTER);

        d.add(south, BorderLayout.SOUTH);

        JButton close = new JButton("Close");
        DesktopTheme.styleSecondaryButton(close);
        close.addActionListener(e -> d.dispose());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setOpaque(false);
        top.add(close);
        d.add(top, BorderLayout.NORTH);

        d.setVisible(true);
    }
}
