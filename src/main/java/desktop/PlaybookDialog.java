package desktop;

import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.PlatformLog;
import simulation.Team;

import javax.swing.BorderFactory;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Dialog for viewing and changing the team's tactical schemes.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class PlaybookDialog extends JDialog {

    private static final String TAG = "PlaybookDialog";
    
    private static final Color BG_COLOR = new Color(15, 20, 28);
    private static final Color SURFACE_COLOR = new Color(25, 32, 45);
    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color TEXT_SECONDARY = new Color(171, 178, 191);

    private final Team team;

    public PlaybookDialog(JFrame owner, Team team) {
        super(owner, "TACTICAL SUITE — " + team.getName().toUpperCase(), true);
        this.team = team;
        setSize(850, 650);
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
        header.setBorder(BorderFactory.createEmptyBorder(25, 30, 20, 30));
        
        JLabel title = new JLabel("TEAM SCHEMATICS");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        
        JLabel subtitle = new JLabel(team.getAbbr() + " STRATEGY CONFIGURATION");
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        subtitle.setForeground(ACCENT_BLUE);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel(new GridLayout(1, 2, 30, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        content.add(buildTacticalPanel("OFFENSIVE DOCTRINE", team.getPlaybookOff(), true));
        content.add(buildTacticalPanel("DEFENSIVE DOCTRINE", team.getPlaybookDef(), false));
        add(content, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        bottom.setBackground(SURFACE_COLOR);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)));
        
        JButton closeBtn = new JButton("FINALIZE STRATEGY") {
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
        closeBtn.setBackground(ACCENT_BLUE);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
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
        label.setForeground(ACCENT_BLUE);
        panel.add(label, BorderLayout.NORTH);

        String[] names = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            names[i] = isOffense ? ((PlaybookOffense)options[i]).getStratName().toUpperCase() : ((PlaybookDefense)options[i]).getStratName().toUpperCase();
        }

        JComboBox<String> combo = new JComboBox<>(names);
        combo.setSelectedIndex(isOffense ? team.getPlaybookOffNum() : team.getPlaybookDefNum());
        combo.setBackground(SURFACE_COLOR);
        combo.setForeground(Color.WHITE);
        combo.setFont(new Font("SansSerif", Font.BOLD, 15));
        combo.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 20)));

        JTextArea desc = new JTextArea();
        desc.setEditable(false);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setFont(new Font("Serif", Font.ITALIC, 16));
        desc.setForeground(TEXT_SECONDARY);
        desc.setOpaque(false);

        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        Runnable updateDesc = () -> {
            int idx = combo.getSelectedIndex();
            statsPanel.removeAll();
            if (isOffense) {
                PlaybookOffense pb = (PlaybookOffense) options[idx];
                desc.setText(pb.getStratDescription());
                addStatRow(statsPanel, "RUN PREFERENCE", pb.getRunPref());
                addStatRow(statsPanel, "RUN PROTECTION", pb.getRunProtection());
                addStatRow(statsPanel, "RUN POTENTIAL", pb.getRunPotential());
                addStatRow(statsPanel, "TE USAGE", pb.getRunUsage());
                addStatRow(statsPanel, "PASS PREFERENCE", pb.getPassPref());
                addStatRow(statsPanel, "PASS PROTECTION", pb.getPassProtection());
            } else {
                PlaybookDefense pb = (PlaybookDefense) options[idx];
                desc.setText(pb.getStratDescription());
                addStatRow(statsPanel, "RUN PREFERENCE", pb.getRunPref());
                addStatRow(statsPanel, "RUN STOPPING", pb.getRunStop());
                addStatRow(statsPanel, "RUN COVERAGE", pb.getRunCoverage());
                addStatRow(statsPanel, "PASS PREFERENCE", pb.getPassPref());
                addStatRow(statsPanel, "PASS RUSH", pb.getPassRush());
                addStatRow(statsPanel, "PASS COVERAGE", pb.getPassCoverage());
            }
            statsPanel.revalidate();
            statsPanel.repaint();
        };

        combo.addActionListener(e -> {
            int idx = combo.getSelectedIndex();
            if (isOffense) {
                team.setPlaybookOffNum(idx);
                team.setPlaybookOffense((PlaybookOffense) options[idx]);
            } else {
                team.setPlaybookDefNum(idx);
                team.setPlaybookDefense((PlaybookDefense) options[idx]);
            }
            updateDesc.run();
        });
        updateDesc.run();

        JPanel center = new JPanel(new BorderLayout(0, 25));
        center.setOpaque(false);
        center.add(desc, BorderLayout.NORTH);
        center.add(statsPanel, BorderLayout.CENTER);

        panel.add(combo, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void addStatRow(JPanel container, String label, int val) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 5)));
        
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(TEXT_SECONDARY);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        
        JLabel v = new JLabel((val > 0 ? "+" : "") + val);
        v.setFont(new Font("Monospaced", Font.BOLD, 14));
        v.setForeground(val > 0 ? new Color(46, 204, 113) : (val < 0 ? new Color(231, 76, 60) : Color.WHITE));
        
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        container.add(row);
    }

    public static void show(JFrame owner, Team team) {
        PlaybookDialog dlg = new PlaybookDialog(owner, team);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
