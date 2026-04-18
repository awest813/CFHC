package desktop;

import simulation.LeagueRecord;
import simulation.PlayerRecord;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.util.Comparator;

/**
 * A detailed roster view for the desktop prototype.
 * Displays a sortable table of players for a specific team.
 */
public class RosterView extends JDialog {

    private static final String[] COLUMNS = {"Pos", "Name", "Yr", "OVR", "Pot", "IQ"};

    public RosterView(JFrame owner, LeagueRecord.TeamRecord team) {
        super(owner, team.name() + " Roster", true);
        setSize(820, 520);
        setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 3, 4, 5 -> Integer.class;
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        team.roster().stream()
                .sorted(Comparator.comparingInt(PlayerRecord::ratOvr).reversed())
                .forEach(p -> model.addRow(new Object[]{
                        p.position(),
                        p.name(),
                        yearLabel(p.year()),
                        p.ratOvr(),
                        p.ratPot(),
                        p.ratIntelligence()
                }));

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        footer.add(new JLabel(String.format(
                "HC: %s (Ovr %d)   OC: %s (Ovr %d)   DC: %s (Ovr %d)",
                team.headCoach().name(), team.headCoach().ratOvr(),
                team.offenseCoach().name(), team.offenseCoach().ratOvr(),
                team.defenseCoach().name(), team.defenseCoach().ratOvr())));
        add(footer, BorderLayout.SOUTH);
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

    public static void show(JFrame owner, LeagueRecord.TeamRecord team) {
        RosterView view = new RosterView(owner, team);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);
    }
}
