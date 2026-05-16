package desktop;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public class CoachDatabasePanel implements LeagueScreen {

    private static final String[] COACH_DATABASE_CATEGORIES = {
            "National Champs", "Conf. Champs", "Bowl Wins", "Total Wins", "Win %",
            "Coach of the Year", "Conf COTY", "All-Americans", "All-Conference",
            "Career Score", "Career Prestige"
    };

    @Override
    public String title() {
        return "Coaches";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> categoryBox = new JComboBox<>(COACH_DATABASE_CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Coach", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);

        Runnable loadCoaches = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                java.util.List<String> rankings = ctx.league().getCoachDatabase(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                simulation.PlatformLog.e("CoachDatabasePanel", "Error loading coach database", ex);
            }
        };

        categoryBox.addActionListener(e -> loadCoaches.run());
        loadCoaches.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Sort by: "));
        topBar.add(categoryBox);
        DesktopTheme.styleToolbar(topBar);
        panel.add(topBar, BorderLayout.NORTH);
        JScrollPane coachDbScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(coachDbScroll, table);
        panel.add(coachDbScroll, BorderLayout.CENTER);
        return panel;
    }
}