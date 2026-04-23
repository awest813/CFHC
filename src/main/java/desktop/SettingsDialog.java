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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BoxLayout;
import javax.swing.Box;

/**
 * Settings / preferences dialog for the desktop app.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class SettingsDialog extends JDialog {

    private static final Color BG_COLOR = new Color(15, 20, 28);
    private static final Color SURFACE_COLOR = new Color(25, 32, 45);
    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color TEXT_SECONDARY = new Color(171, 178, 191);

    private boolean applied = false;

    public SettingsDialog(JFrame owner, League league) {
        super(owner, "SYSTEM CONFIGURATION", true);
        setSize(520, 550);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setBackground(SURFACE_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        JLabel title = new JLabel("LEAGUE PARAMETERS");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        
        add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JCheckBox fullLog = createStyledCheckBox("ENABLE DETAILED SIMULATION LOGS", league.fullGameLog, content);
        content.add(Box.createVerticalStrut(15));
        JCheckBox confRealign = createStyledCheckBox("ENABLE CONFERENCE REALIGNMENT", league.confRealignment, content);
        content.add(Box.createVerticalStrut(15));
        JCheckBox advRealign = createStyledCheckBox("ADVANCED TRANSFERS & REALIGNMENT", league.advancedRealignment, content);
        content.add(Box.createVerticalStrut(15));
        JCheckBox neverRetire = createStyledCheckBox("INFINITE CAREER MODE (NO RETIREMENT)", league.neverRetire, content);

        content.add(Box.createVerticalGlue());

        JLabel hint = new JLabel("<html><body style='width: 350px;'><i>Note: Settings are applied immediately to the active universe. Save your league to persist these changes.</i></body></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(TEXT_SECONDARY);
        hint.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        content.add(hint);

        add(content, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        bottom.setBackground(SURFACE_COLOR);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)));
        
        JButton cancelBtn = new JButton("DISCARD") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        cancelBtn.setForeground(TEXT_SECONDARY);
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        cancelBtn.addActionListener(e -> dispose());
        
        JButton applyBtn = new JButton("APPLY SETTINGS") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        applyBtn.setBackground(ACCENT_BLUE);
        applyBtn.setForeground(Color.WHITE);
        applyBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        applyBtn.setFocusPainted(false);
        applyBtn.setContentAreaFilled(false);
        applyBtn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        applyBtn.addActionListener(e -> {
            league.fullGameLog = fullLog.isSelected();
            league.confRealignment = confRealign.isSelected();
            league.advancedRealignment = advRealign.isSelected();
            league.neverRetire = neverRetire.isSelected();
            applied = true;
            dispose();
        });
        
        bottom.add(cancelBtn);
        bottom.add(applyBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private JCheckBox createStyledCheckBox(String label, boolean selected, JPanel container) {
        JCheckBox cb = new JCheckBox(label, selected);
        cb.setFont(new Font("SansSerif", Font.BOLD, 13));
        cb.setForeground(Color.WHITE);
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        cb.setIcon(new javax.swing.ImageIcon() {
            @Override public int getIconWidth() { return 18; }
            @Override public int getIconHeight() { return 18; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(60, 75, 95));
                g2.drawRoundRect(x, y, 17, 17, 4, 4);
                g2.dispose();
            }
        });
        cb.setSelectedIcon(new javax.swing.ImageIcon() {
            @Override public int getIconWidth() { return 18; }
            @Override public int getIconHeight() { return 18; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_BLUE);
                g2.fillRoundRect(x, y, 17, 17, 4, 4);
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(2));
                g2.drawLine(x + 4, y + 9, x + 8, y + 13);
                g2.drawLine(x + 8, y + 13, x + 13, y + 5);
                g2.dispose();
            }
        });
        cb.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        container.add(cb);
        return cb;
    }

    public boolean wasApplied() {
        return applied;
    }

    public static boolean show(JFrame owner, League league) {
        SettingsDialog dlg = new SettingsDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
        return dlg.wasApplied();
    }
}
