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

        JDialog d = new JDialog(owner, "Coach Program, NIL & Facilities", true);
        d.setSize(520, 580);
        d.setLocationRelativeTo(owner);
        d.setLayout(new BorderLayout(0, 8));
        d.getContentPane().setBackground(DesktopTheme.windowBackground());

        JTextArea area = new JTextArea(buildSummary(userTeam, hc));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        area.setBackground(DesktopTheme.windowBackground());
        area.setForeground(DesktopTheme.textPrimary());
        area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));
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
                    setText(CoachSkills.branchTitle(bi) + "  [Rank " + r + "/3]");
                }
                return this;
            }
        });

        JButton upgrade = new JButton("Upgrade selected branch");
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
                area.setText(buildSummary(userTeam, hc));
                branchBox.repaint();
                refreshXp.run();
            }
        });

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        south.add(xpLabel, BorderLayout.NORTH);

        JLabel hint = new JLabel("<html><i>XP is earned weekly while you advance the season. "
                + "NIL tier grows in the offseason when your budget can fund the collective. "
                + "Training facilities still power player development in the sim core.</i></html>");
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
        close.addActionListener(e -> d.dispose());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.setOpaque(false);
        top.add(close);
        d.add(top, BorderLayout.NORTH);

        d.setVisible(true);
    }

    private static String buildSummary(Team t, Staff hc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Training facilities: L").append(t.getTeamFacilities())
                .append("  (drives base player development in the sim)\n");
        sb.append("NIL / booster collective: Tier ").append(t.nilCollectiveLevel)
                .append("  (home revenue, weekly stipend, recruiting budget)\n\n");
        sb.append("Coach skills:\n");
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            int r = CoachSkills.getRank(hc.coachSkillRanksBits, b);
            int next = CoachSkills.costForNextRank(r);
            sb.append("• ").append(CoachSkills.branchTitle(b)).append(" — rank ").append(r).append("/3");
            if (r < 3) {
                sb.append("  (next: ").append(next).append(" XP)\n");
            } else {
                sb.append("  (max)\n");
            }
            sb.append("  ").append(CoachSkills.branchBlurb(b)).append("\n");
        }
        return sb.toString();
    }
}
