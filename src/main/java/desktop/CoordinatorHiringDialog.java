package desktop;

import simulation.League;
import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;
import staff.DC;
import staff.OC;
import staff.Staff;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;

/**
 * Interactive dialog for hiring offensive and defensive coordinators.
 *
 * <p>Shown during the offseason when the user's team needs a new OC or DC
 * (contract expired, fired, or team change). The user picks from a list of
 * available coaches, or retains the current coordinator.
 */
public class CoordinatorHiringDialog extends JDialog {

    private static final Font BODY_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final int COORDINATOR_CONTRACT_LENGTH = 3;

    private final League league;
    private final Team userTeam;

    public CoordinatorHiringDialog(JFrame owner, League league) {
        super(owner, "Coordinator Hiring", true);
        this.league = league;
        this.userTeam = league.userTeam;
        setSize(750, 520);
        setLayout(new BorderLayout());

        if (userTeam == null || userTeam.getHeadCoach() == null) {
            add(new JLabel("No user team or head coach found.", JLabel.CENTER), BorderLayout.CENTER);
            JButton ok = new JButton("OK");
            ok.addActionListener(e -> dispose());
            JPanel bottom = new JPanel();
            bottom.add(ok);
            add(bottom, BorderLayout.SOUTH);
            return;
        }

        // Determine what needs hiring
        boolean needOC = userTeam.getOC() == null
                || userTeam.getOC().contractYear >= userTeam.getOC().contractLength;
        boolean needDC = userTeam.getDC() == null
                || userTeam.getDC().contractYear >= userTeam.getDC().contractLength;

        if (!needOC && !needDC) {
            buildNoHiringNeeded();
        } else if (needOC) {
            buildOCPanel(needDC);
        } else {
            buildDCPanel();
        }
    }

    private void buildNoHiringNeeded() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        StringBuilder sb = new StringBuilder("Your coaching staff is set for next season.\n\n");
        if (userTeam.getOC() != null) {
            sb.append("OC: ").append(userTeam.getOC().name)
              .append(" (Off ").append(userTeam.getOC().ratOff)
              .append(", Tal ").append(userTeam.getOC().ratTalent)
              .append(", Contract Yr ").append(userTeam.getOC().contractYear)
              .append("/").append(userTeam.getOC().contractLength).append(")\n");
        }
        if (userTeam.getDC() != null) {
            sb.append("DC: ").append(userTeam.getDC().name)
              .append(" (Def ").append(userTeam.getDC().ratDef)
              .append(", Tal ").append(userTeam.getDC().ratTalent)
              .append(", Contract Yr ").append(userTeam.getDC().contractYear)
              .append("/").append(userTeam.getDC().contractLength).append(")\n");
        }

        panel.add(new JLabel(sb.toString().replace("\n", "<br>").replaceFirst("^", "<html>") + "</html>"),
                BorderLayout.CENTER);
        JButton ok = new JButton("Continue");
        ok.addActionListener(e -> {
            league.coordinatorCarousel();
            dispose();
        });
        JPanel bottom = new JPanel();
        bottom.add(ok);
        panel.add(bottom, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private void buildOCPanel(boolean alsoNeedDC) {
        ArrayList<Staff> candidates = league.getOCList(userTeam.getHeadCoach());
        PlaybookOffense[] playbooks = userTeam.getPlaybookOff();

        String[] columns = {"", "Name", "Age", "Off", "Talent", "Playbook"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int startIdx = 0;
        if (userTeam.getOC() != null) {
            Staff current = userTeam.getOC();
            model.addRow(new Object[]{
                    "[Current]",
                    current.name,
                    current.age,
                    current.ratOff,
                    current.ratTalent,
                    playbooks[current.offStrat].getStratName()
            });
            startIdx = 1;
        }

        for (int i = startIdx; i < candidates.size(); i++) {
            Staff c = candidates.get(i);
            model.addRow(new Object[]{
                    "",
                    c.name,
                    c.age,
                    c.ratOff,
                    c.ratTalent,
                    playbooks[c.offStrat].getStratName()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(BODY_FONT);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);

        JLabel header = new JLabel("  Select an Offensive Coordinator:");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton hireBtn = new JButton("Hire Selected OC");
        hireBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a coordinator first.");
                return;
            }
            hireOC(candidates, row);
            dispose();
            if (alsoNeedDC || userTeam.getDC() == null
                    || userTeam.getDC().contractYear >= userTeam.getDC().contractLength) {
                showDCOnly(ownerFrame(), league);
            } else {
                league.coordinatorCarousel();
            }
        });
        buttons.add(hireBtn);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void buildDCPanel() {
        showDCContent(this);
    }

    private void showDCContent(JDialog container) {
        container.getContentPane().removeAll();
        container.setLayout(new BorderLayout());

        ArrayList<Staff> candidates = league.getDCList(userTeam.getHeadCoach());
        PlaybookDefense[] playbooks = userTeam.getPlaybookDef();

        String[] columns = {"", "Name", "Age", "Def", "Talent", "Playbook"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int startIdx = 0;
        if (userTeam.getDC() != null) {
            Staff current = userTeam.getDC();
            model.addRow(new Object[]{
                    "[Current]",
                    current.name,
                    current.age,
                    current.ratDef,
                    current.ratTalent,
                    playbooks[current.defStrat].getStratName()
            });
            startIdx = 1;
        }

        for (int i = startIdx; i < candidates.size(); i++) {
            Staff c = candidates.get(i);
            model.addRow(new Object[]{
                    "",
                    c.name,
                    c.age,
                    c.ratDef,
                    c.ratTalent,
                    playbooks[c.defStrat].getStratName()
            });
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(BODY_FONT);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);

        JLabel header = new JLabel("  Select a Defensive Coordinator:");
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton hireBtn = new JButton("Hire Selected DC");
        hireBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(container, "Select a coordinator first.");
                return;
            }
            hireDC(candidates, row);
            league.coordinatorCarousel();
            container.dispose();
        });
        buttons.add(hireBtn);

        container.add(header, BorderLayout.NORTH);
        container.add(new JScrollPane(table), BorderLayout.CENTER);
        container.add(buttons, BorderLayout.SOUTH);
        container.revalidate();
        container.repaint();
    }

    private void hireOC(ArrayList<Staff> candidates, int selectedIdx) {
        if (selectedIdx == 0 && userTeam.getOC() != null) {
            // Re-sign current OC
            userTeam.getOC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getOC().contractYear = 0;
            userTeam.getOC().baselinePrestige = 0;
        } else {
            Staff hired = candidates.get(selectedIdx);
            userTeam.setOC(new OC(hired, userTeam));
            league.getNewsHeadlines().add(userTeam.getName() + " adds new Off Coord " + userTeam.getOC().name);
            league.getNewsStories().get(league.currentWeek).add(
                    "Off Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, "
                            + userTeam.getName() + " has hired " + userTeam.getOC().name
                            + " to lead the offense.");
            userTeam.getOC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getOC().contractYear = 0;
            league.getCoachFreeAgents().remove(hired);
        }
    }

    private void hireDC(ArrayList<Staff> candidates, int selectedIdx) {
        if (selectedIdx == 0 && userTeam.getDC() != null) {
            // Re-sign current DC
            userTeam.getDC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getDC().contractYear = 0;
            userTeam.getDC().baselinePrestige = 0;
        } else {
            Staff hired = candidates.get(selectedIdx);
            userTeam.setDC(new DC(hired, userTeam));
            league.getNewsHeadlines().add(userTeam.getName() + " adds new Def Coord " + userTeam.getDC().name);
            league.getNewsStories().get(league.currentWeek).add(
                    "Def Coord Change: " + userTeam.getName()
                            + ">After an extensive search for a new coordinator, "
                            + userTeam.getName() + " has hired " + userTeam.getDC().name
                            + " to lead the defense.");
            userTeam.getDC().contractLength = COORDINATOR_CONTRACT_LENGTH;
            userTeam.getDC().contractYear = 0;
            league.getCoachFreeAgents().remove(hired);
        }
    }

    private JFrame ownerFrame() {
        return (JFrame) getOwner();
    }

    /**
     * Shows the coordinator hiring dialog and blocks until the user finishes.
     */
    public static void show(JFrame owner, League league) {
        CoordinatorHiringDialog dlg = new CoordinatorHiringDialog(owner, league);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    /**
     * Shows only the DC hiring step (used after OC is hired in the same flow).
     */
    private static void showDCOnly(JFrame owner, League league) {
        JDialog dcDialog = new JDialog(owner, "Defensive Coordinator Hiring", true);
        dcDialog.setSize(750, 520);
        CoordinatorHiringDialog helper = new CoordinatorHiringDialog(owner, league);
        helper.showDCContent(dcDialog);
        dcDialog.setLocationRelativeTo(owner);
        dcDialog.setVisible(true);
    }
}
