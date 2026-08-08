package desktop;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;

/**
 * Left persistent vertical navigation sidebar component for {@link LeagueHomeView}.
 * Renders 15 navigation options with active neon green selection indicators, badge counters, and seal emblem.
 */
public class DesktopNavSidebar extends JPanel {

    public static final String[] NAV_TITLES = {
            "Dashboard", "Team Management", "Roster", "Depth Chart",
            "Game Plan", "Recruiting", "Scouting", "Training",
            "Schedule", "Stats & History", "Conference", "Facilities",
            "Finances", "Program Prestige", "Settings"
    };

    private static final String[] NAV_ICONS = {
            "\u2302", "\u2666", "\u2630", "\u25A0", "\u2605",
            "\u2191", "\u2261", "\u2637", "\u2318", "\u2609",
            "\u263C", "\u265A", "\u2606", "\u25C9", "\u2699"
    };

    private final JList<String> navList;
    private final Consumer<String> onSelectScreen;

    public DesktopNavSidebar(Consumer<String> onSelectScreen) {
        super(new BorderLayout());
        this.onSelectScreen = onSelectScreen;
        setOpaque(false);
        setPreferredSize(new Dimension(210, 800));

        DefaultListModel<String> model = new DefaultListModel<>();
        for (String title : NAV_TITLES) {
            model.addElement(title);
        }

        navList = new JList<>(model);
        navList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navList.setSelectedIndex(0);
        navList.setOpaque(false);
        navList.setBackground(new Color(9, 17, 28)); // #09111C
        navList.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        navList.setCellRenderer(new NavItemRenderer());

        navList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = navList.getSelectedValue();
                if (selected != null && onSelectScreen != null) {
                    onSelectScreen.accept(selected);
                }
            }
        });

        add(navList, BorderLayout.CENTER);

        // Bottom Seal Emblem Panel
        JPanel sealFooter = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(9, 17, 28));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(DesktopTheme.borderSubtle());
                g2.drawLine(0, 0, getWidth(), 0);

                // Seal circle graphic
                int cx = getWidth() / 2;
                int cy = 35;
                g2.setColor(new Color(9, 20, 16));
                g2.fillOval(cx - 24, cy - 24, 48, 48);
                g2.setColor(DesktopTheme.warningText());
                g2.drawOval(cx - 24, cy - 24, 48, 48);

                g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                g2.drawString("EST 1898", cx - 20, cy + 4);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        sealFooter.setOpaque(false);
        sealFooter.setPreferredSize(new Dimension(210, 80));
        sealFooter.setBorder(BorderFactory.createEmptyBorder(55, 10, 8, 10));

        JLabel sealText = new JLabel("PINE VALLEY STATE", JLabel.CENTER);
        sealText.setFont(new Font("SansSerif", Font.BOLD, 10));
        sealText.setForeground(DesktopTheme.textSecondary());
        sealFooter.add(sealText, BorderLayout.SOUTH);

        add(sealFooter, BorderLayout.SOUTH);
    }

    public void setSelectedScreen(String title) {
        if (title == null) return;
        for (int i = 0; i < NAV_TITLES.length; i++) {
            if (NAV_TITLES[i].equalsIgnoreCase(title) || (NAV_TITLES[i].equals("Dashboard") && title.equals("Home"))) {
                navList.setSelectedIndex(i);
                break;
            }
        }
    }

    public JList<String> getNavList() {
        return navList;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(9, 17, 28)); // #09111C Obsidian Sidebar
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(DesktopTheme.borderSubtle());
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        g2.dispose();
        super.paintComponent(g);
    }

    private static class NavItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String title = value != null ? value.toString() : "";
            String icon = (index >= 0 && index < NAV_ICONS.length) ? NAV_ICONS[index] : "\u25B6";

            String badgeText = "Recruiting".equalsIgnoreCase(title) ? "  [14]" : "";
            JLabel label = (JLabel) super.getListCellRendererComponent(list, "  " + icon + "  " + title.toUpperCase() + badgeText, index, isSelected, cellHasFocus);

            label.setFont(new Font("SansSerif", Font.BOLD, 11));
            label.setPreferredSize(new Dimension(200, 36));
            label.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(new Color(0, 230, 118, 40)); // Neon green glow fill
                label.setForeground(Color.WHITE);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, DesktopTheme.successGreen()),
                        BorderFactory.createEmptyBorder(4, 10, 4, 14)));
            } else {
                label.setOpaque(false);
                label.setForeground(DesktopTheme.textSecondary());
            }

            return label;
        }
    }
}
