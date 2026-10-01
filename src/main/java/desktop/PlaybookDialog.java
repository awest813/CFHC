package desktop;

import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.Locale;

/**
 * Dialog for viewing and changing the team's tactical schemes.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class PlaybookDialog extends JDialog {

    private final Team team;
    private final Runnable onChanged;

    public PlaybookDialog(JFrame owner, Team team) {
        this(owner, team, null);
    }

    public PlaybookDialog(JFrame owner, Team team, Runnable onChanged) {
        super(owner, "SCHEME ROOM - " + team.getName().toUpperCase(Locale.ROOT), true);
        this.team = team;
        this.onChanged = onChanged != null ? onChanged : () -> {};
        setSize(850, 700);
        setLayout(new BorderLayout());
        DesktopTheme.styleDialogContentPane(getContentPane());
        DesktopTheme.applyWindowIcon(this);

        // Header
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DesktopTheme.borderSubtle());
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setBackground(DesktopTheme.tableBase());
        header.setBorder(BorderFactory.createEmptyBorder(25, 30, 20, 30));
        
        JLabel title = new JLabel("TEAM SCHEMES");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(DesktopTheme.textPrimary());
        header.add(title, BorderLayout.WEST);
        
        JLabel subtitle = new JLabel(team.getAbbr() + " GAMEPLAN CONFIGURATION");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        subtitle.setForeground(DesktopTheme.accentBlue());
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel(new GridLayout(1, 2, 30, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        content.add(buildTacticalPanel("OFFENSIVE SCHEME", team.getPlaybookOff(), true));
        content.add(buildTacticalPanel("DEFENSIVE SCHEME", team.getPlaybookDef(), false));
        add(content, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        bottom.setBackground(DesktopTheme.tableBase());
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()));
        
        JButton closeBtn = new JButton("FINALIZE GAMEPLAN") {
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
        closeBtn.setBackground(DesktopTheme.accentBlue());
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(12, 40, 12, 40));
        closeBtn.addActionListener(e -> dispose());
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel buildTacticalPanel(String title, Object[] options, boolean isOffense) {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);
        
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(DesktopTheme.accentBlue());
        label.setAlignmentX(LEFT_ALIGNMENT);

        String[] names = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            names[i] = isOffense ? ((PlaybookOffense)options[i]).getStratName().toUpperCase(Locale.ROOT) : ((PlaybookDefense)options[i]).getStratName().toUpperCase(Locale.ROOT);
        }

        JComboBox<String> combo = new JComboBox<>(names);
        combo.setSelectedIndex(isOffense ? team.getPlaybookOffNum() : team.getPlaybookDefNum());
        combo.setBackground(DesktopTheme.tableBase());
        combo.setForeground(DesktopTheme.textPrimary());
        combo.setFont(new Font("SansSerif", Font.BOLD, 15));
        combo.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));
        combo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel fit = new JLabel();
        fit.setFont(new Font("SansSerif", Font.BOLD, 11));
        fit.setAlignmentX(LEFT_ALIGNMENT);

        // Title above the picker (both used to share BorderLayout.NORTH, so the
        // picker replaced the title).
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(label);
        top.add(Box.createVerticalStrut(8));
        top.add(combo);
        top.add(Box.createVerticalStrut(8));
        top.add(fit);

        JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(new Font("Serif", Font.ITALIC, 16));
        desc.setForeground(DesktopTheme.textSecondary());
        desc.setOpaque(false);

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Only what the game reads. Call weights (how often a run or pass is called,
        // or which the defense takes away) are shown plain; they are not bonuses.
        Runnable updateDesc = () -> {
            int idx = combo.getSelectedIndex();
            statsPanel.removeAll();
            if (isOffense) {
                PlaybookOffense pb = (PlaybookOffense) options[idx];
                desc.setText(pb.getStratDescription());
                addWeightRow(statsPanel, "RUN CALLS", pb.getRunPref());
                addWeightRow(statsPanel, "PASS CALLS", pb.getPassPref());
                addStatRow(statsPanel, "RUN BLOCKING", pb.getRunProtection());
                addStatRow(statsPanel, "BIG-RUN POTENTIAL", pb.getRunPotential());
                addStatRow(statsPanel, "PASS PROTECTION", pb.getPassProtection());
                addStatRow(statsPanel, "BIG-PLAY POTENTIAL", pb.getPassPotential());
                addWeightRow(statsPanel, "THROWS TO BACKS & TES", pb.getPassUsage());
            } else {
                PlaybookDefense pb = (PlaybookDefense) options[idx];
                desc.setText(pb.getStratDescription());
                addWeightRow(statsPanel, "TAKES AWAY THE RUN", pb.getRunPref());
                addWeightRow(statsPanel, "TAKES AWAY THE PASS", pb.getPassPref());
                addStatRow(statsPanel, "RUN STOPPING", pb.getRunStop());
                addStatRow(statsPanel, "BIG-RUN PREVENTION", pb.getRunCoverage());
                addStatRow(statsPanel, "PASS RUSH", pb.getPassRush());
                addStatRow(statsPanel, "PASS COVERAGE", pb.getPassCoverage());
            }
            describeFit(fit, idx, isOffense);
            statsPanel.revalidate();
            statsPanel.repaint();
        };

        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            if (isOffense) {
                team.setPlaybookOffNum(idx);
            } else {
                team.setPlaybookDefNum(idx);
            }
            onChanged.run();
            updateDesc.run();
        });
        updateDesc.run();

        JPanel center = new JPanel(new BorderLayout(0, 25));
        center.setOpaque(false);
        center.add(desc, BorderLayout.NORTH);
        center.add(statsPanel, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Whether the staff runs this book. A book the coordinator runs has no penalty;
     * one only the head coach runs costs a little of the coaching edge in each
     * matchup, one neither runs costs more (Game.getCoachAdv).
     */
    private void describeFit(JLabel fit, int idx, boolean isOffense) {
        String text = team.schemeFitNote(isOffense, idx);
        boolean slowQb = isOffense && PlaybookOffense.forIndex(idx).featuresQbRuns()
                && team.starterQbSpeed() < Team.READ_OPTION_QB_SPEED;
        Color color = switch (team.schemeFit(isOffense, idx)) {
            case COORDINATOR -> slowQb ? DesktopTheme.warningText() : DesktopTheme.successGreen();
            case HEAD_COACH_ONLY -> DesktopTheme.warningText();
            case NEITHER -> DesktopTheme.dangerRed();
            case NO_COORDINATOR -> DesktopTheme.textSecondary();
        };
        fit.setText("<html><body style='width:" + DesktopTheme.htmlWrapWidth(360) + "px'>" + text + "</body></html>");
        fit.setToolTipText(text);
        fit.setForeground(color);
    }

    /** A how-often weight: plain, since more is not better. */
    private void addWeightRow(JPanel container, String label, int val) {
        addRow(container, label, String.valueOf(val), DesktopTheme.textPrimary());
    }

    private void addStatRow(JPanel container, String label, int val) {
        addRow(container, label, (val > 0 ? "+" : "") + val,
                val > 0 ? DesktopTheme.successGreen() : (val < 0 ? DesktopTheme.dangerRed() : DesktopTheme.textPrimary()));
    }

    private void addRow(JPanel container, String label, String value, Color color) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.borderSubtle()));
        
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(DesktopTheme.textSecondary());
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        JLabel v = new JLabel(value);
        v.setFont(new Font("Monospaced", Font.BOLD, 14));
        v.setForeground(color);
        
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        // Keep rows at their own height: BoxLayout stretched the shorter defensive
        // list to fill the column, so its rows sat out of line with the offense's.
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        container.add(row);
    }

    public static void show(JFrame owner, Team team) {
        show(owner, team, null);
    }

    public static void show(JFrame owner, Team team, Runnable onChanged) {
        PlaybookDialog dlg = new PlaybookDialog(owner, team, onChanged);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
