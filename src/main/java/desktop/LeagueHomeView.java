package desktop;

import simulation.Conference;
import simulation.DataRecord;
import simulation.GameFlowManager;
import simulation.League;
import simulation.LeagueLaunchCoordinator;
import simulation.LeagueRecord;
import simulation.PlatformLog;
import simulation.PlayerRecord;
import simulation.SeasonController;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Comparator;
import java.util.List;

/**
 * Graphical 'League Home' view for the desktop prototype. Displays the top teams,
 * conference standings, and basic league info, and allows the user to advance the
 * simulation one week or a full season at a time.
 *
 * <p>The view uses {@link SeasonController} for all week advancement so the full
 * season–offseason–new-season loop works correctly without any Android dependencies.
 */
public class LeagueHomeView extends JFrame {

    private static final String TAG = "LeagueHomeView";
    private static final Color HEADER_BG = new Color(33, 37, 41);
    private static final Color CONF_HEADER_BG = new Color(52, 58, 64);
    private static final Color STATUS_BG = new Color(240, 240, 240);
    private static final int HEADER_HEIGHT = 70;
    private static final String SAVE_EXTENSION = "cfb";

    /** No-op {@link GameFlowManager} — all transitions are handled in-process. */
    private static final GameFlowManager NO_OP_FLOW = new GameFlowManager() {
        @Override public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode p, String u) {}
        @Override public void loadGame(String s) {}
        @Override public void importSave(String u) {}
        @Override public void finishRecruiting(String r) {}
        @Override public void startRecruiting(String u) {}
        @Override public void showNotification(String t, String m) {}
        @Override public void returnToMainHub() {}
    };

    private final League leagueCore;
    private LeagueRecord currentRecord;
    private File lastSavePath;

    private DesktopUiBridge bridge;
    private SeasonController controller;

    private JLabel statusLabel;
    private JLabel playedIndicator;

    public LeagueHomeView(League league) {
        this(league, null);
    }

    public LeagueHomeView(League league, File loadedFrom) {
        this.leagueCore = league;
        this.currentRecord = league.toRecord();
        this.lastSavePath = loadedFrom;

        setTitle(buildWindowTitle());
        setSize(1100, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        bridge = new DesktopUiBridge(this, leagueCore);
        controller = new SeasonController(leagueCore, bridge, NO_OP_FLOW);

        setJMenuBar(buildMenuBar());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private String buildWindowTitle() {
        String suffix = lastSavePath != null ? " \u2014 " + lastSavePath.getName() : "";
        return "CFB Coach \u2014 " + currentRecord.leagueName() + " (" + currentRecord.year() + ")" + suffix;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveLeague(false));
        file.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveLeague(true));
        file.add(saveAsItem);

        file.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> dispatchEvent(new java.awt.event.WindowEvent(this,
                java.awt.event.WindowEvent.WINDOW_CLOSING)));
        file.add(exitItem);

        bar.add(file);

        JMenu season = new JMenu("Season");
        season.setMnemonic(KeyEvent.VK_S);

        JMenuItem playWeek = new JMenuItem("Play Next Week");
        playWeek.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        playWeek.addActionListener(e -> playWeek());
        season.add(playWeek);

        JMenuItem advance = new JMenuItem("Advance Full Season");
        advance.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
        advance.addActionListener(e -> advanceSeason());
        season.add(advance);

        bar.add(season);

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        help.add(aboutItem);
        bar.add(help);

        return bar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(getWidth(), HEADER_HEIGHT));

        JLabel title = new JLabel("  " + currentRecord.leagueName() + " \u2014 Season " + currentRecord.year());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setOpaque(false);

        JButton playWeekBtn = new JButton(playWeekLabel());
        playWeekBtn.setToolTipText("Simulate the next week (Space)");
        playWeekBtn.addActionListener(e -> playWeek());

        JButton advanceBtn = new JButton("Advance Season");
        advanceBtn.setToolTipText("Simulate through the rest of the regular season (Ctrl+A)");
        advanceBtn.addActionListener(e -> advanceSeason());

        JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save the current league (Ctrl+S)");
        saveBtn.addActionListener(e -> saveLeague(false));

        controls.add(playWeekBtn);
        controls.add(advanceBtn);
        controls.add(saveBtn);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private String playWeekLabel() {
        int week = leagueCore.currentWeek;
        int reg = leagueCore.regSeasonWeeks;
        if (week >= reg + 13) return "Recruiting…";
        if (week >= reg + 4)  return "Offseason: Step " + (week - reg - 3);
        if (week >= reg + 3)  return "Play National Championship";
        if (week >= reg + 2)  return "Play Semifinals";
        if (week >= reg + 1)  return "Play Quarterfinals / Bowl Week";
        if (week == reg)      return "Play Conf. Championships";
        if (week <= 0)        return "Begin Season";
        return "Play Week " + (week + 1);
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(STATUS_BG);
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel(buildStatusText());
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        status.add(statusLabel, BorderLayout.WEST);

        playedIndicator = new JLabel(lastSavePath != null
                ? "Save: " + lastSavePath.getName()
                : "Unsaved league");
        playedIndicator.setFont(new Font("SansSerif", Font.PLAIN, 12));
        playedIndicator.setForeground(Color.DARK_GRAY);
        status.add(playedIndicator, BorderLayout.EAST);

        return status;
    }

    private String buildStatusText() {
        int teams = currentRecord.conferences().stream().mapToInt(c -> c.teams().size()).sum();
        return String.format("Week %d  \u2022  %d conferences  \u2022  %d teams  \u2022  %d players in Hall of Fame",
                currentRecord.currentWeek(),
                currentRecord.conferences().size(),
                teams,
                currentRecord.leagueHoF().size());
    }

    private void playWeek() {
        long start = System.currentTimeMillis();
        bridge.clearNewSeasonPending();
        controller.advanceWeek();
        PlatformLog.i(TAG, "advanceWeek: " + (System.currentTimeMillis() - start) + "ms");

        if (bridge.isNewSeasonPending()) {
            startNewSeason();
        } else {
            refresh();
        }
    }

    /**
     * Called when recruiting has been auto-completed and the league should
     * transition to the next season.
     */
    private void startNewSeason() {
        bridge.clearNewSeasonPending();
        leagueCore.startNextSeason();
        bridge = new DesktopUiBridge(this, leagueCore);
        controller = new SeasonController(leagueCore, bridge, NO_OP_FLOW);
        refresh();
        JOptionPane.showMessageDialog(this,
                "Season " + leagueCore.getYear() + " is ready!\n"
                        + "All rosters have been filled via auto-recruiting.\n"
                        + "Press Space or click the Play button to begin.",
                "New Season", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Advances the regular season (weeks 1 through regSeasonWeeks+4) silently. */
    private void advanceSeason() {
        long start = System.currentTimeMillis();
        int played = 0;
        int target = leagueCore.regSeasonWeeks + 4;
        bridge.clearNewSeasonPending();
        while (leagueCore.currentWeek < target) {
            controller.advanceWeek();
            played++;
        }
        PlatformLog.i(TAG, "Advanced " + played + " weeks in "
                + (System.currentTimeMillis() - start) + "ms");
        refresh();
    }

    private void saveLeague(boolean forceChooser) {
        File target = lastSavePath;
        if (forceChooser || target == null) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save League");
            chooser.setFileFilter(new FileNameExtensionFilter("CFB Save (*." + SAVE_EXTENSION + ")", SAVE_EXTENSION));
            chooser.setSelectedFile(new File(suggestedFilename()));
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith("." + SAVE_EXTENSION)) {
                target = new File(target.getParentFile(), target.getName() + "." + SAVE_EXTENSION);
            }
            if (target.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                        "Overwrite existing file \"" + target.getName() + "\"?",
                        "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }

        boolean ok = leagueCore.saveLeague(target);
        if (ok) {
            lastSavePath = target;
            setTitle(buildWindowTitle());
            playedIndicator.setText("Saved: " + target.getName());
            PlatformLog.i(TAG, "League saved to " + target.getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to save league to:\n" + target.getAbsolutePath(),
                    "Save Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String suggestedFilename() {
        String base = currentRecord.leagueName().replaceAll("\\s+", "_");
        return base + "_" + currentRecord.year() + "." + SAVE_EXTENSION;
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "CFB Coach \u2014 Desktop Prototype\n"
                        + "Portable Java build of the CFHC simulation engine.\n\n"
                        + "Hotkeys:\n"
                        + "  Space\t\tPlay next week\n"
                        + "  Ctrl+A\tAdvance full season\n"
                        + "  Ctrl+S\tSave league\n"
                        + "  Ctrl+Shift+S\tSave As...\n"
                        + "  Ctrl+Q\tExit\n",
                "About CFB Coach",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void refresh() {
        this.currentRecord = leagueCore.toRecord();
        setTitle(buildWindowTitle());
        getContentPane().removeAll();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private JTabbedPane buildMainContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Standings",  buildStandingsPanel());
        tabs.addTab("Week " + leagueCore.currentWeek + " Scores", buildScoreboardPanel());
        tabs.addTab("Top 25",     buildTop25Panel());
        tabs.addTab("News",       buildNewsPanel());
        tabs.addTab("Hall of Fame", buildHallOfFamePanel());
        tabs.addTab("Records",    buildLeagueRecordsPanel());
        return tabs;
    }

    private JSplitPane buildStandingsPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setLeftComponent(buildTopTeamsSidebar());
        splitPane.setRightComponent(new JScrollPane(buildConferenceGrid()));
        return splitPane;
    }

    private JPanel buildTop25Panel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultListModel<LeagueRecord.TeamRecord> model = new DefaultListModel<>();
        currentRecord.conferences().stream()
                .flatMap(c -> c.teams().stream())
                .sorted(Comparator.comparingInt(LeagueRecord.TeamRecord::prestige).reversed())
                .limit(25)
                .forEach(model::addElement);

        JList<LeagueRecord.TeamRecord> list = new JList<>(model);
        list.setFont(new Font("SansSerif", Font.PLAIN, 14));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                LeagueRecord.TeamRecord t = (LeagueRecord.TeamRecord) value;
                String label = String.format("  #%-3d %-28s prestige %d    roster %d",
                        index + 1, t.name(), t.prestige(), t.roster().size());
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                openTeamDialog(list.getSelectedValue());
            }
        });
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildScoreboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea scores = new JTextArea();
        scores.setEditable(false);
        scores.setFont(new Font("Monospaced", Font.PLAIN, 13));
        scores.setText(buildScoresText());
        scores.setCaretPosition(0);

        panel.add(new JScrollPane(scores), BorderLayout.CENTER);
        return panel;
    }

    private String buildScoresText() {
        int week = leagueCore.currentWeek;
        if (week <= 0 || week >= leagueCore.getWeeklyScores().size()) {
            return "No games have been played yet. Press Space or click Play Week to simulate.";
        }
        List<String> lines = leagueCore.getWeeklyScores().get(week);
        if (lines == null || lines.isEmpty()) {
            return "No recorded games for week " + week + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Results for week ").append(week).append('\n');
        sb.append("=".repeat(Math.min(60, 20 + leagueCore.leagueName.length()))).append("\n\n");
        for (String line : lines) {
            int splitIdx = line.indexOf('>');
            if (splitIdx >= 0) {
                sb.append(line, 0, splitIdx).append('\n');
                sb.append("  ").append(line.substring(splitIdx + 1).replace("\n", "\n  ")).append('\n');
            } else {
                sb.append(line).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
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
                openTeamDialog(teamList.getSelectedValue());
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
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> jlist, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                LeagueRecord.TeamRecord t = (LeagueRecord.TeamRecord) value;
                Team live = findLiveTeam(t.name());
                String record = live != null ? " (" + live.wins + "-" + live.losses + ")" : "";
                String label2 = t.name() + record + "  \u2014  prestige " + t.prestige();
                return super.getListCellRendererComponent(jlist, label2, index, isSelected, cellHasFocus);
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) {
                openTeamDialog(list.getSelectedValue());
            }
        });
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        return panel;
    }

    private void openTeamDialog(LeagueRecord.TeamRecord team) {
        Team live = findLiveTeam(team.name());
        TeamDetailView.show(this, team, live);
    }

    private Team findLiveTeam(String teamName) {
        for (Conference c : leagueCore.getConferences()) {
            for (Team t : c.confTeams) {
                if (t.name.equals(teamName)) {
                    return t;
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // News panel
    // -------------------------------------------------------------------------

    private JPanel buildNewsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left: headline list
        DefaultListModel<String> headlineModel = new DefaultListModel<>();
        if (leagueCore.getNewsHeadlines() != null) {
            for (String h : leagueCore.getNewsHeadlines()) {
                headlineModel.addElement(h);
            }
        }
        if (headlineModel.isEmpty()) {
            headlineModel.addElement("No news this week.");
        }

        JList<String> headlineList = new JList<>(headlineModel);
        headlineList.setPreferredSize(new Dimension(300, 400));

        // Right: full story area
        JTextArea storyArea = new JTextArea("Select a headline to read the full story.");
        storyArea.setEditable(false);
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);

        // Clicking a headline shows the story from newsStories for the current week
        headlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = headlineList.getSelectedIndex();
                if (idx >= 0) {
                    String story = lookupStory(headlineList.getSelectedValue());
                    storyArea.setText(story != null ? story : headlineList.getSelectedValue());
                    storyArea.setCaretPosition(0);
                }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(headlineList), new JScrollPane(storyArea));
        split.setDividerLocation(280);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Returns the full story text for the given headline by scanning the
     * newsStories list for the current week.
     */
    private String lookupStory(String headline) {
        if (leagueCore.getNewsStories() == null || leagueCore.getNewsStories().isEmpty()) return null;
        int week = leagueCore.currentWeek;
        // newsStories has one list per season-week; search around the current week
        int maxWeek = Math.min(week + 1, leagueCore.getNewsStories().size() - 1);
        if (maxWeek < 0) return null;
        for (int w = maxWeek; w >= 0; w--) {
            for (String story : leagueCore.getNewsStories().get(w)) {
                String[] parts = story.split(">");
                if (parts.length >= 2 && headline.contains(parts[0])) {
                    return parts[1];
                }
                // Also check if the story title is contained in the headline
                if (parts.length >= 1 && headline.startsWith(parts[0])) {
                    return parts.length >= 2 ? parts[1] : parts[0];
                }
            }
        }
        // Fall back: show all stories for the current week
        if (week < leagueCore.getNewsStories().size() && !leagueCore.getNewsStories().get(week).isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String s : leagueCore.getNewsStories().get(week)) {
                sb.append(s.replace(">", "\n\n")).append("\n\n---\n\n");
            }
            return sb.toString();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Hall of Fame panel
    // -------------------------------------------------------------------------

    private JPanel buildHallOfFamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<PlayerRecord> hof = currentRecord.leagueHoF();
        if (hof == null || hof.isEmpty()) {
            JLabel empty = new JLabel("No players have been inducted into the Hall of Fame yet.",
                    JLabel.CENTER);
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        String[] columns = {"Name", "Position", "Team", "OVR"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (PlayerRecord pr : hof) {
            model.addRow(new Object[]{pr.name(), pr.position(), pr.teamName(), pr.ratOvr()});
        }
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel count = new JLabel(hof.size() + " inductees", JLabel.RIGHT);
        count.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        panel.add(count, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // League Records panel
    // -------------------------------------------------------------------------

    private JPanel buildLeagueRecordsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<simulation.DataRecord> records = currentRecord.leagueRecords();
        if (records == null || records.isEmpty()) {
            JLabel empty = new JLabel("No league records have been set yet — play a full season!",
                    JLabel.CENTER);
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        String[] columns = {"Record", "Value", "Holder", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (simulation.DataRecord dr : records) {
            if (dr != null) {
                model.addRow(new Object[]{dr.key(), formatValue(dr.value()), formatHolder(dr.holder()), dr.year()});
            }
        }
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private String formatValue(float value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.2f", value);
    }

    /**
     * Formats a {@link simulation.DataRecord} holder string for display.
     * Holders are stored as {@code "TeamName%PlayerName"} — this converts the
     * separator to an em-dash for readability.
     */
    static String formatHolder(String holder) {
        if (holder == null) return "";
        int pct = holder.indexOf('%');
        if (pct >= 0) {
            return holder.substring(0, pct).trim() + " \u2014 " + holder.substring(pct + 1).trim();
        }
        return holder;
    }

    // -------------------------------------------------------------------------
    // Static factory methods and snapshot viewer
    // -------------------------------------------------------------------------

    public static void show(League league) {
        show(league, null);
    }

    public static void show(League league, File loadedFrom) {
        SwingUtilities.invokeLater(() -> {
            LeagueHomeView view = new LeagueHomeView(league, loadedFrom);
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
