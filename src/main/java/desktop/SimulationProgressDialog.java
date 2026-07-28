package desktop;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * A modal dialog that provides visual feedback during long simulation runs.
 */
public class SimulationProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private volatile boolean cancelled = false;

    public SimulationProgressDialog(JFrame owner, String title) {
        super(owner, title, true);
        setSize(420, 185);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        // Closing via the title-bar X must cancel — default HIDE_ON_CLOSE would unblock
        // the EDT while the worker kept mutating the league with bulkRunning stuck true.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                requestCancel();
            }
        });
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
        progressBar.setForeground(DesktopTheme.selectionAccent());
        panel.add(progressBar, BorderLayout.CENTER);

        JButton cancelBtn = new JButton("Interrupt");
        DesktopTheme.styleSecondaryButton(cancelBtn);
        cancelBtn.addActionListener(e -> requestCancel());
        JPanel btnPanel = new JPanel();
        btnPanel.setOpaque(false);
        btnPanel.add(cancelBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        add(panel);
    }

    private void requestCancel() {
        cancelled = true;
        statusLabel.setText("Interrupting… finishing current week");
    }

    public void setStatus(String status) {
        if (!cancelled) {
            statusLabel.setText(status);
        }
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
