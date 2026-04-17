package desktop;

import simulation.LeagueRecord;
import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

/**
 * A graphical 'League Home' view for the desktop prototype.
 * Displays top teams, conference standings, and basic league info.
 */
public class LeagueHomeView extends JFrame {

    private final simulation.League leagueCore;
    private LeagueRecord currentRecord;

    public LeagueHomeView(simulation.League league) {
        this.leagueCore = league;
        this.currentRecord = league.toRecord();
        
        setTitle("CFB Coach - " + currentRecord.leagueName() + " (" + currentRecord.year() + ")");
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Header with Controls
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(33, 37, 41));
        header.setPreferredSize(new Dimension(1000, 70));
        
        JLabel title = new JLabel("  " + currentRecord.leagueName() + " Season " + currentRecord.year());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        JButton playWeekBtn = new JButton("Play Week " + (currentRecord.currentWeek() + 1));
        JButton advanceBtn = new JButton("Advance Season");
        
        playWeekBtn.addActionListener(e -> playWeek());
        advanceBtn.addActionListener(e -> advanceSeason());

        controls.add(playWeekBtn);
        controls.add(advanceBtn);
        header.add(controls, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        renderMainContent();
    }

    private void playWeek() {
        long start = System.currentTimeMillis();
        leagueCore.playWeek();
        long end = System.currentTimeMillis();
        System.out.println("Week Advancement: " + (end - start) + "ms");
        refresh();
    }

    private void advanceSeason() {
        long start = System.currentTimeMillis();
        // Simplified advancement for prototype
        for (int i = 0; i < 15; i++) {
            leagueCore.playWeek();
        }
        long end = System.currentTimeMillis();
        System.out.println("Full Season Simulation: " + (end - start) + "ms");
        refresh();
    }

    private void refresh() {
        this.currentRecord = leagueCore.toRecord();
        getContentPane().removeAll();
        // Redraw header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(33, 37, 41));
        header.setPreferredSize(new Dimension(1000, 70));
        JLabel title = new JLabel("  " + currentRecord.leagueName() + " Season " + currentRecord.year());
        title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);
        
        JPanel controls = new JPanel(); controls.setOpaque(false);
        JButton playWeekBtn = new JButton("Play Week " + (currentRecord.currentWeek() + 1));
        playWeekBtn.addActionListener(e -> playWeek());
        controls.add(playWeekBtn);
        header.add(controls, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        
        renderMainContent();
        revalidate();
        repaint();
    }

    private void renderMainContent() {
        // Main Content - Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);

        // Sidebar - Top 25
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
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                LeagueRecord.TeamRecord t = (LeagueRecord.TeamRecord) value;
                return super.getListCellRendererComponent(list, "#" + (index + 1) + " " + t.name() + " (" + t.prestige() + ")", index, isSelected, cellHasFocus);
            }
        });
        
        teamList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && teamList.getSelectedValue() != null) {
                RosterView.show(this, teamList.getSelectedValue());
            }
        });

        sidebar.add(new JScrollPane(teamList), BorderLayout.CENTER);
        splitPane.setLeftComponent(sidebar);

        // Center Content - Conference Overview
        JPanel content = new JPanel(new GridLayout(0, 2, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (LeagueRecord.ConferenceRecord conf : currentRecord.conferences()) {
            JPanel cPanel = new JPanel(new BorderLayout());
            cPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            JLabel cLabel = new JLabel(" " + conf.name());
            cLabel.setOpaque(true);
            cLabel.setBackground(new Color(52, 58, 64));
            cLabel.setForeground(Color.WHITE);
            cLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            cPanel.add(cLabel, BorderLayout.NORTH);

            DefaultListModel<LeagueRecord.TeamRecord> confTeamModel = new DefaultListModel<>();
            conf.teams().stream()
                    .sorted(Comparator.comparingInt(LeagueRecord.TeamRecord::prestige).reversed())
                    .forEach(confTeamModel::addElement);
            
            JList<LeagueRecord.TeamRecord> ctList = new JList<>(confTeamModel);
            ctList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && ctList.getSelectedValue() != null) {
                    RosterView.show(this, ctList.getSelectedValue());
                }
            });
            cPanel.add(new JScrollPane(ctList), BorderLayout.CENTER);
            content.add(cPanel);
        }

        splitPane.setRightComponent(new JScrollPane(content));
        add(splitPane, BorderLayout.CENTER);
    }

    public static void show(simulation.League league) {
        SwingUtilities.invokeLater(() -> {
            LeagueHomeView view = new LeagueHomeView(league);
            view.setLocationRelativeTo(null);
            view.setVisible(true);
        });
    }
}

