package desktop;

import simulation.League;
import simulation.Team;
import staff.HeadCoach;
import staff.Staff;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Interactive contract-review dialog shown during the offseason.
 *
 * <p>Displays the user's coaching contract status and offers retirement
 * when the coach reaches retirement age (unless "Never Retire" is on).
 */
public class ContractDialog extends JDialog {

    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final int RETIREMENT_AGE = 65;

    private final League league;
    private boolean retired = false;

    public ContractDialog(JFrame owner, League league) {
        super(owner, league.getYear() + " Contract Status", true);
        this.league = league;
        setSize(560, 420);
        setLayout(new BorderLayout(10, 10));

        Team userTeam = league.userTeam;
        HeadCoach hc = userTeam != null ? userTeam.getHeadCoach() : null;

        // Handle retirement check first
        if (league.isCareerMode() && hc != null && hc.age > RETIREMENT_AGE && !league.neverRetire) {
            buildRetirementPanel(hc);
        } else {
            buildContractPanel(userTeam, hc);
        }
    }

    private void buildRetirementPanel(HeadCoach hc) {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("Retirement Decision");
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        content.add(header, BorderLayout.NORTH);

        String message = "Coach " + hc.name + " (Age " + hc.age + ")\n\n"
                + "You have reached retirement age. You can choose to retire\n"
                + "and end your coaching career, or continue coaching.\n\n"
                + "Career Record: " + hc.stats[0] + "-" + hc.stats[1] + "\n"
                + "National Championships: " + hc.stats[6] + "\n"
                + "Conference Championships: " + hc.stats[2] + "\n"
                + "Bowl Wins: " + hc.stats[4] + "\n"
                + "Win %: " + String.format("%.1f%%", hc.getWinPCT() * 100);

        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setFont(BODY_FONT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        content.add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        JButton continueBtn = new JButton("Continue Coaching");
        continueBtn.addActionListener(e -> {
            retired = false;
            dispose();
        });
        JButton retireBtn = new JButton("Retire");
        retireBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to retire?\nThis cannot be undone.",
                    "Confirm Retirement", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                hc.retirement = true;
                retired = true;
                dispose();
            }
        });
        buttons.add(continueBtn);
        buttons.add(retireBtn);
        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private void buildContractPanel(Team userTeam, HeadCoach hc) {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("Contract Review — " + league.getYear());
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        content.add(header, BorderLayout.NORTH);

        StringBuilder sb = new StringBuilder();
        if (userTeam != null) {
            String contractStr = userTeam.getContractString();
            if (contractStr != null && !contractStr.isEmpty()) {
                sb.append(contractStr);
            } else {
                sb.append("No contract information available.");
            }

            if (hc != null) {
                sb.append("\n\n--- Coaching Profile ---\n");
                sb.append("Name: ").append(hc.name).append("\n");
                sb.append("Age: ").append(hc.age).append("\n");
                sb.append("Overall: ").append(hc.ratOvr).append("\n");
                sb.append("Status: ").append(hc.coachStatus()).append("\n");
                sb.append("Season Grade: ").append(hc.getSeasonGrade()).append("\n");
                sb.append("Contract: Year ").append(hc.contractYear)
                  .append(" of ").append(hc.contractLength).append("\n");
                sb.append("Career: ").append(hc.stats[0]).append("-").append(hc.stats[1])
                  .append("  (").append(String.format("%.1f%%", hc.getWinPCT() * 100)).append(")\n");
            }
        } else {
            sb.append("No user team selected.");
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(BODY_FONT);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        content.add(new JScrollPane(area), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton okBtn = new JButton("OK");
        okBtn.addActionListener(e -> dispose());
        buttons.add(okBtn);
        content.add(buttons, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    public boolean didRetire() {
        return retired;
    }

    /**
     * Shows the contract dialog and blocks until closed.
     * Returns true if the coach chose to retire.
     */
    public static boolean show(JFrame owner, League league) {
        ContractDialog dlg = new ContractDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        return dlg.didRetire();
    }
}
