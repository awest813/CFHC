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

        String[] columns = {"Record", "Value", "Holder", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (DataRecord dr : ctx.record().leagueRecords()) {
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
        DesktopTheme.styleDataTableInScroll(recordsScroll, table);
        panel.add(recordsScroll, BorderLayout.CENTER);
        return panel;
    }
}