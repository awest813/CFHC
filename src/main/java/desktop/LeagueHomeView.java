package desktop;

import positions.Player;
import simulation.Conference;
import simulation.DataRecord;
import simulation.Game;
import simulation.GameFlowManager;
import simulation.League;
import simulation.LeagueExportController;
import simulation.LeagueLaunchCoordinator;
import simulation.LeagueRecord;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.PlayerRecord;
import simulation.SeasonController;
import simulation.Team;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graphical 'League Home' view for the desktop prototype. Displays standings,
 * poll rankings, team/player statistics, scoreboard with week navigation, news,
 * hall of fame, league records, and coach database.
 *
 * <p>The view uses {@link SeasonController} for all week advancement so the full
 * season–offseason–new-season loop works correctly without any Android dependencies.
 */
public class LeagueHomeView extends JFrame {

    private static final String TAG = "LeagueHomeView";
    private static final Color HEADER_BG = new Color(33, 37, 41);
    private static final Color CONF_HEADER_BG = new Color(52, 58, 64);
    private static final Color USER_TEAM_BG = new Color(25, 60, 100);
    private static final Color STATUS_BG = new Color(240, 240, 240);
    private static final int HEADER_HEIGHT = 80;
    private static final String SAVE_EXTENSION = "cfb";
    private static final DecimalFormat DF2 = new DecimalFormat("#.##");

    /** Delimiter used by the engine to separate headline from story body in news strings. */
    private static final String NEWS_STORY_DELIMITER = ">";

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

    /** Team ranking category names matching League.getTeamRankingsStr(selection). */
    private static final String[] TEAM_RANKING_CATEGORIES = {
            "Poll Score", "Prestige", "RPI", "Strength of Schedule", "Strength of Wins",
            "Points/Game", "Opp Points/Game", "Yards/Game", "Opp Yards/Game",
            "Pass Yards/Game", "Rush Yards/Game", "Opp Pass YPG", "Opp Rush YPG",
            "Turnover Diff", "Off. Talent", "Def. Talent", "Chemistry",
            "Recruiting Class", "Discipline", "Budget", "Facilities",
            "Coach Overall", "Coach Score"
    };

    /** Hint shown below the player rankings table. */
    private static final String PLAYER_RANKINGS_HINT = "Statistics update after each simulated week.";

    /** Player ranking category names matching League.getPlayerRankStr(selection). */
    private static final String[] PLAYER_RANKING_CATEGORIES = {
            "QB Pass Rating", "QB Pass Yards", "QB Pass TDs", "QB INTs", "QB Completion %",
            "Rush Yards", "Rush TDs",
            "Receptions", "Receiving Yards", "Receiving TDs",
            "Tackles", "Sacks", "Fumbles Recovered", "Interceptions",
            "FG Made", "FG %",
            "KO Return Yards", "KO Return TDs", "Punt Return Yards", "Punt Return TDs",
            "Coach Overall", "Coach Score"
    };

    /** League history stat category names matching League.getLeagueHistoryStats(selection). */
    private static final String[] LEAGUE_HISTORY_STAT_CATEGORIES = {
            "National Championships", "Conference Championships", "Bowl Wins",
            "Total Wins", "Hall of Fame Players"
    };

    /** Maximum simulation steps for advanceFullYear() before surfacing an error. */
    private static final int MAX_FULL_YEAR_STEPS = 200;

    private final League leagueCore;
    private LeagueRecord currentRecord;
    private File lastSavePath;

    /** Lookup from team name → live Team for O(1) access. */
    private Map<String, Team> liveTeamMap;

    private DesktopUiBridge bridge;
    private SeasonController controller;

    /** Currently viewed scoreboard week (0-based). */
    private int scoreboardWeek;

    /** Tracks whether the league has been modified since the last save. */
    private boolean dirty = false;

    /** Persistent tabbed pane so refresh() can preserve the selected tab. */
    private JTabbedPane mainTabs;

    private JLabel statusLabel;
    private JLabel playedIndicator;

    public LeagueHomeView(League league) {
        this(league, null);
    }

    public LeagueHomeView(League league, File loadedFrom) {
        this.leagueCore = league;
        this.currentRecord = league.toRecord();
        this.lastSavePath = loadedFrom;
        rebuildLiveTeamMap();

        setTitle(buildWindowTitle());
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit();
            }
        });
        setLayout(new BorderLayout());

        bridge = new DesktopUiBridge(this, leagueCore);
        controller = new SeasonController(leagueCore, bridge, NO_OP_FLOW);
        scoreboardWeek = Math.max(0, leagueCore.currentWeek);

        loadApplicationIcon();
        setJMenuBar(buildMenuBar());
        add(buildHeader(), BorderLayout.NORTH);
        mainTabs = buildMainContent();
        add(mainTabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private void rebuildLiveTeamMap() {
        liveTeamMap = new HashMap<>();
        for (Conference c : leagueCore.getConferences()) {
            for (Team t : c.getTeams()) {
                liveTeamMap.put(t.getName(), t);
            }
        }
    }

    /**
     * Attempts to load an application icon from the classpath ({@code assets/cfhc_icon.png}).
     * Silently falls back to the default Java icon if the image is not found.
     */
    private void loadApplicationIcon() {
        try {
            java.io.InputStream iconStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("assets/cfhc_icon.png");
            if (iconStream != null) {
                java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);
                if (icon != null) {
                    setIconImage(icon);
                }
                iconStream.close();
            }
        } catch (Exception ignored) {
            // Icon is cosmetic; log and continue
            PlatformLog.i(TAG, "Application icon not found; using default.");
        }
    }

    private String buildWindowTitle() {
        String suffix = lastSavePath != null ? " \u2014 " + lastSavePath.getName() : "";
        return "CFB Coach \u2014 " + currentRecord.leagueName() + " (" + currentRecord.year() + ")" + suffix;
    }

    // =========================================================================
    // Menu bar
    // =========================================================================

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open\u2026");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> openSaveFile());
        file.add(openItem);

        file.addSeparator();

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveLeague(false));
        file.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As\u2026");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveLeague(true));
        file.add(saveAsItem);

        file.addSeparator();

        JMenuItem exportItem = new JMenuItem("Export Save\u2026");
        exportItem.addActionListener(e -> exportLeague());
        file.add(exportItem);

        JMenuItem importItem = new JMenuItem("Import Custom Universe\u2026");
        importItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK));
        importItem.addActionListener(e -> importCustomUniverse());
        file.add(importItem);

        file.addSeparator();

        JMenuItem settingsItem = new JMenuItem("Settings\u2026");
        settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, KeyEvent.CTRL_DOWN_MASK));
        settingsItem.addActionListener(e -> {
            if (SettingsDialog.show(this, leagueCore)) {
                markDirty();
            }
        });
        file.add(settingsItem);

        file.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> dispatchEvent(new java.awt.event.WindowEvent(this,
                java.awt.event.WindowEvent.WINDOW_CLOSING)));
        file.add(exitItem);

        bar.add(file);

        JMenu season = new JMenu("Season");
        season.setMnemonic(KeyEvent.VK_E);

        JMenuItem playWeek = new JMenuItem("Play Next Week");
        playWeek.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        playWeek.addActionListener(e -> playWeek());
        season.add(playWeek);

        JMenuItem advance = new JMenuItem("Advance Full Season");
        advance.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK));
        advance.addActionListener(e -> advanceSeason());
        season.add(advance);

        JMenuItem advanceFull = new JMenuItem("Advance Through Offseason");
        advanceFull.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        advanceFull.addActionListener(e -> advanceFullYear());
        season.add(advanceFull);

        bar.add(season);

        JMenu team = new JMenu("Team");
        team.setMnemonic(KeyEvent.VK_T);

        JMenuItem playbookItem = new JMenuItem("Playbooks\u2026");
        playbookItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        playbookItem.addActionListener(e -> showPlaybookDialog());
        team.add(playbookItem);

        bar.add(team);

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);

        JMenuItem bowlWatch = new JMenuItem("Bowl Watch");
        bowlWatch.addActionListener(e -> showBowlWatch());
        view.add(bowlWatch);

        JMenuItem ccg = new JMenuItem("Conference Championships");
        ccg.addActionListener(e -> showConfChamps());
        view.add(ccg);

        JMenuItem mockDraft = new JMenuItem("Mock Draft");
        mockDraft.addActionListener(e -> showMockDraft());
        view.add(mockDraft);

        bar.add(view);

        JMenu help = new JMenu("Help");
        help.setMnemonic(KeyEvent.VK_H);
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        help.add(aboutItem);
        bar.add(help);

        return bar;
    }

    // =========================================================================
    // Header with user-team info
    // =========================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(getWidth(), HEADER_HEIGHT));

        // Left: league title + optional user-team summary
        JPanel leftPanel = new JPanel(new GridLayout(0, 1));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel(currentRecord.leagueName() + " \u2014 Season " + currentRecord.year());
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        leftPanel.add(title);

        if (leagueCore.userTeam != null) {
            Team ut = leagueCore.userTeam;
            String userInfo = "\u25B6 " + ut.getName() + "  (" + ut.getWins() + "-" + ut.getLosses()
                    + ")  \u2022  Prestige " + ut.getTeamPrestige()
                    + "  \u2022  Poll #" + ut.getRankTeamPollScore();
            JLabel userLabel = new JLabel(userInfo);
            userLabel.setForeground(new Color(100, 200, 255));
            userLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            leftPanel.add(userLabel);
        }
        header.add(leftPanel, BorderLayout.WEST);

        // Right: action buttons
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 14));
        controls.setOpaque(false);

        JButton playWeekBtn = new JButton(playWeekLabel());
        playWeekBtn.setToolTipText("Simulate the next week (Space)");
        playWeekBtn.addActionListener(e -> playWeek());

        JButton advanceBtn = new JButton("Advance Season");
        advanceBtn.setToolTipText("Simulate through the regular season + bowls (Ctrl+A)");
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
        if (week >= reg + 13) return "Recruiting\u2026";
        if (week >= reg + 4)  return "Offseason: Step " + (week - reg - 3);
        if (week >= reg + 3)  return "Play National Championship";
        if (week >= reg + 2)  return "Play Semifinals";
        if (week >= reg + 1)  return "Play Quarterfinals / Bowl Week";
        if (week == reg)      return "Play Conf. Championships";
        if (week <= 0)        return "Begin Season";
        return "Play Week " + (week + 1);
    }

    // =========================================================================
    // Status bar
    // =========================================================================

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
        String base = String.format("Week %d  \u2022  %d conferences  \u2022  %d teams",
                currentRecord.currentWeek(), currentRecord.conferences().size(), teams);
        int hofSize = currentRecord.leagueHoF() != null ? currentRecord.leagueHoF().size() : 0;
        if (hofSize > 0) {
            base += "  \u2022  " + hofSize + " in Hall of Fame";
        }
        return base;
    }

    /**
     * Prompts the user to save before exiting if there are unsaved changes.
     */
    private void confirmExit() {
        if (dirty) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "You have unsaved changes. Save before exiting?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                saveLeague(false);
                dispose();
                System.exit(0);
            } else if (choice == JOptionPane.NO_OPTION) {
                dispose();
                System.exit(0);
            }
            // CANCEL — do nothing, stay open
        } else {
            dispose();
            System.exit(0);
        }
    }

    /** Marks the league state as modified since the last save. */
    private void markDirty() {
        dirty = true;
    }

    // =========================================================================
    // Season advancement
    // =========================================================================

    private void playWeek() {
        long start = System.currentTimeMillis();
        bridge.clearNewSeasonPending();
        controller.advanceWeek();
        markDirty();
        PlatformLog.i(TAG, "advanceWeek: " + (System.currentTimeMillis() - start) + "ms");

        if (bridge.isNewSeasonPending()) {
            startNewSeason();
        } else {
            scoreboardWeek = leagueCore.currentWeek;
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
        scoreboardWeek = 0;
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
        while (leagueCore.currentWeek < target && !bridge.isNewSeasonPending()) {
            controller.advanceWeek();
            played++;
        }
        markDirty();
        PlatformLog.i(TAG, "Advanced " + played + " weeks in "
                + (System.currentTimeMillis() - start) + "ms");
        if (bridge.isNewSeasonPending()) {
            startNewSeason();
        } else {
            scoreboardWeek = leagueCore.currentWeek;
            refresh();
        }
    }

    /** Advances through the entire season including offseason and recruiting. */
    private void advanceFullYear() {
        long start = System.currentTimeMillis();
        int played = 0;
        bridge.clearNewSeasonPending();
        while (!bridge.isNewSeasonPending()) {
            controller.advanceWeek();
            played++;
            if (played >= MAX_FULL_YEAR_STEPS) {
                JOptionPane.showMessageDialog(this,
                        "Simulation stopped after " + MAX_FULL_YEAR_STEPS + " steps without completing the season.\n"
                                + "This may indicate a simulation bug. Save your league and report the issue.",
                        "Simulation Limit Reached", JOptionPane.WARNING_MESSAGE);
                break;
            }
        }
        markDirty();
        PlatformLog.i(TAG, "Advanced full year (" + played + " steps) in "
                + (System.currentTimeMillis() - start) + "ms");
        if (bridge.isNewSeasonPending()) {
            startNewSeason();
        } else {
            scoreboardWeek = leagueCore.currentWeek;
            refresh();
        }
    }

    // =========================================================================
    // Save / Load
    // =========================================================================

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
            dirty = false;
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

    // =========================================================================
    // Open / Import / Export
    // =========================================================================

    private void openSaveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Save File");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "CFB Save (*." + SAVE_EXTENSION + ", *.txt)",
                SAVE_EXTENSION, "txt"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(this, "File not found:\n" + file.getAbsolutePath(),
                    "Open Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
            League league = new League(
                    file,
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    false
            );
            league.setPlatformResourceProvider(resources);
            PlatformLog.i(TAG, "Loaded save from " + file.getAbsolutePath());
            LeagueHomeView.show(league, file);
            dispose(); // close the current window so only one LeagueHomeView is open
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error opening save file", ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to open save file:\n" + ex.getMessage(),
                    "Open Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportLeague() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export League Save");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showDialog(this, "Export Here");
        if (result != JFileChooser.APPROVE_OPTION) return;

        File exportDir = chooser.getSelectedFile();
        try {
            File exported = LeagueExportController.exportPrimarySave(exportDir, leagueCore);
            PlatformLog.i(TAG, "Exported to " + exported.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "League exported to:\n" + exported.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error exporting league", ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to export league:\n" + ex.getMessage(),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importCustomUniverse() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Custom Universe File");
        chooser.setFileFilter(new FileNameExtensionFilter("Custom Universe Files (*.txt, *.csv)", "txt", "csv"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File importFile = chooser.getSelectedFile();
        try {
            // Create temp files for parsed output
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "cfhc_import");
            if (!tempDir.exists()) tempDir.mkdirs();
            File confFile = new File(tempDir, "conferences.txt");
            File teamsFile = new File(tempDir, "teams.txt");
            File bowlsFile = new File(tempDir, "bowls.txt");

            java.io.FileInputStream fis = new java.io.FileInputStream(importFile);
            simulation.CustomUniverseParser.parse(fis, confFile, teamsFile, bowlsFile);
            fis.close();

            // Get resource strings
            DesktopResourceProvider res = null;
            PlatformResourceProvider provider = leagueCore.resProvider;
            if (provider instanceof DesktopResourceProvider) {
                res = (DesktopResourceProvider) provider;
            }
            if (res == null) {
                JOptionPane.showMessageDialog(this,
                        "Cannot resolve resource provider for custom universe import.",
                        "Import Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String playerNames = res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES);
            String lastNames = res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES);

            LeagueLaunchCoordinator.CustomUniverseFiles customFiles =
                    new LeagueLaunchCoordinator.CustomUniverseFiles(confFile, teamsFile, bowlsFile);

            League newLeague = new League(playerNames, lastNames,
                    customFiles.conferences, customFiles.teams, customFiles.bowls,
                    false, false, bridge);
            newLeague.setPlatformResourceProvider(res);

            // Run team selection wizard on the new league
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Custom universe imported successfully!\n"
                            + "Conferences: " + newLeague.getConferences().size() + "\n"
                            + "Teams: " + newLeague.getTeamList().size() + "\n\n"
                            + "This will replace your current league. Continue?",
                    "Import Custom Universe",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            // Reopen with new league
            dispose();
            show(newLeague);

            PlatformLog.i(TAG, "Custom universe imported from " + importFile.getAbsolutePath());
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error importing custom universe", ex);
            JOptionPane.showMessageDialog(this,
                    "Failed to import custom universe:\n" + ex.getMessage(),
                    "Import Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Team menu
    // =========================================================================

    private void showPlaybookDialog() {
        if (leagueCore.userTeam == null) {
            JOptionPane.showMessageDialog(this,
                    "No user team selected. Start a new game with a team to access playbooks.",
                    "No Team", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PlaybookDialog.show(this, leagueCore.userTeam);
    }

    // =========================================================================
    // View menu dialogs
    // =========================================================================

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "CFB Coach \u2014 Desktop Prototype\n"
                        + "Portable Java build of the CFHC simulation engine.\n\n"
                        + "Hotkeys:\n"
                        + "  Space\t\t\tPlay next week\n"
                        + "  Ctrl+A\t\tAdvance regular season\n"
                        + "  Ctrl+Shift+A\tAdvance full year\n"
                        + "  Ctrl+O\t\tOpen save file\n"
                        + "  Ctrl+S\t\tSave league\n"
                        + "  Ctrl+Shift+S\tSave As\u2026\n"
                        + "  Ctrl+P\t\tPlaybooks\n"
                        + "  Ctrl+Q\t\tExit\n",
                "About CFB Coach",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showBowlWatch() {
        String text = leagueCore.getBowlGameWatchStr();
        if (text == null || text.isEmpty()) text = "No bowl projections available yet.";
        showScrollableText("Bowl Watch", text);
    }

    private void showConfChamps() {
        String text = leagueCore.getCCGsStr();
        if (text == null || text.isEmpty()) text = "Conference championship matchups not set yet.";
        showScrollableText("Conference Championship Games", text);
    }

    private void showMockDraft() {
        String[] draft = leagueCore.getMockDraftPlayersList();
        if (draft == null || draft.length == 0) {
            showScrollableText("Mock Draft", "No mock draft data available.");
            return;
        }
        StringBuilder sb = new StringBuilder("Mock Draft Board\n");
        sb.append("=".repeat(40)).append("\n\n");
        for (int i = 0; i < draft.length; i++) {
            sb.append(String.format("%3d. %s%n", i + 1, draft[i]));
        }
        showScrollableText("Mock Draft", sb.toString());
    }

    private void showScrollableText(String title, String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(650, 450));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // Main content refresh
    // =========================================================================

    private void refresh() {
        this.currentRecord = leagueCore.toRecord();
        rebuildLiveTeamMap();
        setTitle(buildWindowTitle());

        // Preserve the currently selected tab across refresh
        int selectedTab = mainTabs != null ? mainTabs.getSelectedIndex() : 0;

        getContentPane().removeAll();
        add(buildHeader(), BorderLayout.NORTH);
        mainTabs = buildMainContent();
        add(mainTabs, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        if (selectedTab >= 0 && selectedTab < mainTabs.getTabCount()) {
            mainTabs.setSelectedIndex(selectedTab);
        }

        revalidate();
        repaint();
    }

    private JTabbedPane buildMainContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Standings", buildStandingsPanel());
        tabs.addTab("Scoreboard", buildScoreboardPanel());
        tabs.addTab("Poll Rankings", buildPollRankingsPanel());
        tabs.addTab("Team Rankings", buildTeamRankingsPanel());
        tabs.addTab("Player Stats", buildPlayerRankingsPanel());
        tabs.addTab("League History", buildLeagueHistoryPanel());
        tabs.addTab("News", buildNewsPanel());
        tabs.addTab("Coaches", buildCoachDatabasePanel());
        tabs.addTab("Hall of Fame", buildHallOfFamePanel());
        tabs.addTab("Records", buildLeagueRecordsPanel());
        return tabs;
    }

    // =========================================================================
    // Standings tab
    // =========================================================================

    private JSplitPane buildStandingsPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setLeftComponent(buildTopTeamsSidebar());
        splitPane.setRightComponent(new JScrollPane(buildConferenceGrid()));
        return splitPane;
    }

    private JPanel buildTopTeamsSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBorder(BorderFactory.createTitledBorder("Top 25 (Poll)"));

        DefaultListModel<Team> teamModel = new DefaultListModel<>();
        leagueCore.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(25)
                .forEach(teamModel::addElement);

        JList<Team> teamList = new JList<>(teamModel);
        teamList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        teamList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Team t = (Team) value;
                String label = String.format("#%-3d %-22s (%d-%d)  Pres %d",
                        t.getRankTeamPollScore(), t.getName(), t.getWins(), t.getLosses(), t.getTeamPrestige());
                Component c = super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                if (!isSelected && t == leagueCore.userTeam) {
                    c.setBackground(new Color(220, 235, 255));
                }
                return c;
            }
        });
        teamList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Team sel = teamList.getSelectedValue();
                    if (sel != null) openTeamDialogFromLive(sel);
                }
            }
        });

        sidebar.add(new JScrollPane(teamList), BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildConferenceGrid() {
        JPanel content = new JPanel(new GridLayout(0, 2, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Conference conf : leagueCore.getConferences()) {
            content.add(buildConferencePanel(conf));
        }
        return content;
    }

    private JPanel buildConferencePanel(Conference conf) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Conference header with TV info
        String headerText = " " + conf.confName;
        if (conf.confTV) {
            headerText += "  (" + conf.getTVName() + ")";
        }
        JLabel label = new JLabel(headerText);
        label.setOpaque(true);
        label.setBackground(CONF_HEADER_BG);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("SansSerif", Font.BOLD, 15));
        panel.add(label, BorderLayout.NORTH);

        // Sort teams by conference wins then overall record
        List<Team> sorted = new ArrayList<>(conf.getTeams());
        sorted.sort((a, b) -> {
            int cmp = Integer.compare(b.getConfWins(), a.getConfWins());
            if (cmp != 0) return cmp;
            cmp = Integer.compare(a.getConfLosses(), b.getConfLosses());
            if (cmp != 0) return cmp;
            return Integer.compare(b.getWins(), a.getWins());
        });

        // Sortable table with conference standings columns
        String[] cols = {"#", "Team", "Record", "Conf", "Pres"};
        DefaultTableModel confModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 0, 4 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        for (Team t : sorted) {
            confModel.addRow(new Object[]{
                    t.getRankTeamPollScore() <= 25 ? t.getRankTeamPollScore() : null,
                    t.getName(),
                    t.getWins() + "-" + t.getLosses(),
                    t.getConfWins() + "-" + t.getConfLosses(),
                    t.getTeamPrestige()
            });
        }

        JTable confTable = new JTable(confModel);
        confTable.setAutoCreateRowSorter(true);
        confTable.setRowHeight(20);
        confTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        confTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        confTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        confTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        confTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        confTable.getColumnModel().getColumn(4).setPreferredWidth(40);

        // Highlight user team rows
        confTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected && leagueCore.userTeam != null) {
                    String name = (String) tbl.getValueAt(row, 1);
                    if (leagueCore.userTeam.getName().equals(name)) {
                        c.setBackground(new Color(220, 235, 255));
                    } else {
                        c.setBackground(tbl.getBackground());
                    }
                }
                return c;
            }
        });
        // Also set renderer for Integer columns
        confTable.setDefaultRenderer(Integer.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected && leagueCore.userTeam != null) {
                    String name = (String) tbl.getValueAt(row, 1);
                    if (leagueCore.userTeam.getName().equals(name)) {
                        c.setBackground(new Color(220, 235, 255));
                    } else {
                        c.setBackground(tbl.getBackground());
                    }
                }
                return c;
            }
        });

        confTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = confTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) confTable.getValueAt(row, 1);
                        Team t = liveTeamMap.get(name);
                        if (t != null) openTeamDialogFromLive(t);
                    }
                }
            }
        });
        panel.add(new JScrollPane(confTable), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Poll Rankings tab (Top 25 with full details)
    // =========================================================================

    private JPanel buildPollRankingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"Rank", "Team", "Record", "Conf", "Poll Score", "Prestige", "Off Tal", "Def Tal"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                return switch (col) {
                    case 0, 5 -> Integer.class;
                    case 4, 6, 7 -> Float.class;
                    default -> String.class;
                };
            }
        };

        leagueCore.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .forEach(t -> model.addRow(new Object[]{
                        t.getRankTeamPollScore(),
                        t.getName(),
                        t.getWins() + "-" + t.getLosses(),
                        t.getConfWins() + "-" + t.getConfLosses(),
                        t.getTeamPollScore(),
                        t.getTeamPrestige(),
                        t.getOffTalent(),
                        t.getDefTalent()
                }));

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) table.getValueAt(row, 1);
                        Team t = liveTeamMap.get(name);
                        if (t != null) openTeamDialogFromLive(t);
                    }
                }
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(new JLabel("Double-click a team for details. Teams are sortable by column."),
                BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Team Rankings tab (23 categories via combo box)
    // =========================================================================

    private JPanel buildTeamRankingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> categoryBox = new JComboBox<>(TEAM_RANKING_CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Team", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String cellVal = String.valueOf(table.getValueAt(row, 1));
                        // Team name may have newline chars from recruiting class view
                        String teamName = cellVal.split("\n")[0].trim();
                        Team t = liveTeamMap.get(teamName);
                        if (t != null) openTeamDialogFromLive(t);
                    }
                }
            }
        });

        Runnable loadRankings = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                ArrayList<String> rankings = leagueCore.getTeamRankingsStr(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading team rankings", ex);
            }
        };

        categoryBox.addActionListener(e -> loadRankings.run());
        loadRankings.run(); // initial load

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Category: "));
        topBar.add(categoryBox);
        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Player Rankings tab (22 stat categories via League.getPlayerRankStr)
    // =========================================================================

    private JPanel buildPlayerRankingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JComboBox<String> categoryBox = new JComboBox<>(PLAYER_RANKING_CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Player", "Team", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);

        Runnable loadPlayerRankings = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                ArrayList<String> rankings = leagueCore.getPlayerRankStr(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 4);
                        if (parts.length >= 4) {
                            model.addRow(new Object[]{
                                    parts[0].trim(), parts[1].trim(),
                                    parts[2].trim(), parts[3].trim()
                            });
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading player rankings", ex);
            }
        };

        categoryBox.addActionListener(e -> loadPlayerRankings.run());
        loadPlayerRankings.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Category: "));
        topBar.add(categoryBox);
        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(new JLabel(PLAYER_RANKINGS_HINT), BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // League History tab (season champions + all-time team stats)
    // =========================================================================

    private JPanel buildLeagueHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top: season-by-season champion history
        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyArea.setLineWrap(false);
        String historyText = leagueCore.getLeagueHistoryStr();
        if (historyText == null || historyText.trim().isEmpty()) {
            historyText = "No season history yet — play at least one full season.";
        } else {
            // The engine uses '%' as a line-end sentinel; replace for readability
            historyText = historyText.replace("%", "");
        }
        historyArea.setText(historyText);
        historyArea.setCaretPosition(0);
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Season Champions"));
        historyScroll.setPreferredSize(new Dimension(0, 200));

        // Bottom: all-time team stats with category selector
        JPanel statsPanel = new JPanel(new BorderLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("All-Time Team Records"));

        JComboBox<String> categoryBox = new JComboBox<>(LEAGUE_HISTORY_STAT_CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Team", "Total"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String teamName = String.valueOf(table.getValueAt(row, 1));
                        Team t = liveTeamMap.get(teamName);
                        if (t != null) openTeamDialogFromLive(t);
                    }
                }
            }
        });

        Runnable loadHistoryStats = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                ArrayList<String> rankings = leagueCore.getLeagueHistoryStats(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading league history stats", ex);
            }
        };

        categoryBox.addActionListener(e -> loadHistoryStats.run());
        loadHistoryStats.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Category: "));
        topBar.add(categoryBox);
        statsPanel.add(topBar, BorderLayout.NORTH);
        statsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, historyScroll, statsPanel);
        split.setDividerLocation(220);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Scoreboard tab (with week navigation)
    // =========================================================================

    private JPanel buildScoreboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextArea scores = new JTextArea();
        scores.setEditable(false);
        scores.setFont(new Font("Monospaced", Font.PLAIN, 13));

        // Week navigation controls
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JButton prevBtn = new JButton("\u25C0 Prev Week");
        JButton nextBtn = new JButton("Next Week \u25B6");
        JLabel weekLabel = new JLabel();
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        Runnable updateScoreboard = () -> {
            weekLabel.setText("Week " + scoreboardWeek);
            scores.setText(buildScoresTextForWeek(scoreboardWeek));
            scores.setCaretPosition(0);
            prevBtn.setEnabled(scoreboardWeek > 1);
            nextBtn.setEnabled(scoreboardWeek < leagueCore.currentWeek);
        };

        prevBtn.addActionListener(e -> {
            if (scoreboardWeek > 1) {
                scoreboardWeek--;
                updateScoreboard.run();
            }
        });
        nextBtn.addActionListener(e -> {
            if (scoreboardWeek < leagueCore.currentWeek) {
                scoreboardWeek++;
                updateScoreboard.run();
            }
        });

        navPanel.add(prevBtn);
        navPanel.add(weekLabel);
        navPanel.add(nextBtn);
        panel.add(navPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(scores), BorderLayout.CENTER);

        updateScoreboard.run();
        return panel;
    }

    private String buildScoresTextForWeek(int week) {
        if (week <= 0 || leagueCore.getWeeklyScores() == null
                || week >= leagueCore.getWeeklyScores().size()) {
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

    // =========================================================================
    // News tab
    // =========================================================================

    private JPanel buildNewsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultListModel<String> headlineModel = new DefaultListModel<>();

        JTextArea storyArea = new JTextArea("Select a headline to read the full story.");
        storyArea.setEditable(false);
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JList<String> headlineList = new JList<>(headlineModel);
        headlineList.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // Week navigation controls for news archive
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        JButton prevBtn = new JButton("\u25C0 Prev Week");
        JButton nextBtn = new JButton("Next Week \u25B6");
        JButton latestBtn = new JButton("Latest");
        JLabel weekLabel = new JLabel();
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Use an array for mutable access inside lambdas
        final int[] newsWeek = { Math.max(0, leagueCore.currentWeek) };

        Runnable loadNewsForWeek = () -> {
            headlineModel.clear();
            weekLabel.setText("Week " + newsWeek[0] + " News");
            List<List<String>> stories = leagueCore.getNewsStories();
            if (stories != null && newsWeek[0] >= 0 && newsWeek[0] < stories.size()) {
                List<String> weekStories = stories.get(newsWeek[0]);
                if (weekStories != null) {
                    for (String s : weekStories) {
                        String[] parts = s.split(NEWS_STORY_DELIMITER);
                        headlineModel.addElement(parts[0].trim());
                    }
                }
            }
            if (headlineModel.isEmpty()) {
                headlineModel.addElement("No news for week " + newsWeek[0] + ".");
            }
            storyArea.setText("Select a headline to read the full story.");
            prevBtn.setEnabled(newsWeek[0] > 0);
            nextBtn.setEnabled(newsWeek[0] < leagueCore.currentWeek);
        };

        prevBtn.addActionListener(e -> {
            if (newsWeek[0] > 0) { newsWeek[0]--; loadNewsForWeek.run(); }
        });
        nextBtn.addActionListener(e -> {
            if (newsWeek[0] < leagueCore.currentWeek) { newsWeek[0]++; loadNewsForWeek.run(); }
        });
        latestBtn.addActionListener(e -> {
            newsWeek[0] = Math.max(0, leagueCore.currentWeek);
            loadNewsForWeek.run();
        });

        navPanel.add(prevBtn);
        navPanel.add(weekLabel);
        navPanel.add(nextBtn);
        navPanel.add(latestBtn);
        panel.add(navPanel, BorderLayout.NORTH);

        headlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int idx = headlineList.getSelectedIndex();
                if (idx >= 0) {
                    String story = lookupStoryForWeek(headlineList.getSelectedValue(), newsWeek[0]);
                    storyArea.setText(story != null ? story : headlineList.getSelectedValue());
                    storyArea.setCaretPosition(0);
                }
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(headlineList), new JScrollPane(storyArea));
        split.setDividerLocation(320);
        panel.add(split, BorderLayout.CENTER);

        loadNewsForWeek.run();
        return panel;
    }

    /**
     * Looks up the full story body for a headline in a specific week.
     */
    private String lookupStoryForWeek(String headline, int week) {
        List<List<String>> stories = leagueCore.getNewsStories();
        if (stories == null || week < 0 || week >= stories.size()) return null;
        List<String> weekStories = stories.get(week);
        if (weekStories == null) return null;
        for (String story : weekStories) {
            String[] parts = story.split(NEWS_STORY_DELIMITER);
            if (parts.length >= 2 && headline.contains(parts[0].trim())) {
                return parts[1].trim();
            }
            if (parts.length >= 1 && headline.startsWith(parts[0].trim())) {
                return parts.length >= 2 ? parts[1].trim() : parts[0].trim();
            }
        }
        return null;
    }

    /** @deprecated Use {@link #lookupStoryForWeek(String, int)} instead. */
    @Deprecated
    @SuppressWarnings("unused")
    private String lookupStory(String headline) {
        if (leagueCore.getNewsStories() == null || leagueCore.getNewsStories().isEmpty()) return null;
        int week = leagueCore.currentWeek;
        int maxWeek = Math.min(week + 1, leagueCore.getNewsStories().size() - 1);
        if (maxWeek < 0) return null;
        for (int w = maxWeek; w >= 0; w--) {
            List<String> weekStories = leagueCore.getNewsStories().get(w);
            if (weekStories == null) continue;
            for (String story : weekStories) {
                String[] parts = story.split(NEWS_STORY_DELIMITER);
                if (parts.length >= 2 && headline.contains(parts[0])) {
                    return parts[1];
                }
                if (parts.length >= 1 && headline.startsWith(parts[0])) {
                    return parts.length >= 2 ? parts[1] : parts[0];
                }
            }
        }
        if (week >= 0 && week < leagueCore.getNewsStories().size()) {
            List<String> weekStories = leagueCore.getNewsStories().get(week);
            if (weekStories != null && !weekStories.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String s : weekStories) {
                    sb.append(s.replace(NEWS_STORY_DELIMITER, "\n\n")).append("\n\n---\n\n");
                }
                return sb.toString();
            }
        }
        return null;
    }

    // =========================================================================
    // Coach Database tab
    // =========================================================================

    private JPanel buildCoachDatabasePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] categories = {
                "National Champs", "Conf. Champs", "Bowl Wins", "Total Wins", "Win %",
                "Coach of the Year", "Conf COTY", "All-Americans", "All-Conference",
                "Career Score", "Career Prestige"
        };
        JComboBox<String> categoryBox = new JComboBox<>(categories);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Coach", "Value"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);

        Runnable loadCoaches = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                ArrayList<String> rankings = leagueCore.getCoachDatabase(sel);
                if (rankings != null) {
                    for (String line : rankings) {
                        String[] parts = line.split(",", 3);
                        if (parts.length >= 3) {
                            model.addRow(new Object[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                        }
                    }
                }
            } catch (Exception ex) {
                PlatformLog.e(TAG, "Error loading coach database", ex);
            }
        };

        categoryBox.addActionListener(e -> loadCoaches.run());
        loadCoaches.run();

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Sort by: "));
        topBar.add(categoryBox);
        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Hall of Fame tab
    // =========================================================================

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

        // Double-click a HoF player to open their details (if still on a live roster)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) table.getValueAt(row, 0);
                        String teamName = (String) table.getValueAt(row, 2);
                        Player player = findPlayerInLeague(name, teamName);
                        if (player != null) {
                            PlayerDetailView.show(LeagueHomeView.this, player);
                        } else {
                            // Player may have graduated; show a summary from the record
                            int modelRow = table.convertRowIndexToModel(row);
                            if (modelRow >= 0 && modelRow < hof.size()) {
                                PlayerRecord pr = hof.get(modelRow);
                                JOptionPane.showMessageDialog(LeagueHomeView.this,
                                        pr.name() + "  (" + pr.position() + ")\n"
                                                + "Team: " + pr.teamName() + "\n"
                                                + "Overall: " + pr.ratOvr(),
                                        "Hall of Fame — " + pr.name(),
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        }
                    }
                }
            }
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JLabel count = new JLabel(hof.size() + " inductees", JLabel.RIGHT);
        count.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        bottom.add(count, BorderLayout.EAST);
        bottom.add(new JLabel("Double-click a player for details."), BorderLayout.WEST);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // League Records tab
    // =========================================================================

    private JPanel buildLeagueRecordsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<DataRecord> records = currentRecord.leagueRecords();
        if (records == null || records.isEmpty()) {
            JLabel empty = new JLabel("No league records have been set yet \u2014 play a full season!",
                    JLabel.CENTER);
            panel.add(empty, BorderLayout.CENTER);
            return panel;
        }

        String[] columns = {"Record", "Value", "Holder", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (DataRecord dr : records) {
            if (dr != null) {
                model.addRow(new Object[]{dr.key(), formatValue(dr.value()), formatHolder(dr.holder()), dr.year()});
            }
        }
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static String formatValue(float value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.2f", value);
    }

    /**
     * Formats a {@link DataRecord} holder string for display.
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

    // =========================================================================
    // Team dialog helpers
    // =========================================================================

    private void openTeamDialog(LeagueRecord.TeamRecord team) {
        Team live = liveTeamMap.get(team.name());
        TeamDetailView.show(this, team, live);
    }

    private void openTeamDialogFromLive(Team live) {
        LeagueRecord.TeamRecord teamRec = findTeamRecord(live.getName());
        if (teamRec != null) {
            TeamDetailView.show(this, teamRec, live);
        }
    }

    private LeagueRecord.TeamRecord findTeamRecord(String teamName) {
        for (LeagueRecord.ConferenceRecord c : currentRecord.conferences()) {
            for (LeagueRecord.TeamRecord t : c.teams()) {
                if (t.name().equals(teamName)) return t;
            }
        }
        return null;
    }

    /**
     * Searches all live teams for a player matching the given name and team.
     * Returns {@code null} if the player has graduated or is no longer on any roster.
     */
    private Player findPlayerInLeague(String playerName, String teamName) {
        if (playerName == null) return null;
        // First try the specific team
        Team t = liveTeamMap.get(teamName);
        if (t != null) {
            for (Player p : t.getAllPlayers()) {
                if (playerName.equals(p.name)) return p;
            }
        }
        // Fall back: search all teams (player may have transferred)
        for (Team team : liveTeamMap.values()) {
            for (Player p : team.getAllPlayers()) {
                if (playerName.equals(p.name)) return p;
            }
        }
        return null;
    }

    // =========================================================================
    // Static factory methods and snapshot viewer
    // =========================================================================

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
