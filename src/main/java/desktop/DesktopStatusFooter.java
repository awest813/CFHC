package desktop;

import simulation.SoundtrackEngine;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

/**
 * Bottom controller status legend & soundtrack player footer for
 * {@link LeagueHomeView}. Renders controller input chips, the current
 * soundtrack track name (bound to {@link SoundtrackEngine}), an animated
 * equalizer driven by real audio amplitude, and a clickable mute toggle.
 */
public class DesktopStatusFooter extends JPanel {

    private final SoundtrackEngine engine;
    private JLabel trackTitle;
    private JLabel volIcon;
    private JPanel spectrumBar;
    private Timer eqTimer;
    private final Random eqRng = new Random();
    private final float[] barHeights = new float[4];

    public DesktopStatusFooter(SoundtrackEngine engine) {
        super(new BorderLayout(20, 0));
        this.engine = engine != null ? engine : SoundtrackEngine.NO_OP;
        setOpaque(false);
        setPreferredSize(new Dimension(1200, 36));
        setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));

        // Left Controller Input Legend — shows the real keyboard shortcuts
        // (SPACE/ENTER=select, ESC=back, F1=help) alongside the gamepad-style
        // button metaphor.
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legendPanel.setOpaque(false);
        legendPanel.add(buildButtonChip("A", "SELECT (SPACE)", DesktopTheme.successGreen()));
        legendPanel.add(buildButtonChip("B", "BACK (ESC)", DesktopTheme.dangerRed()));
        legendPanel.add(buildButtonChip("Y", "HELP (F1)", DesktopTheme.warningText()));
        add(legendPanel, BorderLayout.WEST);

        // Right Soundtrack Audio Player Ticker
        JPanel audioTicker = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        audioTicker.setOpaque(false);

        JLabel musicIcon = new JLabel("\u266B");
        musicIcon.setFont(new Font("SansSerif", Font.BOLD, 12));
        musicIcon.setForeground(Color.WHITE);

        trackTitle = new JLabel(updateTrackLabel());
        trackTitle.setFont(new Font("SansSerif", Font.PLAIN, 11));
        trackTitle.setForeground(Color.WHITE);

        // Animated equalizer — reads engine amplitude, animates bars.
        spectrumBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color eqColor = engine.getState() == SoundtrackEngine.State.PLAYING
                        ? DesktopTheme.successGreen() : DesktopTheme.textSecondary();
                g2.setColor(eqColor);
                int[] heights = { (int) (barHeights[0] * 14), (int) (barHeights[1] * 14),
                        (int) (barHeights[2] * 14), (int) (barHeights[3] * 14) };
                for (int i = 0; i < 4; i++) {
                    int h = Math.max(2, heights[i]);
                    g2.fillRect(i * 5, 14 - h, 3, h);
                }
                g2.dispose();
            }
        };
        spectrumBar.setPreferredSize(new Dimension(22, 14));
        spectrumBar.setOpaque(false);

        // Clickable speaker icon — toggles mute.
        volIcon = new JLabel(engine.isMuted() ? "\uD83D\uDD07" : "\uD83D\uDD0A", JLabel.CENTER);
        volIcon.setFont(new Font("SansSerif", Font.PLAIN, 12));
        volIcon.setForeground(DesktopTheme.textSecondary());
        volIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        volIcon.setToolTipText("Click to toggle soundtrack");
        volIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean nowMuted = !engine.isMuted();
                engine.setMuted(nowMuted);
                volIcon.setText(nowMuted ? "\uD83D\uDD07" : "\uD83D\uDD0A");
            }
        });

        audioTicker.add(musicIcon);
        audioTicker.add(trackTitle);
        audioTicker.add(spectrumBar);
        audioTicker.add(volIcon);
        add(audioTicker, BorderLayout.EAST);

        // Equalizer animation timer — 50ms interval.
        eqTimer = new Timer(50, e -> animateEqualizer());
        eqTimer.start();
    }

    /** Called by LeagueHomeView when the track changes — refreshes the label. */
    public void refreshTrackDisplay() {
        trackTitle.setText(updateTrackLabel());
        volIcon.setText(engine.isMuted() ? "\uD83D\uDD07" : "\uD83D\uDD0A");
    }

    private String updateTrackLabel() {
        SoundtrackEngine.Track t = engine.getCurrentTrack();
        if (t == null) return "\u266B  No soundtrack";
        return t.getDisplayName();
    }

    /**
     * Animate the equalizer bars. When music is playing, bars respond to the
     * real synthesis amplitude from the engine. When paused/stopped, bars
     * decay to zero.
     */
    private void animateEqualizer() {
        boolean playing = engine.getState() == SoundtrackEngine.State.PLAYING && !engine.isMuted();
        float amp = engine.getAmplitude();
        for (int i = 0; i < 4; i++) {
            if (playing && amp > 0.01f) {
                // Mix real amplitude with random jitter for organic movement.
                float jitter = 0.3f + eqRng.nextFloat() * 0.7f;
                float target = Math.min(1f, amp * jitter * 1.5f);
                // Smooth interpolation toward target.
                barHeights[i] = barHeights[i] * 0.5f + target * 0.5f;
            } else {
                // Decay to zero.
                barHeights[i] *= 0.7f;
                if (barHeights[i] < 0.02f) barHeights[i] = 0f;
            }
        }
        spectrumBar.repaint();
    }

    /** Stop the equalizer timer (call on window close). */
    public void dispose() {
        if (eqTimer != null) eqTimer.stop();
    }

    private JPanel buildButtonChip(String letter, String label, Color btnColor) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        chip.setOpaque(false);

        JLabel btn = new JLabel(letter, JLabel.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
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
