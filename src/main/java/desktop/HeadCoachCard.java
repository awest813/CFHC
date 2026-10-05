package desktop;

import simulation.Team;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Swing dashboard card: HEAD COACH profile. Binds to real coach data
 * (name, career record, overall + off/def/talent/discipline ratings,
 * contract year, age). Fills the 12th dashboard grid cell and replaces
 * static demo content with live state.
 */
public class HeadCoachCard extends CustomCardPanel {

    public HeadCoachCard(Team team) {
        this(team, null);
    }

    public HeadCoachCard(Team team, Runnable onOpenCoach) {
        super("Head Coach");
        JPanel content = getContentArea();

        if (onOpenCoach != null) {
            JPanel headerRight = new JPanel();
            headerRight.setOpaque(false);
            JLabel viewCoach = new JLabel("PROFILE \u25B8");
            viewCoach.setFont(new Font("SansSerif", Font.BOLD, 9));
            viewCoach.setForeground(DesktopTheme.successGreen());
            viewCoach.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            viewCoach.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onOpenCoach.run();
                }
            });
            headerRight.add(viewCoach);
            if (getHeaderBar() != null) {
                getHeaderBar().add(headerRight, BorderLayout.EAST);
            }

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to view My Coach profile");
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onOpenCoach.run();
                }
            });
        }

        HeadCoach hc = team != null ? team.getHeadCoach() : null;
        boolean hasCoach = hc != null;

        String coachName = hasCoach ? hc.name : "Vacant";
        String position = hasCoach && hc.position != null ? hc.position : "Head Coach";

        // Name row: coach name left, position caption right.
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);
        JLabel nameLabel = new JLabel(coachName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        nameLabel.setForeground(DesktopTheme.textPrimary());
        JLabel titleLabel = new JLabel(position.toUpperCase());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        titleLabel.setForeground(DesktopTheme.textSecondary());
        top.add(nameLabel, BorderLayout.CENTER);
        top.add(titleLabel, BorderLayout.EAST);

        // Ratings: one compact row of five HUD tiles (OVR + off/def/talent/discipline).
        // The previous 64px OVR block + 2x2 grid needed ~90px of height but the
        // dashboard row only leaves ~40px here, so every tile was cut in half.
        JPanel stats = new JPanel(new GridLayout(1, 5, 4, 0));
        stats.setOpaque(false);
        stats.add(buildStatTile("OVR", hasCoach ? hc.ratOvr : 0, true));
        stats.add(buildStatTile("OFF", hasCoach ? hc.ratOff : 0, false));
        stats.add(buildStatTile("DEF", hasCoach ? hc.ratDef : 0, false));
        stats.add(buildStatTile("TALENT", hasCoach ? hc.ratTalent : 0, false));
        stats.add(buildStatTile("DISC", hasCoach ? hc.ratDiscipline : 0, false));

        // Career record + contract info on a single footer line.
        String record = hasCoach ? hc.getWins() + "-" + hc.getLosses() : "\u2014";
        String contract = hasCoach
                ? "Yr " + (hc.contractYear + 1) + " of " + hc.contractLength + "  \u2022  Age " + hc.age
                : "\u2014";

        JPanel recordBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        recordBox.setOpaque(false);
        JLabel recordCaption = new JLabel("RECORD");
        recordCaption.setFont(new Font("SansSerif", Font.BOLD, 9));
        recordCaption.setForeground(DesktopTheme.textSecondary());
        JLabel recordLabel = new JLabel(record);
        recordLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
        recordLabel.setForeground(DesktopTheme.textPrimary());
        recordBox.add(recordCaption);
        recordBox.add(recordLabel);

        JLabel contractLabel = new JLabel(contract, JLabel.RIGHT);
        contractLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        contractLabel.setForeground(DesktopTheme.textSecondary());

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(5, 0, 0, 0)));
        footer.add(recordBox, BorderLayout.WEST);
        footer.add(contractLabel, BorderLayout.EAST);

        // Layout: name top, rating tiles middle (kept at preferred height), record bottom.
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.add(stats, BorderLayout.NORTH);
        body.add(middle, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);

        content.add(body, BorderLayout.CENTER);
    }

    private JPanel buildStatTile(String label, int value, boolean primary) {
        JPanel tile = new JPanel(new BorderLayout(0, 1)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DesktopTheme.tableStripe());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tile.setOpaque(false);
        tile.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

        JLabel lbl = new JLabel(label, JLabel.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 8));
        lbl.setForeground(DesktopTheme.textSecondary());

        JLabel val = new JLabel(value > 0 ? String.valueOf(value) : "\u2014", JLabel.CENTER);
        val.setFont(new Font("Monospaced", Font.BOLD, 14));
        if (primary && value > 0) {
            val.setForeground(DesktopTheme.successGreen());
        } else if (value >= 85) {
            val.setForeground(DesktopTheme.successGreen());
        } else if (value >= 70) {
            val.setForeground(DesktopTheme.warningText());
        } else {
            val.setForeground(DesktopTheme.textSecondary());
        }

        tile.add(lbl, BorderLayout.NORTH);
        tile.add(val, BorderLayout.CENTER);
        return tile;
    }
}
