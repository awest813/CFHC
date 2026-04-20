package desktop;

import simulation.League;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Settings / preferences dialog for the desktop app.
 *
 * <p>Exposes the boolean flags stored in the live {@link League} object:
 * <ul>
 *   <li><b>Full Game Log</b> — enables detailed play-by-play logging for all games</li>
 *   <li><b>Conference Realignment</b> — allows teams to change conferences each off-season</li>
 *   <li><b>Advanced Realignment</b> — enables the more aggressive realignment events</li>
 *   <li><b>Never Retire</b> — prevents the coach from being forced to retire</li>
 * </ul>
 *
 * <p>Changes are applied immediately to the live league object. The user should save
 * their league afterwards to persist the settings.
 */
public class SettingsDialog extends JDialog {

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 13);

    /** Set to {@code true} when the user clicks Apply &amp; Close. */
    private boolean applied = false;

    public SettingsDialog(JFrame owner, League league) {
        super(owner, "Settings", true);
        setSize(420, 320);
        setLayout(new BorderLayout(10, 10));

        JPanel content = new JPanel(new GridLayout(0, 1, 6, 6));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("League Settings");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        content.add(header);

        JCheckBox fullLog = new JCheckBox("Full Game Log (detailed play-by-play)");
        fullLog.setFont(LABEL_FONT);
        fullLog.setSelected(league.fullGameLog);
        content.add(fullLog);

        JCheckBox confRealign = new JCheckBox("Conference Realignment");
        confRealign.setFont(LABEL_FONT);
        confRealign.setSelected(league.confRealignment);
        content.add(confRealign);

        JCheckBox advRealign = new JCheckBox("Advanced Realignment (aggressive conference moves)");
        advRealign.setFont(LABEL_FONT);
        advRealign.setSelected(league.advancedRealignment);
        content.add(advRealign);

        JCheckBox neverRetire = new JCheckBox("Never Retire (prevent forced coach retirement)");
        neverRetire.setFont(LABEL_FONT);
        neverRetire.setSelected(league.neverRetire);
        content.add(neverRetire);

        JLabel hint = new JLabel("Changes take effect immediately. Save your league to persist them.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        content.add(hint);

        add(content, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton applyBtn = new JButton("Apply & Close");
        applyBtn.addActionListener(e -> {
            league.fullGameLog = fullLog.isSelected();
            league.confRealignment = confRealign.isSelected();
            league.advancedRealignment = advRealign.isSelected();
            league.neverRetire = neverRetire.isSelected();
            applied = true;
            dispose();
        });
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        bottom.add(applyBtn);
        bottom.add(cancelBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    /** Returns {@code true} if the user applied settings changes. */
    public boolean wasApplied() {
        return applied;
    }

    /**
     * Shows the settings dialog and blocks until the user closes it.
     * Returns {@code true} if the user applied changes.
     *
     * @param owner  parent frame
     * @param league the active league whose settings will be modified
     * @return {@code true} if settings were changed
     */
    public static boolean show(JFrame owner, League league) {
        SettingsDialog dlg = new SettingsDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        return dlg.wasApplied();
    }
}
