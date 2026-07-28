package desktop;

import positions.Player;
import simulation.League;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Interactive redshirt management dialog.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class RedshirtDialog extends JDialog {

    private static final String[] COLUMNS = {"Pos", "Name", "Yr", "OVR", "Team"};

    private final League league;
    private DefaultTableModel currentModel;
    private DefaultTableModel eligibleModel;
    private List<Player> currentList;
    private List<Player> eligibleList;

    public RedshirtDialog(JFrame owner, League league) {
        super(owner, "ROSTER MANAGEMENT — REDSHIRTS", true);
        this.league = league;
        setSize(1000, 650);
        setLayout(new BorderLayout());
        DesktopTheme.styleDialogContentPane(getContentPane());
        DesktopTheme.applyWindowIcon(this);

        buildContent();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        bottom.setBackground(DesktopTheme.tableBase());
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()));
        
        JButton doneBtn = DesktopTheme.createGlassButton("CLOSE MANAGEMENT", DesktopTheme.accentBlue());
        doneBtn.addActionListener(e -> dispose());
        bottom.add(doneBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void buildContent() {
        JPanel northStack = new JPanel(new BorderLayout());
        northStack.setOpaque(true);
        northStack.setBackground(DesktopTheme.tableBase());

        // Top Hint Bar
        JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(DesktopTheme.borderSubtle());
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        hintBar.setBackground(DesktopTheme.tableBase());
        JLabel hint = new JLabel("<html>Season redshirts (including auto-redshirts for players with fewer than 4 games) "
                + "are listed below when available. Use the pools to review or adjust eligibility.</html>");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(DesktopTheme.textSecondary());
        hintBar.add(hint);
        northStack.add(hintBar, BorderLayout.NORTH);

        if (league.userTeam != null && !league.userTeam.getRedshirtList().isEmpty()) {
            StringBuilder update = new StringBuilder();
            update.append("Players redshirted this season:\n\n");
            for (String row : league.userTeam.getRedshirtList()) {
                update.append(row).append('\n');
            }
            JTextArea seasonList = new JTextArea(update.toString());
            seasonList.setEditable(false);
            DesktopTheme.styleTextContent(seasonList);
            JScrollPane seasonScroll = new JScrollPane(seasonList);
            seasonScroll.setPreferredSize(new java.awt.Dimension(0, 110));
            seasonScroll.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(DesktopTheme.borderSubtle()),
                    league.getYear() + " Redshirts"));
            northStack.add(seasonScroll, BorderLayout.CENTER);
        }
        add(northStack, BorderLayout.NORTH);

        // Left — currently redshirted players
        currentModel = createModel();
        JTable currentTable = createModernTable(currentModel, "Inactive redshirt pool");
        StripedRowRenderer.install(currentTable);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
        
        JLabel leftHeader = new JLabel("INACTIVE REDSHIRT POOL");
        leftHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        leftHeader.setForeground(DesktopTheme.dangerRed());
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        
        JScrollPane currentScroll = new JScrollPane(currentTable);
        currentScroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));
        currentScroll.getViewport().setBackground(DesktopTheme.windowBackground());
        leftPanel.add(currentScroll, BorderLayout.CENTER);

        JButton removeBtn = DesktopTheme.createGlassButton("REMOVE STATUS \u25B6", DesktopTheme.tableBase());
        removeBtn.setForeground(DesktopTheme.dangerRed());
        removeBtn.addActionListener(e -> {
            int row = currentTable.getSelectedRow();
            if (row < 0 || row >= currentList.size()) {
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog("Select a redshirted player first."),
                        "Redshirts",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Player p = currentList.get(row);
            p.isRedshirt = false;
            refresh();
        });
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        leftBottom.setOpaque(false);
        leftBottom.add(removeBtn);
        leftPanel.add(leftBottom, BorderLayout.SOUTH);

        // Right — freshmen eligible for redshirt
        eligibleModel = createModel();
        JTable eligibleTable = createModernTable(eligibleModel, "Redshirt-eligible players");
        StripedRowRenderer.install(eligibleTable);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
        
        String teamName = (league.userTeam != null) ? league.userTeam.getAbbr() + " ELIGIBLE PROSPECTS" : "FRESHMEN ELIGIBLES";
        JLabel rightHeader = new JLabel(teamName.toUpperCase(Locale.ROOT));
        rightHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        rightHeader.setForeground(DesktopTheme.successGreen());
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        
        JScrollPane eligibleScroll = new JScrollPane(eligibleTable);
        eligibleScroll.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));
        eligibleScroll.getViewport().setBackground(DesktopTheme.windowBackground());
        rightPanel.add(eligibleScroll, BorderLayout.CENTER);

        JButton grantBtn = DesktopTheme.createGlassButton("\u25C0 GRANT REDSHIRT", DesktopTheme.successGreen());
        grantBtn.addActionListener(e -> {
            int row = eligibleTable.getSelectedRow();
            if (row < 0 || row >= eligibleList.size()) {
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog("Select an eligible freshman first."),
                        "Redshirts",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Player p = eligibleList.get(row);
            p.isRedshirt = true;
            refresh();
        });
        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rightBottom.setOpaque(false);
        rightBottom.add(grantBtn);
        rightPanel.add(rightBottom, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(500);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(5);
        add(split, BorderLayout.CENTER);

        populateTables();
    }

    private JTable createModernTable(DefaultTableModel model, String accessibleName) {
        return DesktopTheme.stylePickerTable(model, 35, 11, accessibleName);
    }

    private DefaultTableModel createModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 3 ? Integer.class : String.class;
            }
        };
    }

    private void populateTables() {
        // Collect all currently redshirted players across the league
        currentList = new ArrayList<>(league.getRedshirts());
        currentList.sort(Comparator.comparing((Player p) -> p.position)
                .thenComparing(p -> p.name));

        currentModel.setRowCount(0);
        for (Player p : currentList) {
            currentModel.addRow(playerRow(p));
        }

        // Collect eligible freshmen from the user's team
        eligibleList = new ArrayList<>();
        if (league.userTeam != null) {
            for (Player p : league.userTeam.getAllPlayers()) {
                if (isRedshirtEligible(p)) {
                    eligibleList.add(p);
                }
            }
            eligibleList.sort(Comparator.comparing((Player p) -> p.position)
                    .thenComparing(p -> p.name));
        }

        eligibleModel.setRowCount(0);
        for (Player p : eligibleList) {
            eligibleModel.addRow(playerRow(p));
        }
    }

    private void refresh() {
        populateTables();
    }

    private static Object[] playerRow(Player p) {
        String teamName = p.team != null ? p.team.getAbbr() : "";
        return new Object[]{p.position, p.name, DesktopTheme.yearAbbreviation(p.year), p.ratOvr, teamName};
    }

    private static boolean isRedshirtEligible(Player p) {
        return p.year == 1 && !p.isRedshirt && !p.wasRedshirt && !p.isMedicalRS;
    }

    public static void show(JFrame owner, League league) {
        RedshirtDialog dlg = new RedshirtDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
