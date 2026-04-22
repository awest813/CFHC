package desktop;

import positions.Player;
import simulation.League;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Interactive redshirt management dialog.
 * Polished with 'Industrial Glass' aesthetic.
 */
public class RedshirtDialog extends JDialog {

    private static final String[] COLUMNS = {"Pos", "Name", "Yr", "OVR", "Team"};
    
    private static final Color BG_COLOR = new Color(15, 20, 28);
    private static final Color SURFACE_COLOR = new Color(25, 32, 45);
    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color DANGER_RED = new Color(231, 76, 60);
    private static final Color TEXT_SECONDARY = new Color(171, 178, 191);

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
        getContentPane().setBackground(BG_COLOR);

        buildContent();

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        bottom.setBackground(SURFACE_COLOR);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 20)));
        
        JButton doneBtn = createGlassButton("CLOSE MANAGEMENT", ACCENT_BLUE);
        doneBtn.addActionListener(e -> dispose());
        bottom.add(doneBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void buildContent() {
        // Top Hint Bar
        JPanel hintBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        hintBar.setBackground(SURFACE_COLOR);
        JLabel hint = new JLabel("TACTICAL NOTE: Redshirts preserve eligibility for players with minimal game participation.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(TEXT_SECONDARY);
        hintBar.add(hint);
        add(hintBar, BorderLayout.NORTH);

        // Left — currently redshirted players
        currentModel = createModel();
        JTable currentTable = createModernTable(currentModel);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
        
        JLabel leftHeader = new JLabel("INACTIVE REDSHIRT POOL");
        leftHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        leftHeader.setForeground(DANGER_RED);
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        
        JScrollPane currentScroll = new JScrollPane(currentTable);
        currentScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 10)));
        currentScroll.getViewport().setBackground(BG_COLOR);
        leftPanel.add(currentScroll, BorderLayout.CENTER);

        JButton removeBtn = createGlassButton("REMOVE STATUS \u25B6", SURFACE_COLOR);
        removeBtn.setForeground(DANGER_RED);
        removeBtn.addActionListener(e -> {
            int row = currentTable.getSelectedRow();
            if (row < 0 || row >= currentList.size()) return;
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
        JTable eligibleTable = createModernTable(eligibleModel);

        JPanel rightPanel = new JPanel(new BorderLayout(0, 15));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));
        
        String teamName = (league.userTeam != null) ? league.userTeam.getAbbr() + " ELIGIBLE PROSPECTS" : "FRESHMEN ELIGIBLES";
        JLabel rightHeader = new JLabel(teamName.toUpperCase());
        rightHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        rightHeader.setForeground(SUCCESS_GREEN);
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        
        JScrollPane eligibleScroll = new JScrollPane(eligibleTable);
        eligibleScroll.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 10)));
        eligibleScroll.getViewport().setBackground(BG_COLOR);
        rightPanel.add(eligibleScroll, BorderLayout.CENTER);

        JButton grantBtn = createGlassButton("\u25C0 GRANT REDSHIRT", SUCCESS_GREEN);
        grantBtn.addActionListener(e -> {
            int row = eligibleTable.getSelectedRow();
            if (row < 0 || row >= eligibleList.size()) return;
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

    private JTable createModernTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(BG_COLOR);
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(255, 255, 255, 10));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(ACCENT_BLUE);
        
        table.getTableHeader().setBackground(SURFACE_COLOR);
        table.getTableHeader().setForeground(TEXT_SECONDARY);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 40));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 10)));
        
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
        btn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        return btn;
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
        return new Object[]{p.position, p.name, yearLabel(p.year), p.ratOvr, teamName};
    }

    private static boolean isRedshirtEligible(Player p) {
        return p.year == 1 && !p.isRedshirt && !p.wasRedshirt && !p.isMedicalRS;
    }

    private static String yearLabel(int year) {
        return switch (year) {
            case 0 -> "RS";
            case 1 -> "FR";
            case 2 -> "SO";
            case 3 -> "JR";
            case 4 -> "SR";
            case 5 -> "5SR";
            default -> String.valueOf(year);
        };
    }

    public static void show(JFrame owner, League league) {
        RedshirtDialog dlg = new RedshirtDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }
}
