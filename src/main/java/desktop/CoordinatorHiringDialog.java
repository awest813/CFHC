package desktop;

import simulation.League;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;
import staff.DC;
import staff.OC;
import staff.Staff;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Interactive dialog for hiring offensive and defensive coordinators.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class CoordinatorHiringDialog extends JDialog {

    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final int COORDINATOR_CONTRACT_LENGTH = 3;

    private final League league;
    private final Team userTeam;

    public CoordinatorHiringDialog(JFrame owner, League league) {
        super(owner, "STAFF ACQUISITION — COORDINATORS", true);
        this.league = league;
        this.userTeam = league.userTeam;
        setSize(900, 600);
        setLayout(new BorderLayout());
        getContentPane().setBackground(DesktopTheme.windowBackground());

        if (userTeam == null || userTeam.getHeadCoach() == null) {
            buildErrorPanel("No active user team or head coach record found.");
            return;
        }

        boolean needOC = userTeam.getOC() == null
                || userTeam.getOC().contractYear >= userTeam.getOC().contractLength;
        boolean needDC = userTeam.getDC() == null
                || userTeam.getDC().contractYear >= userTeam.getDC().contractLength;

        if (!needOC && !needDC) {
            buildNoHiringNeeded();
        } else if (needOC) {
            buildOCPanel(needDC);
        } else {
            buildDCPanel();
        }
    }

    private void buildErrorPanel(String msg) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        panel.add(new JLabel("<html><center style='color:#E74C3C; font-size:14pt;'>"
                + DesktopTheme.escapeForHtml(msg) + "</center></html>", JLabel.CENTER), BorderLayout.CENTER);
        JButton ok = createGlassButton("CLOSE", ACCENT_BLUE);
        ok.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void buildNoHiringNeeded() {
        JPanel panel = new JPanel(new BorderLayout(25, 25));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel header = new JLabel("COACHING STAFF STABILITY");
        header.setFont(new Font("SansSerif", Font.BOLD, 22));
        header.setForeground(ACCENT_BLUE);
        panel.add(header, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel(new java.awt.GridLayout(1, 2, 25, 0));
        cardsPanel.setOpaque(false);

        if (userTeam.getOC() != null) {
            cardsPanel.add(createStaffStabilityCard("OFFENSIVE COORDINATOR", userTeam.getOC(), new Color(52, 152, 219)));
        }
        if (userTeam.getDC() != null) {
            cardsPanel.add(createStaffStabilityCard("DEFENSIVE COORDINATOR", userTeam.getDC(), new Color(231, 76, 60)));
        }

        panel.add(cardsPanel, BorderLayout.CENTER);

        JButton ok = createGlassButton("PROCEED TO SEASON", ACCENT_BLUE);
        ok.addActionListener(e -> {
            league.coordinatorCarousel();
            dispose();
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private JPanel createStaffStabilityCard(String role, Staff s, Color accent) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(DesktopTheme.tableBase());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DesktopTheme.borderSubtle(), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel(role);
        title.setFont(new Font("SansSerif", Font.BOLD, 10));
        title.setForeground(accent);
        card.add(title, BorderLayout.NORTH);

        String stats = String.format(Locale.ROOT,
            "<html><body style='color:" + DesktopTheme.cssRgb(DesktopTheme.textSecondary()) + "; font-family:SansSerif; font-size:11pt; line-height:1.6;'>" +
            "<b style='color:" + DesktopTheme.cssRgb(DesktopTheme.textPrimary()) + "; font-size:14pt;'>%s</b><br/><br/>" +
            "AGE: <b style='color:" + DesktopTheme.cssRgb(DesktopTheme.textPrimary()) + "'>%d</b><br/>" +
            "RATING: <b style='color:" + DesktopTheme.cssRgb(DesktopTheme.textPrimary()) + "'>%d</b><br/>" +
            "TALENT: <b style='color:" + DesktopTheme.cssRgb(DesktopTheme.textPrimary()) + "'>%d</b><br/>" +
            "TENURE: YEAR %d OF %d<br/>" +
            "</body></html>",
            DesktopTheme.escapeForHtml(s.name.toUpperCase(Locale.ROOT)), s.age, (s instanceof OC ? s.ratOff : s.ratDef), s.ratTalent,
            s.contractYear, s.contractLength
        );
        card.add(new JLabel(stats), BorderLayout.CENTER);

        return card;
    }

    private void buildOCPanel(boolean alsoNeedDC) {
        ArrayList<Staff> candidates = league.getOCList(userTeam.getHeadCoach());
        PlaybookOffense[] playbooks = userTeam.getPlaybookOff();

        String[] columns = {"", "NAME", "AGE", "OFF", "TALENT", "PLAYBOOK"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 2 || col == 3 || col == 4) return Integer.class;
                return String.class;
            }
        };

        if (userTeam.getOC() != null) {
            Staff current = userTeam.getOC();
            model.addRow(new Object[]{"[CURRENT]", current.name.toUpperCase(Locale.ROOT), current.age, current.ratOff, current.ratTalent, playbooks[current.offStrat].getStratName().toUpperCase(Locale.ROOT)});
        }

        for (int i = (userTeam.getOC() != null ? 1 : 0); i < candidates.size(); i++) {
            Staff c = candidates.get(i);
            model.addRow(new Object[]{"", c.name.toUpperCase(Locale.ROOT), c.age, c.ratOff, c.ratTalent, playbooks[c.offStrat].getStratName().toUpperCase(Locale.ROOT)});
        }

        JTable table = createModernTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);

        JPanel headerPanel = createHeaderPanel("HIRE OFFENSIVE COORDINATOR");
        
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 20));
        buttons.setBackground(DesktopTheme.tableBase());
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()));
        
        JButton hireBtn = createGlassButton("CONFIRM HIRE", ACCENT_BLUE);
        hireBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog("Select a coordinator from the registry first."),
                        "Coordinator Hiring",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            hireOC(candidates, row);
            dispose();
            if (alsoNeedDC || userTeam.getDC() == null || userTeam.getDC().contractYear >= userTeam.getDC().contractLength) {
                showDCOnly(ownerFrame(), league);
            } else {
                league.coordinatorCarousel();
            }
        });
        buttons.add(hireBtn);

        add(headerPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(DesktopTheme.windowBackground());
        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void buildDCPanel() {
        showDCContent(this);
    }

    private void showDCContent(JDialog container) {
        container.getContentPane().removeAll();
        container.setLayout(new BorderLayout());
        container.getContentPane().setBackground(DesktopTheme.windowBackground());

        ArrayList<Staff> candidates = league.getDCList(userTeam.getHeadCoach());
        PlaybookDefense[] playbooks = userTeam.getPlaybookDef();

        String[] columns = {"", "NAME", "AGE", "DEF", "TALENT", "PLAYBOOK"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 2 || col == 3 || col == 4) return Integer.class;
                return String.class;
            }
        };

        if (userTeam.getDC() != null) {
            Staff current = userTeam.getDC();
            model.addRow(new Object[]{"[CURRENT]", current.name.toUpperCase(Locale.ROOT), current.age, current.ratDef, current.ratTalent, playbooks[current.defStrat].getStratName().toUpperCase(Locale.ROOT)});
        }

        for (int i = (userTeam.getDC() != null ? 1 : 0); i < candidates.size(); i++) {
            Staff c = candidates.get(i);
            model.addRow(new Object[]{"", c.name.toUpperCase(Locale.ROOT), c.age, c.ratDef, c.ratTalent, playbooks[c.defStrat].getStratName().toUpperCase(Locale.ROOT)});
        }

        JTable table = createModernTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);

        JPanel headerPanel = createHeaderPanel("HIRE DEFENSIVE COORDINATOR");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 25, 20));
        buttons.setBackground(DesktopTheme.tableBase());
        buttons.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()));

        JButton hireBtn = createGlassButton("CONFIRM HIRE", ACCENT_BLUE);
        hireBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(container,
                        DesktopTheme.messageForDialog("Select a coordinator from the registry first."),
                        "Coordinator Hiring",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            hireDC(candidates, row);
            league.coordinatorCarousel();
            container.dispose();
        });
        buttons.add(hireBtn);

        container.add(headerPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(DesktopTheme.windowBackground());
        container.add(scroll, BorderLayout.CENTER);
        container.add(buttons, BorderLayout.SOUTH);
        container.revalidate();
        container.repaint();
    }

    private JPanel createHeaderPanel(String title) {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DesktopTheme.borderSubtle());
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        headerPanel.setBackground(DesktopTheme.tableBase());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        JLabel header = new JLabel(title);
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setForeground(DesktopTheme.textPrimary());
        headerPanel.add(header, BorderLayout.WEST);
        return headerPanel;
    }

    private JTable createModernTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(40);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(DesktopTheme.windowBackground());
        table.setForeground(DesktopTheme.textPrimary());
        table.setGridColor(DesktopTheme.borderSubtle());
        table.setShowVerticalLines(false);
        table.setSelectionBackground(ACCENT_BLUE);
        
        table.getTableHeader().setBackground(DesktopTheme.tableBase());
        table.getTableHeader().setForeground(DesktopTheme.textSecondary());
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        table.getTableHeader().setPreferredSize(new Dimension(0, 45));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesktopTheme.borderSubtle()));
        
        return table;
    }

    private JButton createGlassButton(String text, Color bg) {
        JButton btn = new JButton(text) {
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
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        return btn;
    }

    private void hireOC(ArrayList<Staff> candidates, int selectedIdx) {
        if (selectedIdx == 0 && userTeam.getOC() != null) {
            userTeam.getOC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getOC().contractYear = 0;
            userTeam.getOC().baselinePrestige = 0;
        } else {
            Staff hired = candidates.get(selectedIdx);
            userTeam.setOC(new OC(hired, userTeam));
            league.getNewsHeadlines().add(userTeam.getName() + " adds new Off Coord " + userTeam.getOC().name);
            while (league.getNewsStories().size() <= league.currentWeek) {
                league.getNewsStories().add(new java.util.ArrayList<>());
            }
            league.getNewsStories().get(league.currentWeek).add(
                    "Off Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, "
                            + userTeam.getName() + " has hired " + userTeam.getOC().name
                            + " to lead the offense.");
            userTeam.getOC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getOC().contractYear = 0;
            league.getCoachFreeAgents().remove(hired);
        }
    }

    private void hireDC(ArrayList<Staff> candidates, int selectedIdx) {
        if (selectedIdx == 0 && userTeam.getDC() != null) {
            userTeam.getDC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getDC().contractYear = 0;
            userTeam.getDC().baselinePrestige = 0;
        } else {
            Staff hired = candidates.get(selectedIdx);
            userTeam.setDC(new DC(hired, userTeam));
            league.getNewsHeadlines().add(userTeam.getName() + " adds new Def Coord " + userTeam.getDC().name);
            while (league.getNewsStories().size() <= league.currentWeek) {
                league.getNewsStories().add(new java.util.ArrayList<>());
            }
            league.getNewsStories().get(league.currentWeek).add(
                    "Def Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, "
                            + userTeam.getName() + " has hired " + userTeam.getDC().name
                            + " to lead the defense.");
            userTeam.getDC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getDC().contractYear = 0;
            league.getCoachFreeAgents().remove(hired);
        }
    }

    private JFrame ownerFrame() {
        return (JFrame) getOwner();
    }

    public static void show(JFrame owner, League league) {
        CoordinatorHiringDialog dlg = new CoordinatorHiringDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    private static void showDCOnly(JFrame owner, League league) {
        JDialog dcDialog = new JDialog(owner, "STAFF ACQUISITION — DEFENSIVE COORDINATOR", true);
        dcDialog.setSize(900, 600);
        CoordinatorHiringDialog helper = new CoordinatorHiringDialog(owner, league);
        helper.showDCContent(dcDialog);
        dcDialog.setLocationRelativeTo(owner);
        dcDialog.setVisible(true);
    }
}
