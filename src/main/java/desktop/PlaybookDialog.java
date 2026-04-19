package desktop;

import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.PlatformLog;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Dialog for viewing and changing the user team's offensive and defensive
 * playbooks.  Each playbook is selected from a combo box; a description
 * and stat modifiers are shown beneath.
 */
public class PlaybookDialog extends JDialog {

    private static final String TAG = "PlaybookDialog";

    private final Team team;

    public PlaybookDialog(JFrame owner, Team team) {
        super(owner, "Playbooks — " + team.getName(), true);
        this.team = team;
        setSize(640, 520);
        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel(new GridLayout(1, 2, 10, 0));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildOffensePanel());
        content.add(buildDefensePanel());
        add(content, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    // -------------------------------------------------------------------------
    // Offense panel
    // -------------------------------------------------------------------------

    private JPanel buildOffensePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Offense"));

        PlaybookOffense[] options = team.getPlaybookOff();
        String[] names = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            names[i] = options[i].getStratName();
        }

        JComboBox<String> combo = new JComboBox<>(names);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        combo.setSelectedIndex(team.getPlaybookOffNum());

        JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        Runnable updateDesc = () -> {
            int idx = combo.getSelectedIndex();
            PlaybookOffense pb = options[idx];
            desc.setText(
                    pb.getStratName() + "\n\n"
                    + pb.getStratDescription() + "\n\n"
                    + "Run Preference: " + modifier(pb.getRunPref()) + "\n"
                    + "Run Protection: " + modifier(pb.getRunProtection()) + "\n"
                    + "Run Potential: " + modifier(pb.getRunPotential()) + "\n"
                    + "Run TE Usage: " + modifier(pb.getRunUsage()) + "\n\n"
                    + "Pass Preference: " + modifier(pb.getPassPref()) + "\n"
                    + "Pass Protection: " + modifier(pb.getPassProtection()) + "\n"
                    + "Pass Potential: " + modifier(pb.getPassPotential()) + "\n"
                    + "Pass TE Usage: " + modifier(pb.getPassUsage())
            );
            desc.setCaretPosition(0);
        };

        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            team.setPlaybookOffNum(idx);
            team.setPlaybookOffense(options[idx]);
            PlatformLog.i(TAG, "Offense playbook changed to " + options[idx].getStratName());
            updateDesc.run();
        });
        updateDesc.run();

        panel.add(combo, BorderLayout.NORTH);
        panel.add(desc, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Defense panel
    // -------------------------------------------------------------------------

    private JPanel buildDefensePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Defense"));

        PlaybookDefense[] options = team.getPlaybookDef();
        String[] names = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            names[i] = options[i].getStratName();
        }

        JComboBox<String> combo = new JComboBox<>(names);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        combo.setSelectedIndex(team.getPlaybookDefNum());

        JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 13));

        Runnable updateDesc = () -> {
            int idx = combo.getSelectedIndex();
            PlaybookDefense pb = options[idx];
            desc.setText(
                    pb.getStratName() + "\n\n"
                    + pb.getStratDescription() + "\n\n"
                    + "Run Preference: " + modifier(pb.getRunPref()) + "\n"
                    + "Run Stop: " + modifier(pb.getRunStop()) + "\n"
                    + "Run Coverage: " + modifier(pb.getRunCoverage()) + "\n"
                    + "Run Spy: " + modifier(pb.getRunSpy()) + "\n\n"
                    + "Pass Preference: " + modifier(pb.getPassPref()) + "\n"
                    + "Pass Rush: " + modifier(pb.getPassRush()) + "\n"
                    + "Pass Coverage: " + modifier(pb.getPassCoverage()) + "\n"
                    + "Pass Spy: " + modifier(pb.getPassSpy())
            );
            desc.setCaretPosition(0);
        };

        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            team.setPlaybookDefNum(idx);
            team.setPlaybookDefense(options[idx]);
            PlatformLog.i(TAG, "Defense playbook changed to " + options[idx].getStratName());
            updateDesc.run();
        });
        updateDesc.run();

        panel.add(combo, BorderLayout.NORTH);
        panel.add(desc, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String modifier(int value) {
        if (value > 0) return "+" + value;
        return String.valueOf(value);
    }

    public static void show(JFrame owner, Team team) {
        PlaybookDialog dlg = new PlaybookDialog(owner, team);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
