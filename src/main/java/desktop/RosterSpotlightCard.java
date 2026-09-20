package desktop;

import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Swing dashboard card component for ROSTER SPOTLIGHT.
 * Displays dual player cards (top offensive + top defensive player by OVR)
 * with pixel art portraits, OVR badges, archetype, and year. Binds to the
 * real user-team roster (was hardcoded "Mason Harrison / Jalen Bryant").
 */
public class RosterSpotlightCard extends CustomCardPanel {

    public RosterSpotlightCard(Team team) {
        this(team, null);
    }

    public RosterSpotlightCard(Team team, Consumer<positions.Player> onSelectPlayer) {
        super("Roster Spotlight");
        JPanel content = getContentArea();

        JPanel dualGrid = new JPanel(new GridLayout(1, 2, 8, 0));
        dualGrid.setOpaque(false);

        // Pick the top offensive (QB/RB/WR/TE) and top defensive (DL/LB/CB/S)
        // player by overall from the real roster.
        positions.Player offense = topOffensivePlayer(team);
        positions.Player defense = topDefensivePlayer(team);

        dualGrid.add(buildPlayerCard(offense, true, onSelectPlayer));
        dualGrid.add(buildPlayerCard(defense, false, onSelectPlayer));

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

    private JPanel buildPlayerCard(positions.Player player, boolean isOffense, Consumer<positions.Player> onSelectPlayer) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(11, 20, 34));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        boolean hasPlayer = player != null;
        if (hasPlayer && onSelectPlayer != null) {
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onSelectPlayer.accept(player);
                }
            });
        }

        String rawName = hasPlayer ? player.getName() : "—";
        String displayName = rawName;
        String initials = "??";
        if (hasPlayer && !rawName.isEmpty()) {
            String[] parts = rawName.trim().split("\\s+");
            if (parts.length >= 2 && parts[0].length() > 0 && parts[1].length() > 0) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
                if (rawName.length() > 12) {
                    displayName = parts[0].charAt(0) + ". " + parts[1];
                }
            } else {
                initials = rawName.substring(0, Math.min(2, rawName.length())).toUpperCase();
            }
        }

        String pos = hasPlayer && player.position != null ? player.position : "—";
        String year = hasPlayer ? player.getYrStr() : "";
        String archetype = hasPlayer ? player.getArchetypeDisplayName() : "";
        String ovr = hasPlayer ? String.valueOf(player.ratOvr) : "—";

        card.setToolTipText(hasPlayer
                ? (rawName + " (" + pos + ", " + year + ") • " + (archetype.isEmpty() ? "" : archetype + " • ") + ovr + " OVR"
                    + (onSelectPlayer != null ? " (Click to view details)" : ""))
                : "No player data");

        // Top Row: Role Pill (OFFENSE / DEFENSE) on Left + OVR on Right
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel roleBadge = new JLabel(isOffense ? "OFF" : "DEF");
        roleBadge.setFont(new Font("SansSerif", Font.BOLD, 9));
        roleBadge.setForeground(isOffense ? new Color(96, 165, 250) : new Color(248, 113, 113));

        JLabel ovrBadge = new JLabel(ovr + " OVR");
        ovrBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
        ovrBadge.setForeground(DesktopTheme.successGreen());

        topRow.add(roleBadge, BorderLayout.WEST);
        topRow.add(ovrBadge, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Center Area: Pixel Jersey Avatar + Name + Year/Pos
        JPanel centerArea = new JPanel(new BorderLayout(0, 4));
        centerArea.setOpaque(false);

        final String avatarInitials = initials;
        JPanel spriteBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DesktopTheme.tableStripe());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                // Jersey body
                g2.setColor(isOffense ? new Color(30, 64, 175) : new Color(153, 27, 27));
                g2.fillRoundRect(3, 16, getWidth() - 6, getHeight() - 17, 4, 4);

                // Face
                g2.setColor(isOffense ? new Color(243, 208, 168) : new Color(141, 85, 36));
                g2.fillOval(getWidth() / 2 - 7, 7, 14, 14);

                // Helmet
                g2.setColor(isOffense ? new Color(59, 130, 246) : new Color(239, 68, 68));
                g2.fillArc(getWidth() / 2 - 8, 5, 16, 14, 0, 180);

                // Jersey Initials
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(avatarInitials);
                g2.drawString(avatarInitials, (getWidth() - tw) / 2, getHeight() - 4);

                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
            }
        };
        spriteBox.setPreferredSize(new Dimension(38, 40));
        spriteBox.setOpaque(false);

        JPanel spriteCenter = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        spriteCenter.setOpaque(false);
        spriteCenter.add(spriteBox);
        centerArea.add(spriteCenter, BorderLayout.NORTH);

        JPanel meta = new JPanel(new GridLayout(2, 1, 0, 1));
        meta.setOpaque(false);

        JLabel nameLbl = new JLabel(displayName, JLabel.CENTER);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        nameLbl.setForeground(Color.WHITE);
        nameLbl.setToolTipText(rawName);

        JLabel subLbl = new JLabel(hasPlayer ? (pos + " \u2022 " + year) : "Empty", JLabel.CENTER);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 9));
        subLbl.setForeground(DesktopTheme.textSecondary());

        meta.add(nameLbl);
        meta.add(subLbl);
        centerArea.add(meta, BorderLayout.CENTER);
        card.add(centerArea, BorderLayout.CENTER);

        // Footer: Archetype pill
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        foot.setOpaque(false);

        String archText = archetype.isEmpty() ? (hasPlayer ? pos : "No data") : archetype;
        JLabel archLbl = new JLabel(archText, JLabel.CENTER);
        archLbl.setFont(new Font("SansSerif", Font.BOLD, 8));
        archLbl.setForeground(DesktopTheme.warningText());
        archLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)));

        foot.add(archLbl);
        card.add(foot, BorderLayout.SOUTH);

        return card;
    }
}
