package desktop;

import positions.Player;
import simulation.PlayerRecord;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class HallOfFamePanel implements LeagueScreen {

    @Override
    public String title() {
        return "Hall of Fame";
    }

    @Override
    public JPanel build(LeagueScreenContext ctx) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<PlayerRecord> hof = ctx.record().leagueHoF();
        if (hof == null || hof.isEmpty()) {
            JLabel empty = new JLabel("No players have been inducted into the Hall of Fame yet.",
                    JLabel.CENTER);
            empty.setForeground(DesktopTheme.textSecondary());
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        String[] columns = {"Name", "Position", "Team", "OVR"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return col == 3 ? Integer.class : String.class;
            }
        };
        for (PlayerRecord pr : hof) {
            model.addRow(new Object[]{pr.name(), pr.position(), pr.teamName(), pr.ratOvr()});
        }
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
                        Player player = ctx.findPlayerInLeague(name, teamName);
                        if (player != null) {
                            PlayerDetailView.show(ctx.parent(), player);
                        } else {
                            int modelRow = table.convertRowIndexToModel(row);
                            if (modelRow >= 0 && modelRow < hof.size()) {
                                PlayerRecord pr = hof.get(modelRow);
                                JOptionPane.showMessageDialog(ctx.parent(),
                                        DesktopTheme.messageForDialog(
                                                pr.name() + "  (" + pr.position() + ")\n"
                                                        + "Team: " + pr.teamName() + "\n"
                                                        + "Overall: " + pr.ratOvr()),
                                        "Hall of Fame - " + pr.name(),
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                }
            }
        });

        JScrollPane hofScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(hofScroll, table);
        panel.add(hofScroll, BorderLayout.CENTER);
        return panel;
    }
}