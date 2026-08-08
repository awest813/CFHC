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
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Swing dashboard card component for ROSTER SPOTLIGHT.
 * Displays dual player cards with pixel art portraits, OVR badges, archetype traits, stats grid, and last game line.
 */
public class RosterSpotlightCard extends CustomCardPanel {

    public RosterSpotlightCard() {
        super("Roster Spotlight");
        JPanel content = getContentArea();

        JPanel dualGrid = new JPanel(new GridLayout(1, 2, 10, 0));
        dualGrid.setOpaque(false);

        // Player 1: QB Mason Harrison
        dualGrid.add(buildPlayerCard(
                "7", "MASON HARRISON", "QB", "Junior \u2022 6'3\" \u2022 205 lbs",
                "Field General", "88",
                new String[]{"CMP%", "YDS", "TD", "INT", "QBR"},
                new String[]{"67.1", "1,912", "17", "4", "84.3"},
                "LAST GAME: 22/31, 287 YDS, 3 TD", true
        ));

        // Player 2: LB Jalen Bryant
        dualGrid.add(buildPlayerCard(
                "32", "JALEN BRYANT", "LB", "Senior \u2022 6'1\" \u2022 228 lbs",
                "Run Stopper", "84",
                new String[]{"TCK", "TFL", "SACK", "INT", "FF"},
                new String[]{"58", "8.0", "3.5", "1", "2"},
                "LAST GAME: 9 TKL, 1.5 TFL, 1 FF", false
        ));

        content.add(dualGrid, BorderLayout.CENTER);
    }

    private JPanel buildPlayerCard(String number, String name, String pos, String bio, String archetype, String ovr, String[] statLabels, String[] statVals, String lastGame, boolean isQb) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(6, 12, 20));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Header Row: Sprite Avatar + Meta + OVR Badge
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        // Sprite Avatar Container
        JPanel spriteBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(13, 23, 38));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Pixel sprite face drawing
                g2.setColor(isQb ? new Color(243, 208, 168) : new Color(141, 85, 36));
                g2.fillRect(14, 8, 16, 14);
                g2.setColor(new Color(27, 77, 62));
                g2.fillRect(10, 6, 24, 8);
                g2.fillRect(6, 22, 32, 26);
                g2.setColor(DesktopTheme.warningText());
                g2.fillRect(14, 24, 16, 24);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(number, 18, 42);

                g2.dispose();
            }
        };
        spriteBox.setPreferredSize(new Dimension(44, 50));
        spriteBox.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1));

        JPanel meta = new JPanel(new GridLayout(4, 1, 0, 1));
        meta.setOpaque(false);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        nameLbl.setForeground(Color.WHITE);

        JLabel bioLbl = new JLabel(bio);
        bioLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));
        bioLbl.setForeground(DesktopTheme.textSecondary());

        JLabel archLbl = new JLabel(archetype);
        archLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        archLbl.setForeground(DesktopTheme.textSecondary());

        JLabel moraleLbl = new JLabel("MORALE  \uD83D\uDE04 High");
        moraleLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        moraleLbl.setForeground(DesktopTheme.successGreen());

        meta.add(nameLbl);
        meta.add(bioLbl);
        meta.add(archLbl);
        meta.add(moraleLbl);

        JPanel ovrBadge = new JPanel(new GridLayout(2, 1, 0, 0));
        ovrBadge.setOpaque(false);
        JLabel oTitle = new JLabel("OVR", JLabel.CENTER);
        oTitle.setFont(new Font("SansSerif", Font.BOLD, 8));
        oTitle.setForeground(DesktopTheme.textSecondary());
        JLabel oVal = new JLabel(ovr, JLabel.CENTER);
        oVal.setFont(new Font("SansSerif", Font.BOLD, 18));
        oVal.setForeground(DesktopTheme.successGreen());
        ovrBadge.add(oTitle);
        ovrBadge.add(oVal);

        headerRow.add(spriteBox, BorderLayout.WEST);
        headerRow.add(meta, BorderLayout.CENTER);
        headerRow.add(ovrBadge, BorderLayout.EAST);

        card.add(headerRow, BorderLayout.NORTH);

        // Stats Grid Row
        JPanel statsRow = new JPanel(new GridLayout(1, statLabels.length, 2, 0));
        statsRow.setOpaque(true);
        statsRow.setBackground(new Color(17, 28, 46));
        statsRow.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        for (int i = 0; i < statLabels.length; i++) {
            JPanel cell = new JPanel(new GridLayout(2, 1, 0, 1));
            cell.setOpaque(false);
            JLabel sl = new JLabel(statLabels[i], JLabel.CENTER);
            sl.setFont(new Font("SansSerif", Font.BOLD, 8));
            sl.setForeground(DesktopTheme.textSecondary());
            JLabel sv = new JLabel(statVals[i], JLabel.CENTER);
            sv.setFont(new Font("Monospaced", Font.BOLD, 10));
            sv.setForeground(Color.WHITE);
            cell.add(sl);
            cell.add(sv);
            statsRow.add(cell);
        }

        card.add(statsRow, BorderLayout.CENTER);

        // Footer Last Game Line
        JLabel lastLbl = new JLabel(lastGame, JLabel.CENTER);
        lastLbl.setFont(new Font("SansSerif", Font.PLAIN, 8));
        lastLbl.setForeground(DesktopTheme.textSecondary());
        lastLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(3, 0, 0, 0)));

        card.add(lastLbl, BorderLayout.SOUTH);
        return card;
    }
}
