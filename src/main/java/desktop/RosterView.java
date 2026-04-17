package desktop;

import simulation.LeagueRecord;
import simulation.PlayerRecord;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * A detailed roster view for the desktop prototype.
 * Displays a sortable table of players for a specific team.
 */
public class RosterView extends JDialog {

    public RosterView(JFrame owner, LeagueRecord.TeamRecord team) {
        super(owner, team.name() + " Roster", true);
        setSize(800, 500);
        setLayout(new BorderLayout());

        String[] columns = {"Pos", "Name", "Yr", "OVR", "Pot", "Intelligence"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        
        for (PlayerRecord p : team.roster()) {
            model.addRow(new Object[]{
                p.position(),
                p.name(),
                p.year(),
                p.ratOvr(),
                p.ratPot(),
                p.ratIntelligence()
            });
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.add(new JLabel("Coach: " + team.headCoach().name() + " (Ovr: " + team.headCoach().ratOvr() + ")"));
        add(footer, BorderLayout.SOUTH);
    }

    public static void show(JFrame owner, LeagueRecord.TeamRecord team) {
        RosterView view = new RosterView(owner, team);
        view.setLocationRelativeTo(owner);
        view.setVisible(true);
    }
}
