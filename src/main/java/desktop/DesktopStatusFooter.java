package desktop;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Bottom controller status legend & audio player footer component for {@link LeagueHomeView}.
 * Renders controller input chips ((A) SELECT, (B) BACK, (Y) HELP) and soundtrack ticker.
 */
public class DesktopStatusFooter extends JPanel {

    public DesktopStatusFooter() {
        super(new BorderLayout(20, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(1200, 36));
        setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));

        // Left Controller Input Legend
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legendPanel.setOpaque(false);

        legendPanel.add(buildButtonChip("A", "SELECT", DesktopTheme.successGreen()));
        legendPanel.add(buildButtonChip("B", "BACK", DesktopTheme.dangerRed()));
        legendPanel.add(buildButtonChip("Y", "HELP", DesktopTheme.warningText()));

        add(legendPanel, BorderLayout.WEST);

        // Right Soundtrack Audio Player Ticker
        JPanel audioTicker = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        audioTicker.setOpaque(false);

        JLabel musicIcon = new JLabel("\u266B");
        musicIcon.setFont(new Font("SansSerif", Font.BOLD, 12));
        musicIcon.setForeground(Color.WHITE);

        JLabel trackTitle = new JLabel("Campus Drive \u2014 Midnight Rally");
        trackTitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        trackTitle.setForeground(Color.WHITE);

        // Equalizer Bar Visualizer Component
        JPanel spectrumBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DesktopTheme.successGreen());
                g2.fillRect(0, 4, 3, 10);
                g2.fillRect(5, 0, 3, 14);
                g2.fillRect(10, 6, 3, 8);
                g2.fillRect(15, 2, 3, 12);
                g2.dispose();
            }
        };
        spectrumBar.setPreferredSize(new Dimension(20, 14));
        spectrumBar.setOpaque(false);

        JLabel volIcon = new JLabel("\uD83D\uDD0A");
        volIcon.setFont(new Font("SansSerif", Font.PLAIN, 11));
        volIcon.setForeground(DesktopTheme.textSecondary());

        audioTicker.add(musicIcon);
        audioTicker.add(trackTitle);
        audioTicker.add(spectrumBar);
        audioTicker.add(volIcon);

        add(audioTicker, BorderLayout.EAST);
    }

    private JPanel buildButtonChip(String letter, String label, Color btnColor) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chip.setOpaque(false);

        JLabel btn = new JLabel(letter, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(btnColor);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(16, 16));
        btn.setFont(new Font("SansSerif", Font.BOLD, 10));
        btn.setForeground(Color.BLACK);

        JLabel txt = new JLabel(label);
        txt.setFont(new Font("SansSerif", Font.BOLD, 10));
        txt.setForeground(DesktopTheme.textSecondary());

        chip.add(btn);
        chip.add(txt);
        return chip;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(5, 10, 18)); // #050A12 Obsidian Footer
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(DesktopTheme.borderSubtle());
        g2.drawLine(0, 0, getWidth(), 0);

        g2.dispose();
        super.paintComponent(g);
    }
}
