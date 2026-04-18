package desktop;

import simulation.LeagueRecord;
import simulation.PlatformLog;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Comparator;

/**
 * Graphical 'League Home' view for the desktop prototype. Displays the top teams,
 * conference standings, and basic league info, and allows the user to advance the
 * simulation one week or a full season at a time.
 */
public class LeagueHomeView extends JFrame {

    private static final String TAG = "LeagueHomeView";
    private static final Color HEADER_BG = new Color(33, 37, 41);
    private static final Color CONF_HEADER_BG = new Color(52, 58, 64);
    private static final int HEADER_HEIGHT = 70;
    private static final int REGULAR_SEASON_WEEKS = 15;

    private final simulation.League leagueCore;
    private LeagueRecord currentRecord;

    public LeagueHomeView(simulation.League league) {
        this.leagueCore = league;
        this.currentRecord = league.toRecord();

        setTitle("CFB Coach - " + currentRecord.leagueName() + " (" + currentRecord.year() + ")");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(getWidth(), HEADER_HEIGHT));

        JLabel title = new JLabel("  " + currentRecord.leagueName() + " Season " + currentRecord.year());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setOpaque(false);

        JButton playWeekBtn = new JButton("Play Week " + (currentRecord.currentWeek() + 1));
        playWeekBtn.addActionListener(e -> playWeek());

        JButton advanceBtn = new JButton("Advance Season");
        advanceBtn.addActionListener(e -> advanceSeason());

        controls.add(playWeekBtn);
        controls.add(advanceBtn);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private void playWeek() {
        long start = System.currentTimeMillis();
        leagueCore.playWeek();
        PlatformLog.i(TAG, "Week advancement: " + (System.currentTimeMillis() - start) + "ms");
        refresh();
    }

    private void advanceSeason() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < REGULAR_SEASON_WEEKS; i++) {
            leagueCore.playWeek();
        }
        PlatformLog.i(TAG, "Full season simulation: " + (System.currentTimeMillis() - start) + "ms");
        refresh();
    }

    private void refresh() {
        this.currentRecord = leagueCore.toRecord();
        setTitle("CFB Coach - " + currentRecord.leagueName() + " (" + currentRecord.year() + ")");
        getContentPane().removeAll();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JSplitPane buildMainContent() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);

        splitPane.setLeftComponent(buildTopTeamsSidebar());
        splitPane.setRightComponent(new JScrollPane(buildConferenceGrid()));

        return splitPane;
    }

    private JPanel buildTopTeamsSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBorder(BorderFactory.createTitledBorder("Top Teams (Prestige)"));

        DefaultListModel<LeagueRecord.TeamRecord> teamModel = new DefaultListModel<>();
        currentRecord.conferences().stream()
                .flatMap(c -> c.teams().stream())
                .sorted(Comparator.comparingInt(LeagueRecord.TeamRecord::prestige).reversed())
                .limit(25)
                .forEach(teamModel::addElement);

        JList<LeagueRecord.TeamRecord> teamList = new JList<>(teamModel);
        teamList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                LeagueRecord.TeamRecord t = (LeagueRecord.TeamRecord) value;
                String label = "#" + (index + 1) + " " + t.name() + " (" + t.prestige() + ")";
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        teamList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && teamList.getSelectedValue() != null) {
                RosterView.show(this, teamList.getSelectedValue());
            }
        });

        sidebar.add(new JScrollPane(teamList), BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildConferenceGrid() {
        JPanel content = new JPanel(new GridLayout(0, 2, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (LeagueRecord.ConferenceRecord conf : currentRecord.conferences()) {
            content.add(buildConferencePanel(conf));
        }
        return content;
    }

    private JPanel buildConferencePanel(LeagueRecord.ConferenceRecord conf) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel label = new JLabel(" " + conf.name());
        label.setOpaque(true);
        label.setBackground(CONF_HEADER_BG);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(label, BorderLayout.NORTH);

        DefaultListModel<LeagueRecord.TeamRecord> model = new DefaultListModel<>();
        conf.teams().stream()
                .sorted(Comparator.comparingInt(LeagueRecord.TeamRecord::prestige).reversed())
                .forEach(model::addElement);

        JList<LeagueRecord.TeamRecord> list = new JList<>(model);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                RosterView.show(this, list.getSelectedValue());
            }
        });
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    public static void show(simulation.League league) {
        SwingUtilities.invokeLater(() -> {
            LeagueHomeView view = new LeagueHomeView(league);
            view.setLocationRelativeTo(null);
            view.setVisible(true);
        });
    }

    /**
     * Opens a read-only snapshot viewer for an already-loaded LeagueRecord.
     * Useful when inspecting a save without wiring up the live simulation.
     */
    public static void showSnapshot(LeagueRecord record) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog((JFrame) null, "CFB Coach - " + record.leagueName()
                    + " (" + record.year() + ") [read-only]", true);
            dialog.setSize(900, 600);
            dialog.setLayout(new BorderLayout());

            DefaultListModel<LeagueRecord.TeamRecord> model = new DefaultListModel<>();
            record.conferences().stream()
                    .flatMap(c -> c.teams().stream())
                    .sorted(Comparator.comparingInt(LeagueRecord.TeamRecord::prestige).reversed())
                    .forEach(model::addElement);
            JList<LeagueRecord.TeamRecord> list = new JList<>(model);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    LeagueRecord.TeamRecord t = (LeagueRecord.TeamRecord) value;
                    String label = "#" + (index + 1) + " " + t.name() + " (prestige " + t.prestige() + ")";
                    return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                }
            });
            dialog.add(new JScrollPane(list), BorderLayout.CENTER);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }
}
