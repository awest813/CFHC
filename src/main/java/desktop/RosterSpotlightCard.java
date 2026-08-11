package desktop;

import simulation.Team;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Swing dashboard card component for ROSTER SPOTLIGHT.
 * Displays dual player cards (top offensive + top defensive player by OVR)
 * with pixel art portraits, OVR badges, archetype, and year. Binds to the
 * real user-team roster (was hardcoded "Mason Harrison / Jalen Bryant").
 */
public class RosterSpotlightCard extends CustomCardPanel {

    public RosterSpotlightCard(Team team) {
        super("Roster Spotlight");
        JPanel content = getContentArea();

        JPanel dualGrid = new JPanel(new GridLayout(1, 2, 10, 0));
        dualGrid.setOpaque(false);

        // Pick the top offensive (QB/RB/WR/TE) and top defensive (DL/LB/CB/S)
        // player by overall from the real roster.
        positions.Player offense = topOffensivePlayer(team);
        positions.Player defense = topDefensivePlayer(team);

        dualGrid.add(buildPlayerCard(offense, true));
        dualGrid.add(buildPlayerCard(defense, false));

        content.add(dualGrid, BorderLayout.CENTER);
    }

    private static positions.Player topOffensivePlayer(Team team) {
        if (team == null) return null;
        List<positions.Player> candidates = new ArrayList<>();
        addAll(candidates, team.getTeamQBs());
        addAll(candidates, team.getTeamRBs());
        addAll(candidates, team.getTeamWRs());
        addAll(candidates, team.getTeamTEs());
        return highestOvr(candidates);
    }

    private static positions.Player topDefensivePlayer(Team team) {
        if (team == null) return null;
        List<positions.Player> candidates = new ArrayList<>();
        addAll(candidates, team.getTeamDLs());
        addAll(candidates, team.getTeamLBs());
        addAll(candidates, team.getTeamCBs());
        addAll(candidates, team.getTeamSs());
        return highestOvr(candidates);
    }

    private static void addAll(List<positions.Player> dest, List<? extends positions.Player> src) {
        if (src != null) dest.addAll(src);
    }

    private static positions.Player highestOvr(List<positions.Player> players) {
        return players.stream()
                .filter(p -> p != null)
                .max(Comparator.comparingInt(p -> p.ratOvr))
                .orElse(null);
    }

    private JPanel buildPlayerCard(positions.Player player, boolean isOffense) {
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

        boolean hasPlayer = player != null;
        String name = hasPlayer ? player.getName().toUpperCase() : "\u2014";
        String pos = hasPlayer && player.position != null ? player.position : "\u2014";
        String year = hasPlayer ? player.getYrStr() : "";
        String bio = hasPlayer ? (year + " \u2022 " + pos) : "No player";
        String archetype = hasPlayer ? player.getArchetypeDisplayName() : "";
        String ovr = hasPlayer ? String.valueOf(player.ratOvr) : "\u2014";
        final String initials = hasPlayer && name.length() >= 2 ? name.substring(0, 2) : "??";

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
                g2.setColor(isOffense ? new Color(243, 208, 168) : new Color(141, 85, 36));
                g2.fillRect(14, 8, 16, 14);
                g2.setColor(new Color(27, 77, 62));
                g2.fillRect(10, 6, 24, 8);
                g2.fillRect(6, 22, 32, 26);
                g2.setColor(DesktopTheme.warningText());
                g2.fillRect(14, 24, 16, 24);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(initials, 12, 42);

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

        JLabel archLbl = new JLabel(archetype.isEmpty() ? pos : archetype);
        archLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        archLbl.setForeground(DesktopTheme.textSecondary());

        JLabel moraleLbl = new JLabel(hasPlayer ? ("OVR " + ovr + "  \u2022  " + pos) : "Roster empty");
        moraleLbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        moraleLbl.setForeground(DesktopTheme.successGreen());

        meta.add(nameLbl);
        meta.add(bioLbl);
        meta.add(archLbl);
        meta.add(moraleLbl);

        headerRow.add(spriteBox, BorderLayout.WEST);
        headerRow.add(meta, BorderLayout.CENTER);

        // OVR badge
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
        headerRow.add(ovrBadge, BorderLayout.EAST);

        card.add(headerRow, BorderLayout.NORTH);

        // Footer: position summary line (stat grids vary too much by position
        // to hardcode columns; the header now carries the real OVR + archetype).
        JLabel footLbl = new JLabel(hasPlayer
                ? (pos + " \u2022 " + year + (archetype.isEmpty() ? "" : " \u2022 " + archetype))
                : "No roster data", JLabel.CENTER);
        footLbl.setFont(new Font("SansSerif", Font.PLAIN, 8));
        footLbl.setForeground(DesktopTheme.textSecondary());
        footLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(3, 0, 0, 0)));
        card.add(footLbl, BorderLayout.SOUTH);

        return card;
    }
}
