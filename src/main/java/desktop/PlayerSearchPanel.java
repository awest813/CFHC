package desktop;

import positions.Player;
import simulation.League;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.Map;

public class PlayerSearchPanel implements LeagueScreen {

    private JComponent searchTarget;

    @Override
    public String title() {
        return "Player Search";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(buildScreenHeader("Player Search", "Find players across every roster by name, position, and class."), BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.add(new JLabel("Name:"));
        JTextField nameField = new JTextField(12);
        nameField.setToolTipText("Type any part of a player's name. (Ctrl+F)");
        filterBar.add(nameField);
        searchTarget = nameField;

        filterBar.add(new JLabel("Position:"));
        String[] positions = {"ALL", "QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"};
        JComboBox<String> posBox = new JComboBox<>(positions);
        filterBar.add(posBox);

        filterBar.add(new JLabel("Year:"));
        String[] years = {"ALL", "FR", "SO", "JR", "SR"};
        JComboBox<String> yearBox = new JComboBox<>(years);
        filterBar.add(yearBox);

        JButton searchBtn = new JButton("Search");
        filterBar.add(searchBtn);
        JLabel resultCount = new JLabel();
        resultCount.setForeground(DesktopTheme.textSecondary());
        filterBar.add(resultCount);
        DesktopTheme.styleToolbar(filterBar);

        topPanel.add(filterBar, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Name", "Pos", "Team", "Year", "OVR", "Pot"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4 || col == 5) return Integer.class;
                return String.class;
            }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) table.getValueAt(row, 0);
                        String teamName = (String) table.getValueAt(row, 2);
                        Team t = ctx.teamMap().get(teamName);
                        if (t != null) {
                            Player p = findPlayerByName(t, name);
                            if (p != null) PlayerDetailView.show(ctx.parent(), p);
                        }
                    }
                }
            }
        });

        JScrollPane searchScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(searchScroll, table);
        panel.add(searchScroll, BorderLayout.CENTER);

        Runnable runSearch = () -> {
            String query = nameField.getText().toLowerCase(Locale.ROOT).trim();
            String posFilter = (String) posBox.getSelectedItem();
            String yearFilter = (String) yearBox.getSelectedItem();
            int yearInt = -1;
            if ("FR".equals(yearFilter)) yearInt = 1;
            else if ("SO".equals(yearFilter)) yearInt = 2;
            else if ("JR".equals(yearFilter)) yearInt = 3;
            else if ("SR".equals(yearFilter)) yearInt = 4;

            model.setRowCount(0);
            for (Team t : ctx.league().getTeamList()) {
                for (Player p : t.getAllPlayers()) {
                    if (!query.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(query)) continue;
                    if (!"ALL".equals(posFilter) && !p.position.equals(posFilter)) continue;
                    if (yearInt != -1 && p.year != yearInt) continue;
                    model.addRow(new Object[]{
                            p.name, p.position, t.getName(), formatYear(p.year), p.ratOvr, p.ratPot
                    });
                }
            }
            resultCount.setText(model.getRowCount() + " players");
        };

        searchBtn.addActionListener(e -> runSearch.run());
        nameField.addActionListener(e -> runSearch.run());
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { runSearch.run(); }
            @Override public void removeUpdate(DocumentEvent e) { runSearch.run(); }
            @Override public void changedUpdate(DocumentEvent e) { runSearch.run(); }
        });
        posBox.addActionListener(e -> runSearch.run());
        yearBox.addActionListener(e -> runSearch.run());
        runSearch.run();

        return panel;
    }

    @Override
    public JComponent searchTarget() {
        return searchTarget;
    }

    private static JPanel buildScreenHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLabel.setForeground(DesktopTheme.textPrimary());
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitleLabel.setForeground(DesktopTheme.textSecondary());
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(subtitleLabel, BorderLayout.SOUTH);
        return header;
    }

    private static String formatYear(int year) {
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

    private static Player findPlayerByName(Team team, String name) {
        if (team == null || name == null) return null;
        for (Player p : team.getAllPlayers()) {
            if (name.equals(p.name)) return p;
        }
        return null;
    }
}