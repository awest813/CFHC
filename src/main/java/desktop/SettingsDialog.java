package desktop;

import simulation.League;
import simulation.LeagueSettingsOptions;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
 */
public class SettingsDialog extends JDialog {

    private static final Color ACCENT_BLUE = new Color(52, 152, 219);

    private boolean applied = false;

    public SettingsDialog(JFrame owner, League league) {
        super(owner, "League Settings", true);
        setSize(560, 680);
        setMinimumSize(new java.awt.Dimension(520, 560));
        setLayout(new BorderLayout());
        getContentPane().setBackground(dialogBackground());

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
        header.setBackground(dialogSurface());
        header.setBorder(BorderFactory.createEmptyBorder(22, 30, 20, 30));
        
        JLabel title = new JLabel("League Settings");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(DesktopTheme.textPrimary());
        JLabel subtitle = new JLabel("Adjust career, simulation, and universe rules.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(DesktopTheme.textSecondary());
        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        header.add(titleBlock, BorderLayout.WEST);
        
        add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));

        JLabel displaySection = sectionLabel("Display");
        content.add(displaySection);
        content.add(Box.createVerticalStrut(8));
        JCheckBox desktopDark = createStyledCheckBox("Dark mode", DesktopTheme.isDark(), content);
        content.add(Box.createVerticalStrut(12));

        JLabel gameplaySection = sectionLabel("Gameplay");
        content.add(gameplaySection);
        content.add(Box.createVerticalStrut(8));
        JCheckBox showPotential = createStyledCheckBox("Show player potential", league.showPotential, content);
        content.add(Box.createVerticalStrut(12));
        JCheckBox fullLog = createStyledCheckBox("Detailed simulation logs", league.fullGameLog, content);
        content.add(Box.createVerticalStrut(12));
        JCheckBox careerMode = createStyledCheckBox("Coach career movement", league.careerMode, content);
        content.add(Box.createVerticalStrut(12));
        JCheckBox neverRetire = createStyledCheckBox("Infinite career mode", league.neverRetire, content);
        content.add(Box.createVerticalStrut(12));
        JCheckBox enableTv = createStyledCheckBox("TV contracts", league.enableTV, content);
        content.add(Box.createVerticalStrut(12));

        JLabel universeSection = sectionLabel("Universe Rules");
        content.add(universeSection);
        content.add(Box.createVerticalStrut(8));
        JCheckBox expandedPlayoffs = createStyledCheckBox("Expanded playoffs", league.expPlayoffs, content);
        expandedPlayoffs.setEnabled(league.currentWeek < league.regSeasonWeeks);
        if (!expandedPlayoffs.isEnabled()) {
            expandedPlayoffs.setToolTipText("Expanded playoffs can only be changed before the regular season starts.");
        }
        content.add(Box.createVerticalStrut(15));
        JCheckBox confRealign = createStyledCheckBox("Conference realignment", league.confRealignment, content);
        content.add(Box.createVerticalStrut(15));
        JCheckBox advRealign = createStyledCheckBox("Advanced transfers and realignment", league.advancedRealignment, content);
        content.add(Box.createVerticalStrut(15));
        JCheckBox universalProRel = createStyledCheckBox("Universal promotion / relegation", league.enableUnivProRel, content);
        universalProRel.setEnabled(league.currentWeek == 0);
        if (!universalProRel.isEnabled()) {
            universalProRel.setToolTipText("Promotion/relegation conversion is locked after Week 0.");
        }
        wireMutuallyExclusiveLeagueModes(confRealign, advRealign, universalProRel);

        content.add(Box.createVerticalGlue());

        JLabel hint = new JLabel("<html><body style='width: 390px;'><i>Settings are applied immediately to the active universe. Expanded playoffs and promotion/relegation can only be changed before the regular season starts. Save your league to persist these changes.</i></body></html>");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(DesktopTheme.textSecondary());
        hint.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        content.add(hint);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        bottom.setBackground(dialogSurface());
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)));
        
        JButton cancelBtn = new JButton("Cancel") {
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
        cancelBtn.setForeground(DesktopTheme.textSecondary());
        cancelBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        cancelBtn.addActionListener(e -> dispose());
        
        JButton applyBtn = new JButton("Apply Settings") {
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
            boolean enablingProRel = universalProRel.isEnabled()
                    && universalProRel.isSelected()
                    && !league.enableUnivProRel;
            if (enablingProRel && !confirmPromotionRelegationConversion()) {
                return;
            }

            LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(league);
            options.showPotential = showPotential.isSelected();
            options.fullGameLog = fullLog.isSelected();
            options.careerMode = careerMode.isSelected();
            options.neverRetire = neverRetire.isSelected();
            options.enableTv = enableTv.isSelected();
            options.expandedPlayoffs = expandedPlayoffs.isSelected();
            options.conferenceRealignment = confRealign.isSelected();
            options.advancedRealignment = advRealign.isSelected();
            options.universalProRel = universalProRel.isSelected();
            options.applyTo(league, expandedPlayoffs.isEnabled(), universalProRel.isEnabled(), true);
            DesktopTheme.setDark(desktopDark.isSelected());
            java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(SettingsDialog.this);
            if (w instanceof LeagueHomeView) {
                ((LeagueHomeView) w).applyDesktopTheme();
            }
            applied = true;
            dispose();
        });
        
        bottom.add(cancelBtn);
        bottom.add(applyBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void wireMutuallyExclusiveLeagueModes(JCheckBox confRealign,
                                                  JCheckBox advRealign,
                                                  JCheckBox universalProRel) {
        advRealign.addActionListener(e -> {
            if (advRealign.isSelected()) {
                confRealign.setSelected(true);
                universalProRel.setSelected(false);
            }
        });
        confRealign.addActionListener(e -> {
            if (confRealign.isSelected()) {
                universalProRel.setSelected(false);
            }
        });
        universalProRel.addActionListener(e -> {
            if (universalProRel.isSelected()) {
                confRealign.setSelected(false);
                advRealign.setSelected(false);
            }
        });
    }

    private boolean confirmPromotionRelegationConversion() {
        int choice = JOptionPane.showConfirmDialog(this,
                DesktopTheme.messageForDialog("Universal promotion/relegation rewrites the conference structure and schedule.\nThis should only be enabled before Week 1.\n\nConvert this league now?"),
                "Enable Promotion/Relegation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private static Color dialogBackground() {
        return DesktopTheme.isDark() ? new Color(15, 20, 28) : DesktopTheme.windowBackground();
    }

    private static Color dialogSurface() {
        return DesktopTheme.isDark() ? new Color(25, 32, 45) : new Color(246, 248, 251);
    }

    private static JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(DesktopTheme.textSecondary());
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        return label;
    }

    private JCheckBox createStyledCheckBox(String label, boolean selected, JPanel container) {
        JCheckBox cb = new JCheckBox(label, selected);
        cb.setFont(new Font("SansSerif", Font.BOLD, 13));
        cb.setForeground(DesktopTheme.textPrimary());
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        cb.setIcon(new javax.swing.ImageIcon() {
            @Override public int getIconWidth() { return 18; }
            @Override public int getIconHeight() { return 18; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DesktopTheme.isDark() ? new Color(60, 75, 95) : new Color(166, 176, 190));
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
