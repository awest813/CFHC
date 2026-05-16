package desktop;

import simulation.PlatformLog;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class LeagueHistoryPanel implements LeagueScreen {

    private static final String TAG = "LeagueHistoryPanel";

    private static final String[] CATEGORIES = {
            "National Championships", "Conference Championships", "Bowl Wins",
            "Total Wins", "Hall of Fame Players"
    };

    @Override
    public String title() {
        return "League History";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyArea.setLineWrap(false);
        String historyText = ctx.league().getLeagueHistoryStr();
        if (historyText == null || historyText.trim().isEmpty()) {
            historyText = "No season history yet - play at least one full season.";
        } else {
            historyText = historyText.replace("%", "");
        }
        historyArea.setText(historyText);
        historyArea.setCaretPosition(0);
        DesktopTheme.styleTextContent(historyArea);
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(DesktopTheme.titledBorder("Season Champions"));
        historyScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        historyScroll.setOpaque(true);
        historyScroll.setPreferredSize(new Dimension(0, 200));

        JPanel statsPanel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(statsPanel);
        statsPanel.setBorder(DesktopTheme.titledBorder("All-Time Team Records"));

        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Team", "Total"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String teamName = String.valueOf(table.getValueAt(row, 1));
                        Team t = ctx.teamMap().get(teamName);
                        if (t != null) ctx.nav().openTeamDetail(t);
                    }
                }
            }
        });

        Runnable loadHistoryStats = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                ArrayList<String> rankings = ctx.league().getLeagueHistoryStats(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading league history stats", ex);
            }
        };

        categoryBox.addActionListener(e -> loadHistoryStats.run());
        loadHistoryStats.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Category: "));
        topBar.add(categoryBox);
        DesktopTheme.styleToolbar(topBar);
        statsPanel.add(topBar, BorderLayout.NORTH);
        JScrollPane histStatsScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(histStatsScroll, table);
        statsPanel.add(histStatsScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, historyScroll, statsPanel);
        split.setDividerLocation(220);
        split.setOpaque(true);
        split.setBackground(DesktopTheme.windowBackground());
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }
}