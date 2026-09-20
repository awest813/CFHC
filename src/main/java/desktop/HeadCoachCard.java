package desktop;

import simulation.Team;
import staff.HeadCoach;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
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

        // Name + title row
        JPanel top = new JPanel(new GridLayout(0, 1, 0, 1));
        top.setOpaque(false);
        JLabel nameLabel = new JLabel(coachName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        nameLabel.setForeground(DesktopTheme.textPrimary());
        JLabel titleLabel = new JLabel(position);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        titleLabel.setForeground(DesktopTheme.textSecondary());
        top.add(nameLabel);
        top.add(titleLabel);

        // Big overall rating container
        JPanel ovrWrapper = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DesktopTheme.tableStripe());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        ovrWrapper.setPreferredSize(new Dimension(64, 60));
        ovrWrapper.setOpaque(false);
        ovrWrapper.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel ovr = new JLabel(hasCoach ? String.valueOf(hc.ratOvr) : "\u2014", JLabel.CENTER);
        ovr.setFont(new Font("SansSerif", Font.BOLD, 28));
        ovr.setForeground(DesktopTheme.successGreen());
        JLabel ovrCaption = new JLabel("OVR", JLabel.CENTER);
        ovrCaption.setFont(new Font("SansSerif", Font.BOLD, 8));
        ovrCaption.setForeground(DesktopTheme.textSecondary());
        ovrWrapper.add(ovr, BorderLayout.CENTER);
        ovrWrapper.add(ovrCaption, BorderLayout.SOUTH);

        // Rating sub-stats grid (off/def/talent/discipline) — 4 HUD metric tiles.
        JPanel stats = new JPanel(new GridLayout(2, 2, 5, 4));
        stats.setOpaque(false);
        if (hasCoach) {
            stats.add(buildStatTile("OFF", hc.ratOff));
            stats.add(buildStatTile("DEF", hc.ratDef));
            stats.add(buildStatTile("TALENT", hc.ratTalent));
            stats.add(buildStatTile("DISC", hc.ratDiscipline));
        } else {
            stats.add(buildStatTile("OFF", 0));
            stats.add(buildStatTile("DEF", 0));
            stats.add(buildStatTile("TALENT", 0));
            stats.add(buildStatTile("DISC", 0));
        }

        // Career record + contract info
        String record = hasCoach ? hc.getWins() + "-" + hc.getLosses() : "\u2014";
        String contract = hasCoach
                ? "Yr " + (hc.contractYear + 1) + " of " + hc.contractLength + "  \u2022  Age " + hc.age
                : "\u2014";

        JLabel recordLabel = new JLabel("Record: " + record);
        recordLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        recordLabel.setForeground(DesktopTheme.textPrimary());
        JLabel contractLabel = new JLabel(contract);
        contractLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        contractLabel.setForeground(DesktopTheme.textSecondary());

        JPanel footer = new JPanel(new GridLayout(0, 1, 0, 1));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(4, 0, 0, 0)));
        footer.add(recordLabel);
        footer.add(contractLabel);

        // Layout: name top, big OVR + sub-stats middle, record/contract bottom.
        JPanel body = new JPanel(new BorderLayout(8, 6));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(8, 0));
        middle.setOpaque(false);
        middle.add(ovrWrapper, BorderLayout.WEST);
        middle.add(stats, BorderLayout.CENTER);
        body.add(middle, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);

        content.add(body, BorderLayout.CENTER);
    }

    private JPanel buildStatTile(String label, int value) {
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
        tile.setBorder(BorderFactory.createEmptyBorder(2, 3, 2, 3));

        JLabel lbl = new JLabel(label, JLabel.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 8));
        lbl.setForeground(DesktopTheme.textSecondary());

        JLabel val = new JLabel(value > 0 ? String.valueOf(value) : "\u2014", JLabel.CENTER);
        val.setFont(new Font("SansSerif", Font.BOLD, 11));
        if (value >= 85) {
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
