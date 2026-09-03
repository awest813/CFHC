package desktop;

import simulation.DataRecord;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;

public class LeagueRecordsPanel implements LeagueScreen {

    @Override
    public String title() {
        return "Records";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(DesktopTheme.buildScreenHeader("League Records",
                "All-time single-season and career records across the universe."), BorderLayout.NORTH);

        // The engine seeds unset records with a "XXX%XXX" holder (and 1000
        // for lower-is-better categories) — showing those rows would be a
        // wall of placeholder data before any game is played.
        java.util.List<DataRecord> set = new java.util.ArrayList<>();
        for (DataRecord dr : ctx.record().leagueRecords()) {
            if (!isUnset(dr)) {
                set.add(dr);
            }
        }
        if (set.isEmpty()) {
            javax.swing.JLabel empty = new javax.swing.JLabel(
                    "No records yet — they're set as games are played.",
                    javax.swing.JLabel.CENTER);
            empty.setForeground(DesktopTheme.textSecondary());
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        String[] columns = {"Record", "Value", "Holder", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (DataRecord dr : set) {
            model.addRow(new Object[]{
                    dr.key(),
                    LeagueScreenContext.formatValue(dr.value()),
                    LeagueScreenContext.formatHolder(dr.holder()),
                    dr.year()
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(22);
        StripedRowRenderer.install(table);
        JScrollPane recordsScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(recordsScroll, table, "League records");
        panel.add(recordsScroll, BorderLayout.CENTER);
        return panel;
    }

    /** True while a record still holds the engine's unset placeholder. */
    private static boolean isUnset(DataRecord dr) {
        return dr.holder() == null || dr.holder().isEmpty()
                || "XXX%XXX".equals(dr.holder());
    }
}