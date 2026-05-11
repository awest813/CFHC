package desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * A modal dialog that provides visual feedback during long simulation runs.
 */
public class SimulationProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private boolean cancelled = false;

    public SimulationProgressDialog(JFrame owner, String title) {
        super(owner, title, true);
        setSize(420, 185);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        DesktopTheme.styleDialogContentPane(getContentPane());

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 20, 24));

        statusLabel = new JLabel("Simulating season...");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        statusLabel.setForeground(DesktopTheme.textPrimary());
        panel.add(statusLabel, BorderLayout.NORTH);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(370, 26));
        progressBar.setBackground(DesktopTheme.textAreaEditorBackground());
        progressBar.setForeground(DesktopTheme.isDark()
                ? new Color(100, 170, 255)
                : new Color(50, 120, 210));
        panel.add(progressBar, BorderLayout.CENTER);

        JButton cancelBtn = new JButton("Interrupt");
        DesktopTheme.styleSecondaryButton(cancelBtn);
        cancelBtn.addActionListener(e -> cancelled = true);
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(cancelBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        add(panel);
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }

    public void setProgress(int value) {
        progressBar.setValue(value);
    }

    public void setIndeterminate(boolean ind) {
        progressBar.setIndeterminate(ind);
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
