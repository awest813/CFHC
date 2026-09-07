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
        table.getColumnModel().getColumn(0).setPreferredWidth(260);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setMaxWidth(80);
        StripedRowRenderer.install(table);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && row < set.size()) {
                        DataRecord dr = set.get(row);
                        String holder = dr.holder();
                        if (holder != null && !holder.isEmpty()) {
                            String[] parts = holder.split("\\(");
                            String playerName = parts[0].trim();
                            String teamAbbr = parts.length > 1 ? parts[1].replace(")", "").trim() : "";
                            positions.Player p = ctx.findPlayerInLeague(playerName, teamAbbr);
                            if (p != null) {
                                PlayerDetailView.show(ctx.parent(), p);
                            } else if (!teamAbbr.isEmpty()) {
                                simulation.Team t = ctx.teamMap().get(teamAbbr);
                                if (t == null) {
                                    for (simulation.Team candidate : ctx.league().getTeamList()) {
                                        if (candidate.getAbbr() != null && candidate.getAbbr().equalsIgnoreCase(teamAbbr)) {
                                            t = candidate;
                                            break;
                                        }
                                    }
                                }
                                if (t != null) {
                                    ctx.nav().openTeamDetail(t);
                                }
                            }
                        }
                    }
                }
            }
        });

        JScrollPane recordsScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(recordsScroll, table, "League records");
        panel.add(recordsScroll, BorderLayout.CENTER);

        javax.swing.JLabel hint = new javax.swing.JLabel("Double-click any record row to view details.");
        hint.setForeground(DesktopTheme.textSecondary());
        panel.add(hint, BorderLayout.SOUTH);

        return panel;
    }

    /** True while a record still holds the engine's unset placeholder. */
    private static boolean isUnset(DataRecord dr) {
        return dr.holder() == null || dr.holder().isEmpty()
                || "XXX%XXX".equals(dr.holder());
    }
}