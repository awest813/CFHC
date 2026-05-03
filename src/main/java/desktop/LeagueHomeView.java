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
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
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
 * Graphical 'League Home' view for the desktop app. Displays standings,
 * poll rankings, team/player statistics, scoreboard with week navigation, news,
 * hall of fame, league records, and coach database.
 *
 * <p>The view uses {@link SeasonController} for all week advancement so the full
 * season–offseason–new-season loop works correctly without any Android dependencies.
 */
public class LeagueHomeView extends JFrame {

    private static final String TAG = "LeagueHomeView";
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

    private static final String[] NAV_TITLES = {
            "Home", "Recruiting", "Standings", "Scoreboard", "My Coach",
            "Poll Rankings", "Team Rankings", "Player Stats", "Player Search",
            "League History", "News", "Coaches", "Hall of Fame", "Records", "Settings"
    };

    private String selectedScreen = "Home";
    private JPanel mainContentCards;
    private CardLayout mainCardLayout;
    private JList<String> navigationList;

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
        registerGlobalShortcuts();
        setJMenuBar(buildMenuBar());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        applyWindowTheme();
    }

    private void applyWindowTheme() {
        getContentPane().setBackground(DesktopTheme.windowBackground());
        JMenuBar mb = getJMenuBar();
        if (mb != null) {
            mb.setOpaque(true);
            mb.setBackground(DesktopTheme.menuBarBackground());
        }
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
        String dirtyMark = dirty ? " *" : "";
        return "CFB Coach \u2014 " + currentRecord.leagueName() + " (" + currentRecord.year() + ")" + suffix + dirtyMark;
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
        settingsItem.addActionListener(e -> openSettingsDialog());
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
        advance.setEnabled(leagueCore.currentWeek < leagueCore.regSeasonWeeks + 4);
        season.add(advance);

        JMenuItem advanceFull = new JMenuItem("Advance Through Offseason");
        advanceFull.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
        advanceFull.addActionListener(e -> advanceFullYear());
        season.add(advanceFull);

        season.addSeparator();

        JMenuItem recruitingTabItem = new JMenuItem("Show Recruiting");
        recruitingTabItem.addActionListener(e -> selectRecruitingTab());
        season.add(recruitingTabItem);

        bar.add(season);

        JMenu team = new JMenu("Team");
        team.setMnemonic(KeyEvent.VK_T);

        JMenuItem playbookItem = new JMenuItem("Playbooks\u2026");
        playbookItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        playbookItem.addActionListener(e -> showPlaybookDialog());
        playbookItem.setEnabled(leagueCore.userTeam != null);
        team.add(playbookItem);

        JMenuItem myProgramItem = new JMenuItem("My Program\u2026");
        myProgramItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK));
        myProgramItem.addActionListener(e -> openUserTeamDetail());
        myProgramItem.setEnabled(leagueCore.userTeam != null);
        team.add(myProgramItem);

        bar.add(team);

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);

        JCheckBoxMenuItem darkModeItem = new JCheckBoxMenuItem("Dark mode", DesktopTheme.isDark());
        darkModeItem.addActionListener(e -> {
            DesktopTheme.setDark(darkModeItem.isSelected());
            refresh();
        });
        view.add(darkModeItem);
        view.addSeparator();

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
        JMenuItem shortcutsItem = new JMenuItem("Keyboard Shortcuts\u2026");
        shortcutsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        shortcutsItem.addActionListener(e -> showKeyboardShortcuts());
        help.add(shortcutsItem);

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        help.add(aboutItem);
        bar.add(help);

        bar.setOpaque(true);
        bar.setBackground(DesktopTheme.menuBarBackground());

        return bar;
    }

    private void registerGlobalShortcuts() {
        getRootPane().registerKeyboardAction(
                e -> showKeyboardShortcuts(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(
                e -> showKeyboardShortcuts(),
                KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // =========================================================================
    // Header with user-team info
    // =========================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DesktopTheme.headerBackground());
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

        JButton advanceBtn = new JButton("Simulate to Post-Season");
        advanceBtn.setToolTipText("Advance through the remaining regular season games.");
        advanceBtn.addActionListener(e -> simulateToPostSeason(leagueCore.regSeasonWeeks));
        advanceBtn.setEnabled(leagueCore.currentWeek < leagueCore.regSeasonWeeks);

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
        status.setBackground(DesktopTheme.statusBackground());
        status.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        statusLabel = new JLabel(buildStatusText());
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(DesktopTheme.textPrimary());
        status.add(statusLabel, BorderLayout.WEST);

        playedIndicator = new JLabel(lastSavePath != null
                ? "Save: " + lastSavePath.getName()
                : "Unsaved league");
        playedIndicator.setFont(new Font("SansSerif", Font.PLAIN, 12));
        playedIndicator.setForeground(dirty ? DesktopTheme.warningText() : DesktopTheme.textSecondary());
        status.add(playedIndicator, BorderLayout.EAST);

        return status;
    }

    private String buildStatusText() {
        int week = currentRecord.currentWeek();
        int reg = leagueCore.regSeasonWeeks;
        String weekLabel;
        if (week >= reg + 3) weekLabel = "National Championship";
        else if (week >= reg + 2) weekLabel = "Playoff Semifinals";
        else if (week >= reg + 1) weekLabel = "Quarterfinals / Bowls";
        else if (week == reg) weekLabel = "Conf. Championships";
        else if (week == 0) weekLabel = "Pre-Season";
        else weekLabel = "Week " + week;

        int teams = currentRecord.conferences().stream().mapToInt(c -> c.teams().size()).sum();
        String base = String.format("%s  \u2022  %d conferences  \u2022  %d teams",
                weekLabel, currentRecord.conferences().size(), teams);
        
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
                    DesktopTheme.messageForDialog("You have unsaved changes. Save before exiting?"),
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                saveLeague(false);
                if (!dirty) {
                    dispose();
                    System.exit(0);
                }
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
        updateDirtyChrome();
    }

    private void updateDirtyChrome() {
        setTitle(buildWindowTitle());
        if (playedIndicator != null) {
            playedIndicator.setText(dirty
                    ? "Unsaved changes"
                    : (lastSavePath != null ? "Saved: " + lastSavePath.getName() : "Unsaved league"));
            playedIndicator.setForeground(dirty ? DesktopTheme.warningText() : DesktopTheme.textSecondary());
        }
    }

    // =========================================================================
    // Season advancement
    // =========================================================================

    private void playWeek() {
        if (bridge.isAwaitingDockedRecruiting()) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "Finish recruiting in Recruiting, then the new season will roll forward automatically."),
                    "Recruiting",
                    JOptionPane.INFORMATION_MESSAGE);
            selectRecruitingTab();
            return;
        }

        int weekBefore = leagueCore.currentWeek;
        bridge.clearNewSeasonPending();
        controller.advanceWeek();
        markDirty();

        if (bridge.isAwaitingDockedRecruiting()) {
            scoreboardWeek = leagueCore.currentWeek;
            refresh();
            selectRecruitingTab();
            return;
        }

        if (bridge.isNewSeasonPending()) {
            startNewSeason();
        } else {
            scoreboardWeek = leagueCore.currentWeek;
            refresh();
            // Show result summary for user team
            showWeekResultSummary(weekBefore);
        }
    }

    private void showWeekResultSummary(int week) {
        if (leagueCore.userTeam == null) return;
        simulation.Game g = null;
        for (simulation.Game game : leagueCore.userTeam.getGameSchedule()) {
            if (leagueCore.userTeam.getGameSchedule().indexOf(game) == week) {
                g = game;
                break;
            }
        }

        if (g != null && g.hasPlayed && !"BYE WEEK".equals(g.gameName)) {
            String opp = g.homeTeam == leagueCore.userTeam ? g.awayTeam.getAbbr() : g.homeTeam.getAbbr();
            String site = g.homeTeam == leagueCore.userTeam ? "vs " : "at ";
            int score = g.homeTeam == leagueCore.userTeam ? g.homeScore : g.awayScore;
            int oppScore = g.homeTeam == leagueCore.userTeam ? g.awayScore : g.homeScore;
            String result = score > oppScore ? "WIN" : (score < oppScore ? "LOSS" : "TIE");
            
            String msg = String.format("Week %d Result:\n\n%s %s %s\nFinal Score: %d - %d\n\nRecord: %d-%d",
                    week + 1, result, site, opp, score, oppScore, 
                    leagueCore.userTeam.getWins(), leagueCore.userTeam.getLosses());
            
            JOptionPane.showMessageDialog(this, DesktopTheme.messageForDialog(msg), "Game Result",
                    score >= oppScore ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
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
                DesktopTheme.messageForDialog(
                "Season " + leagueCore.getYear() + " is ready!\n"
                        + "Press Space or click the Play button to begin."),
                "New Season", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Advances the regular season (weeks 1 through regSeasonWeeks+4) silently. */
    /**
     * Async simulation that stops at a specific target week or major event.
     * Keeps the UI responsive and provides feedback.
     */
    private void simulateToPostSeason(int targetWeek) {
        if (targetWeek <= leagueCore.currentWeek) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "This league is already at or beyond that point in the season.\nUse Play Next Week or Advance Through Offseason instead."),
                    "Nothing to Simulate",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        SimulationProgressDialog dialog = new SimulationProgressDialog(this, "Season Simulation");
        int startWeek = leagueCore.currentWeek;
        int maxWeeks = Math.max(1, targetWeek - startWeek);

        javax.swing.SwingWorker<Integer, String> worker = new javax.swing.SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                int played = 0;
                while (leagueCore.currentWeek < targetWeek && !bridge.isNewSeasonPending()
                        && !bridge.isAwaitingDockedRecruiting()) {
                    if (dialog.isCancelled()) break;
                    controller.advanceWeek();
                    played++;
                    int progress = (int) ((float) played / maxWeeks * 100);
                    setProgress(Math.min(100, progress));
                    publish("Playing Week " + (leagueCore.currentWeek));
                }
                return played;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                dialog.setStatus(chunks.get(chunks.size() - 1));
                dialog.setProgress(getProgress());
            }

            @Override
            protected void done() {
                dialog.dispose();
                markDirty();
                try {
                    int played = get();
                    PlatformLog.i(TAG, "Simulated " + played + " weeks.");
                } catch (Exception ignored) {}

                if (bridge.isAwaitingDockedRecruiting()) {
                    scoreboardWeek = leagueCore.currentWeek;
                    refresh();
                    selectRecruitingTab();
                    JOptionPane.showMessageDialog(LeagueHomeView.this,
                            DesktopTheme.messageForDialog(
                            "Simulation paused at recruiting. Use Recruiting, then continue with Play Week."),
                            "Recruiting",
                            JOptionPane.INFORMATION_MESSAGE);
                } else if (bridge.isNewSeasonPending()) {
                    startNewSeason();
                } else {
                    scoreboardWeek = leagueCore.currentWeek;
                    refresh();
                }
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                dialog.setProgress((Integer) evt.getNewValue());
            }
        });

        worker.execute();
        dialog.setVisible(true);
    }

    private void advanceSeason() {
        simulateToPostSeason(leagueCore.regSeasonWeeks + 4);
    }

    /** Advances through the entire season including offseason and recruiting. */
    private void advanceFullYear() {
        long start = System.currentTimeMillis();
        int played = 0;
        bridge.clearNewSeasonPending();
        while (!bridge.isNewSeasonPending()) {
            if (bridge.isAwaitingDockedRecruiting()) {
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog(
                        "Bulk advance stopped at recruiting.\n"
                                + "Open Recruiting, click Finish Recruiting, then use Play Week or Save."),
                        "Recruiting",
                        JOptionPane.INFORMATION_MESSAGE);
                break;
            }
            controller.advanceWeek();
            played++;
            if (played >= MAX_FULL_YEAR_STEPS) {
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog(
                        "Simulation stopped after " + MAX_FULL_YEAR_STEPS + " steps without completing the season.\n"
                                + "This may indicate a simulation bug. Save your league and report the issue."),
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
            if (bridge.isAwaitingDockedRecruiting()) {
                selectRecruitingTab();
            }
        }
    }

    private void selectRecruitingTab() {
        selectScreen("Recruiting");
    }

    private static JPanel buildScreenHeader(String title, String subtitle) {
        JPanel header = new JPanel(new GridLayout(0, 1, 0, 2));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        titleLabel.setForeground(DesktopTheme.textPrimary());
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitleLabel.setForeground(DesktopTheme.textSecondary());
        header.add(titleLabel);
        header.add(subtitleLabel);
        return header;
    }

    // =========================================================================
    // Save / Load
    // =========================================================================

    private void saveLeague(boolean forceChooser) {
        File target = lastSavePath;
        if (forceChooser || target == null) {
            JFileChooser chooser = new JFileChooser();
            DesktopTheme.styleFileChooser(chooser);
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
                        DesktopTheme.messageForDialog("Overwrite existing file \"" + target.getName() + "\"?"),
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
            updateDirtyChrome();
            PlatformLog.i(TAG, "League saved to " + target.getAbsolutePath());
        } else {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("Failed to save league to:\n" + target.getAbsolutePath()),
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
        DesktopTheme.styleFileChooser(chooser);
        chooser.setDialogTitle("Open Save File");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "CFB Save (*." + SAVE_EXTENSION + ", *.txt)",
                SAVE_EXTENSION, "txt"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.isFile()) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("File not found:\n" + file.getAbsolutePath()),
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
            league.rebuildScheduleIfNeeded();
            if (!DesktopTeamSelectionDialog.ensureUserTeam(this, league)) {
                return;
            }
            PlatformLog.i(TAG, "Loaded save from " + file.getAbsolutePath());
            LeagueHomeView.show(league, file);
            dispose(); // close the current window so only one LeagueHomeView is open
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error opening save file", ex);
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("Failed to open save file:\n" + ex.getMessage()),
                    "Open Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportLeague() {
        JFileChooser chooser = new JFileChooser();
        DesktopTheme.styleFileChooser(chooser);
        chooser.setDialogTitle("Export League Save");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showDialog(this, "Export Here");
        if (result != JFileChooser.APPROVE_OPTION) return;

        File exportDir = chooser.getSelectedFile();
        try {
            File exported = LeagueExportController.exportPrimarySave(exportDir, leagueCore);
            PlatformLog.i(TAG, "Exported to " + exported.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("League exported to:\n" + exported.getAbsolutePath()),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error exporting league", ex);
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("Failed to export league:\n" + ex.getMessage()),
                    "Export Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importCustomUniverse() {
        JFileChooser chooser = new JFileChooser();
        DesktopTheme.styleFileChooser(chooser);
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
                        DesktopTheme.messageForDialog(
                        "Cannot resolve resource provider for custom universe import."),
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
                    DesktopTheme.messageForDialog(
                    "Custom universe imported successfully!\n"
                            + "Conferences: " + newLeague.getConferences().size() + "\n"
                            + "Teams: " + newLeague.getTeamList().size() + "\n\n"
                            + "This will replace your current league. Continue?"),
                    "Import Custom Universe",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            if (!DesktopTeamSelectionDialog.ensureUserTeam(this, newLeague)) {
                return;
            }

            // Reopen with new league
            dispose();
            show(newLeague);

            PlatformLog.i(TAG, "Custom universe imported from " + importFile.getAbsolutePath());
        } catch (Exception ex) {
            PlatformLog.e(TAG, "Error importing custom universe", ex);
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("Failed to import custom universe:\n" + ex.getMessage()),
                    "Import Failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Clean up temp files
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "cfhc_import");
            if (tempDir.exists()) {
                File[] temps = tempDir.listFiles();
                if (temps != null) {
                    for (File f : temps) f.delete();
                }
                tempDir.delete();
            }
        }
    }

    // =========================================================================
    // Team menu
    // =========================================================================

    private void showPlaybookDialog() {
        if (leagueCore.userTeam == null) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "No user team selected. Start a new game with a team to access playbooks."),
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
                DesktopTheme.messageForDialog(
                "CFB Coach \u2014 Desktop (CFHC)\n"
                        + "Portable Java build of the College Football Head Coach simulation.\n\n"
                        + "Press F1 or Ctrl+/ for the full shortcut list."),
                "About CFB Coach",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showKeyboardShortcuts() {
        String text = """
                Navigation & windows
                  F1, Ctrl+/     This shortcut list
                  Ctrl+U         My Program (roster, depth chart, facilities)
                  Ctrl+P         Playbooks
                  Ctrl+,         Settings

                Season
                  Space          Play next week / advance phase
                  Ctrl+A         Simulate regular season through post-season
                  Ctrl+Shift+A   Advance through offseason (stops at recruiting)

                Recruiting
                  Recruiting     Opens after NLI week; finish there to start the new season.

                Files
                  Ctrl+O         Open save
                  Ctrl+S         Save
                  Ctrl+Shift+S   Save As
                  Ctrl+I         Import custom universe
                  Ctrl+Q         Exit

                Data views
                  Double-click a team in Standings for full team detail.
                  Double-click players in rankings where supported.
                """;
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        DesktopTheme.styleTextContent(area);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        scroll.setPreferredSize(new Dimension(520, 360));
        JOptionPane.showMessageDialog(this, scroll, "Keyboard Shortcuts", JOptionPane.PLAIN_MESSAGE);
    }

    private void openUserTeamDetail() {
        if (leagueCore.userTeam == null) {
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog(
                    "No user-controlled team is set. Start a career from the hub to pick a program."),
                    "My Program",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Team live = leagueCore.userTeam;
        LeagueRecord.TeamRecord rec = findTeamRecord(live.getName());
        if (rec != null) {
            TeamDetailView.show(this, rec, live);
        }
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
        String[] draft;
        try {
            draft = leagueCore.getMockDraftPlayersList();
        } catch (RuntimeException ex) {
            PlatformLog.e(TAG, "Error building mock draft", ex);
            showScrollableText("Mock Draft", "Mock draft data is not available yet. Try again after more players have declared.");
            return;
        }
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
        DesktopTheme.styleTextContent(area);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        scroll.setPreferredSize(new Dimension(650, 450));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void openSettingsDialog() {
        if (SettingsDialog.show(this, leagueCore)) {
            markDirty();
        }
        refresh();
    }

    // =========================================================================
    // Main content refresh
    // =========================================================================

    /** Called when desktop shell theme changes (e.g. from {@link SettingsDialog}). */
    public void applyDesktopTheme() {
        refresh();
    }

    private void refresh() {
        this.currentRecord = leagueCore.toRecord();
        rebuildLiveTeamMap();
        setTitle(buildWindowTitle());

        getContentPane().removeAll();
        setJMenuBar(buildMenuBar());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        selectScreen(selectedScreen);

        revalidate();
        repaint();
        applyWindowTheme();
    }

    private JPanel buildMainContent() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(true);
        shell.setBackground(DesktopTheme.windowBackground());

        navigationList = buildNavigationList();
        JScrollPane navScroll = new JScrollPane(navigationList);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.getViewport().setBackground(DesktopTheme.sidebarBackground());

        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setOpaque(true);
        sidebar.setBackground(DesktopTheme.sidebarBackground());
        sidebar.setPreferredSize(new Dimension(205, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DesktopTheme.borderSubtle()));

        JPanel navHeader = new JPanel(new GridLayout(0, 1, 0, 2));
        navHeader.setOpaque(true);
        navHeader.setBackground(DesktopTheme.sidebarBackground());
        navHeader.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
        JLabel office = new JLabel("LEAGUE OFFICE");
        office.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        office.setForeground(DesktopTheme.sidebarText());
        JLabel phase = new JLabel(decodeSeasonPeriod());
        phase.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        phase.setForeground(DesktopTheme.textSecondary());
        navHeader.add(office);
        navHeader.add(phase);

        JLabel seasonContext = new JLabel("Week " + Math.max(0, leagueCore.currentWeek)
                + " / Year " + leagueCore.getYear());
        seasonContext.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        seasonContext.setForeground(DesktopTheme.textSecondary());
        seasonContext.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DesktopTheme.borderSubtle()),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        sidebar.add(navHeader, BorderLayout.NORTH);
        sidebar.add(navScroll, BorderLayout.CENTER);
        sidebar.add(seasonContext, BorderLayout.SOUTH);
        shell.add(sidebar, BorderLayout.WEST);

        mainCardLayout = new CardLayout();
        mainContentCards = new JPanel(mainCardLayout);
        mainContentCards.setOpaque(true);
        mainContentCards.setBackground(DesktopTheme.windowBackground());
        addScreenCard("Home", buildDashboardPanel());
        addScreenCard("Recruiting", buildRecruitingTab());
        addScreenCard("Standings", buildStandingsPanel());
        addScreenCard("Scoreboard", buildScoreboardPanel());
        addScreenCard("My Coach", buildCoachProfilePanel());
        addScreenCard("Poll Rankings", buildPollRankingsPanel());
        addScreenCard("Team Rankings", buildTeamRankingsPanel());
        addScreenCard("Player Stats", buildPlayerRankingsPanel());
        addScreenCard("Player Search", buildPlayerSearchPanel());
        addScreenCard("League History", buildLeagueHistoryPanel());
        addScreenCard("News", buildNewsPanel());
        addScreenCard("Coaches", buildCoachDatabasePanel());
        addScreenCard("Hall of Fame", buildHallOfFamePanel());
        addScreenCard("Records", buildLeagueRecordsPanel());
        addScreenCard("Settings", buildSettingsPanel());
        shell.add(mainContentCards, BorderLayout.CENTER);
        selectScreen(selectedScreen);
        return shell;
    }

    private JList<String> buildNavigationList() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String title : NAV_TITLES) {
            model.addElement(title);
        }
        JList<String> list = new JList<>(model);
        list.setFixedCellHeight(34);
        list.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(DesktopTheme.sidebarBackground());
        list.setForeground(DesktopTheme.sidebarText());
        list.setSelectionBackground(DesktopTheme.sidebarSelectionBackground());
        list.setSelectionForeground(Color.WHITE);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
                label.setOpaque(true);
                label.setBackground(isSelected ? DesktopTheme.sidebarSelectionBackground() : DesktopTheme.sidebarBackground());
                label.setForeground(isSelected ? Color.WHITE : DesktopTheme.sidebarText());
                return label;
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String value = list.getSelectedValue();
                if (value != null) {
                    selectScreen(value);
                }
            }
        });
        return list;
    }

    private void addScreenCard(String title, Component component) {
        mainContentCards.add(component, title);
    }

    private void selectScreen(String title) {
        if (!isKnownScreen(title)) {
            title = "Home";
        }
        selectedScreen = title;
        if (mainCardLayout != null && mainContentCards != null) {
            mainCardLayout.show(mainContentCards, title);
        }
        if (navigationList != null && !title.equals(navigationList.getSelectedValue())) {
            navigationList.setSelectedValue(title, true);
        }
    }

    private static boolean isKnownScreen(String title) {
        for (String navTitle : NAV_TITLES) {
            if (navTitle.equals(title)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Recruiting tab (docked board at NLI week)
    // =========================================================================

    private JPanel buildRecruitingTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(true);
        outer.setBackground(DesktopTheme.windowBackground());
        outer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (bridge.isAwaitingDockedRecruiting()) {
            RecruitingPanel panel = new RecruitingPanel(leagueCore, data -> SwingUtilities.invokeLater(() -> {
                bridge.completeDockedRecruiting(data);
                markDirty();
                if (bridge.isNewSeasonPending()) {
                    startNewSeason();
                } else {
                    refresh();
                }
            }));
            outer.add(panel, BorderLayout.CENTER);
            return outer;
        }

        JLabel info = new JLabel();
        info.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        String html;
        if (leagueCore.userTeam == null) {
            html = "<html><div style='width:520px'><b>No program selected.</b><br><br>"
                    + "Start or load a career with a team to run recruiting here.</div></html>";
        } else if (!leagueCore.userTeam.isUserControlled()) {
            html = "<html><div style='width:520px'>Recruiting is only available for user-controlled schools.</div></html>";
        } else if (leagueCore.currentWeek >= leagueCore.regSeasonWeeks + 13) {
            html = "<html><div style='width:520px'><b>NLI week is active.</b><br><br>"
                    + "Press <b>Space</b> or use the header <b>Play</b> button once to load the signing board here. "
                    + "CPU programs are processed when you do.</div></html>";
        } else {
            html = "<html><div style='width:520px'>The interactive board opens here after realignment when you advance into "
                    + "<b>signing week</b>. Use <b>Season \u2192 Show Recruiting</b> any time to jump back to this page."
                    + "</div></html>";
        }
        info.setText(html);
        info.setForeground(DesktopTheme.textPrimary());
        outer.add(info, BorderLayout.NORTH);
        return outer;
    }

    // =========================================================================
    // Standings tab
    // =========================================================================

    private JSplitPane buildStandingsPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);
        splitPane.setOpaque(true);
        splitPane.setBackground(DesktopTheme.windowBackground());
        splitPane.setLeftComponent(buildTopTeamsSidebar());
        JScrollPane gridScroll = new JScrollPane(buildConferenceGrid());
        gridScroll.getViewport().setBackground(DesktopTheme.windowBackground());
        gridScroll.setOpaque(true);
        splitPane.setRightComponent(gridScroll);
        return splitPane;
    }

    private JPanel buildTopTeamsSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(sidebar);
        sidebar.setBorder(DesktopTheme.titledBorder("Top 25 (Poll)"));

        DefaultListModel<Team> teamModel = new DefaultListModel<>();
        leagueCore.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(25)
                .forEach(teamModel::addElement);

        JList<Team> teamList = new JList<>(teamModel);
        teamList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleListShell(teamList);
        teamList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Team t = (Team) value;
                String label = String.format("#%-3d %-22s (%d-%d)  Pres %d",
                        t.getRankTeamPollScore(), t.getName(), t.getWins(), t.getLosses(), t.getTeamPrestige());
                Component c = super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                if (!(c instanceof JLabel jl)) {
                    return c;
                }
                jl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    DesktopTheme.decorateListCellLabel(jl, index, true, null);
                } else if (t == leagueCore.userTeam) {
                    DesktopTheme.decorateListCellLabel(jl, index, false, DesktopTheme.userTeamRowTint());
                } else {
                    DesktopTheme.decorateListCellLabel(jl, index, false, null);
                }
                return jl;
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

        JScrollPane teamScroll = new JScrollPane(teamList);
        teamScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        teamScroll.setOpaque(true);
        sidebar.add(teamScroll, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildConferenceGrid() {
        JPanel content = new JPanel(new GridLayout(0, 2, 10, 10));
        content.setOpaque(true);
        content.setBackground(DesktopTheme.windowBackground());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (Conference conf : leagueCore.getConferences()) {
            content.add(buildConferencePanel(conf));
        }
        return content;
    }

    private JPanel buildConferencePanel(Conference conf) {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createLineBorder(DesktopTheme.borderSubtle()));

        // Conference header with TV info
        String headerText = " " + conf.confName;
        if (conf.confTV) {
            headerText += "  (" + conf.getTVName() + ")";
        }
        JLabel label = new JLabel(headerText);
        label.setOpaque(true);
        label.setBackground(DesktopTheme.conferenceHeaderBackground());
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

        javax.swing.table.TableCellRenderer confRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!(c instanceof JLabel jl)) {
                    return c;
                }
                jl.setOpaque(true);
                jl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                if (isSelected) {
                    c.setBackground(DesktopTheme.selectionAccent());
                    c.setForeground(java.awt.Color.WHITE);
                    return c;
                }
                String name = (String) tbl.getValueAt(row, 1);
                if (leagueCore.userTeam != null && leagueCore.userTeam.getName().equals(name)) {
                    c.setBackground(DesktopTheme.userTeamRowTint());
                } else {
                    c.setBackground(row % 2 == 0 ? DesktopTheme.tableBase() : DesktopTheme.tableStripe());
                }
                c.setForeground(DesktopTheme.textPrimary());
                return c;
            }
        };
        confTable.setDefaultRenderer(Object.class, confRenderer);
        confTable.setDefaultRenderer(Integer.class, confRenderer);

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
        JScrollPane confScroll = new JScrollPane(confTable);
        DesktopTheme.styleDataTableInScroll(confScroll, confTable);
        panel.add(confScroll, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Poll Rankings tab (Top 25 with full details)
    // =========================================================================

    private JPanel buildPollRankingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
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
        StripedRowRenderer.install(table);
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

        JScrollPane pollScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(pollScroll, table);
        panel.add(pollScroll, BorderLayout.CENTER);
        JLabel pollFoot = new JLabel("Double-click a team for details. Teams are sortable by column.");
        pollFoot.setForeground(DesktopTheme.textSecondary());
        panel.add(pollFoot, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Team Rankings tab (23 categories via combo box)
    // =========================================================================

    private JPanel buildTeamRankingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
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
        StripedRowRenderer.install(table);
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
                java.util.List<String> rankings = leagueCore.getTeamRankingsStr(sel);
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
        DesktopTheme.styleToolbar(topBar);
        panel.add(topBar, BorderLayout.NORTH);
        JScrollPane teamRankScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(teamRankScroll, table);
        panel.add(teamRankScroll, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Player Rankings tab (22 stat categories via League.getPlayerRankStr)
    // =========================================================================

    private JPanel buildPlayerRankingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
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
        StripedRowRenderer.install(table);

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
        DesktopTheme.styleToolbar(topBar);
        panel.add(topBar, BorderLayout.NORTH);
        JScrollPane playerRankScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(playerRankScroll, table);
        panel.add(playerRankScroll, BorderLayout.CENTER);
        JLabel prHint = new JLabel(PLAYER_RANKINGS_HINT);
        prHint.setForeground(DesktopTheme.textSecondary());
        panel.add(prHint, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Player Search tab (League-wide search and filter)
    // =========================================================================

    private JPanel buildPlayerSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(buildScreenHeader("Player Search", "Find players across every roster by name, position, and class."), BorderLayout.NORTH);

        // Filters
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.add(new JLabel("Name:"));
        javax.swing.JTextField nameField = new javax.swing.JTextField(12);
        nameField.setToolTipText("Type any part of a player's name.");
        filterBar.add(nameField);

        filterBar.add(new JLabel("Position:"));
        String[] positions = {"ALL", "QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"};
        JComboBox<String> posBox = new JComboBox<>(positions);
        filterBar.add(posBox);

        filterBar.add(new JLabel("Year:"));
        String[] years = {"ALL", "FR", "SO", "JR", "SR"};
        JComboBox<String> yearBox = new JComboBox<>(years);
        filterBar.add(yearBox);

        JButton searchBtn = new JButton("Search");
        filterBar.add(searchBtn);
        JLabel resultCount = new JLabel();
        resultCount.setForeground(DesktopTheme.textSecondary());
        filterBar.add(resultCount);
        DesktopTheme.styleToolbar(filterBar);

        topPanel.add(filterBar, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {"Name", "Pos", "Team", "Year", "OVR", "Pot"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 4 || col == 5) return Integer.class;
                return String.class;
            }
        };
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);

        // Double-click to open details
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String name = (String) table.getValueAt(row, 0);
                        String teamName = (String) table.getValueAt(row, 2);
                        Team t = liveTeamMap.get(teamName);
                        if (t != null) {
                            Player p = findPlayerByName(t, name);
                            if (p != null) PlayerDetailView.show(LeagueHomeView.this, p);
                        }
                    }
                }
            }
        });

        JScrollPane searchScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(searchScroll, table);
        panel.add(searchScroll, BorderLayout.CENTER);

        Runnable runSearch = () -> {
            String query = nameField.getText().toLowerCase().trim();
            String posFilter = (String) posBox.getSelectedItem();
            String yearFilter = (String) yearBox.getSelectedItem();
            int yearInt = -1;
            if ("FR".equals(yearFilter)) yearInt = 1;
            else if ("SO".equals(yearFilter)) yearInt = 2;
            else if ("JR".equals(yearFilter)) yearInt = 3;
            else if ("SR".equals(yearFilter)) yearInt = 4;

            model.setRowCount(0);
            List<Team> allTeams = leagueCore.getTeamList();
            for (Team t : allTeams) {
                for (Player p : t.getAllPlayers()) {
                    if (!query.isEmpty() && !p.name.toLowerCase().contains(query)) continue;
                    if (!"ALL".equals(posFilter) && !p.position.equals(posFilter)) continue;
                    if (yearInt != -1 && p.year != yearInt) continue;

                    model.addRow(new Object[]{
                            p.name, p.position, t.getName(), formatYear(p.year), p.ratOvr, p.ratPot
                    });
                }
            }
            resultCount.setText(model.getRowCount() + " players");
        };

        searchBtn.addActionListener(e -> runSearch.run());
        nameField.addActionListener(e -> runSearch.run()); // Search on Enter
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { runSearch.run(); }
            @Override public void removeUpdate(DocumentEvent e) { runSearch.run(); }
            @Override public void changedUpdate(DocumentEvent e) { runSearch.run(); }
        });
        posBox.addActionListener(e -> runSearch.run());
        yearBox.addActionListener(e -> runSearch.run());
        runSearch.run();

        return panel;
    }

    private String formatYear(int year) {
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

    private Player findPlayerByName(Team team, String name) {
        if (team == null || name == null) return null;
        for (Player p : team.getAllPlayers()) {
            if (name.equals(p.name)) return p;
        }
        return null;
    }

    // =========================================================================
    // League History tab (season champions + all-time team stats)
    // =========================================================================

    private JPanel buildLeagueHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
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
        DesktopTheme.styleTextContent(historyArea);
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyScroll.setBorder(DesktopTheme.titledBorder("Season Champions"));
        historyScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        historyScroll.setOpaque(true);
        historyScroll.setPreferredSize(new Dimension(0, 200));

        // Bottom: all-time team stats with category selector
        JPanel statsPanel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(statsPanel);
        statsPanel.setBorder(DesktopTheme.titledBorder("All-Time Team Records"));

        JComboBox<String> categoryBox = new JComboBox<>(LEAGUE_HISTORY_STAT_CATEGORIES);
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 13));

        String[] columns = {"Rank", "Team", "Total"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        StripedRowRenderer.install(table);
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
        DesktopTheme.styleToolbar(topBar);
        statsPanel.add(topBar, BorderLayout.NORTH);
        JScrollPane histStatsScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(histStatsScroll, table);
        statsPanel.add(histStatsScroll, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, historyScroll, statsPanel);
        split.setDividerLocation(220);
        split.setOpaque(true);
        split.setBackground(DesktopTheme.windowBackground());
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Scoreboard tab (with week navigation)
    // =========================================================================

    private JPanel buildScoreboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel(new String[]{"Matchup", "Result", "Type"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        StripedRowRenderer.install(table);

        // Week navigation controls
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(buildScreenHeader("Scoreboard", "Browse completed weeks and open box scores."), BorderLayout.NORTH);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton prevBtn = new JButton("\u25C0 Previous");
        JButton nextBtn = new JButton("Next \u25B6");
        JButton currentBtn = new JButton("Current Week");
        JLabel weekLabel = new JLabel();
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        weekLabel.setForeground(DesktopTheme.textPrimary());

        Runnable updateScoreboard = () -> {
            weekLabel.setText(scoreboardWeek <= 0 ? "Pre-Season" : "Week " + scoreboardWeek);
            model.setRowCount(0);
            java.util.List<java.util.List<String>> scores = leagueCore.getWeeklyScores();
            if (scores != null && scoreboardWeek >= 0 && scoreboardWeek < scores.size()) {
                for (String s : scores.get(scoreboardWeek)) {
                    if (s == null) continue;
                    String[] parts = s.split(",");
                    if (parts.length >= 3) model.addRow(new Object[]{parts[0], parts[1], parts[2]});
                    else if (parts.length == 1) model.addRow(new Object[]{parts[0], "", "Game"});
                }
            }
            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{"No recorded games for this week.", "", ""});
            }
            prevBtn.setEnabled(scoreboardWeek > 0);
            nextBtn.setEnabled(scoreboardWeek < leagueCore.currentWeek);
            currentBtn.setEnabled(scoreboardWeek != leagueCore.currentWeek);
        };

        prevBtn.addActionListener(e -> { if(scoreboardWeek > 0) { scoreboardWeek--; updateScoreboard.run(); } });
        nextBtn.addActionListener(e -> { if(scoreboardWeek < leagueCore.currentWeek) { scoreboardWeek++; updateScoreboard.run(); } });
        currentBtn.addActionListener(e -> { scoreboardWeek = Math.max(0, leagueCore.currentWeek); updateScoreboard.run(); });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String matchup = String.valueOf(table.getValueAt(row, 0));
                        if (matchup.contains(" at ")) {
                            showBoxScoreFromMatchup(matchup);
                        }
                    }
                }
            }
        });

        navPanel.add(prevBtn);
        navPanel.add(weekLabel);
        navPanel.add(nextBtn);
        navPanel.add(currentBtn);
        DesktopTheme.styleToolbar(navPanel);

        updateScoreboard.run();

        topPanel.add(navPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);
        JScrollPane scoreScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(scoreScroll, table);
        panel.add(scoreScroll, BorderLayout.CENTER);
        JLabel scoreHint = new JLabel("Double-click any game to view the box score.");
        scoreHint.setForeground(DesktopTheme.textSecondary());
        panel.add(scoreHint, BorderLayout.SOUTH);
        return panel;
    }

    private void showBoxScoreFromMatchup(String matchup) {
        if (matchup == null || !matchup.contains(" at ")) return;
        // Matchup usually looks like "Team A 24 at Team B 31   Final"
        String[] parts = matchup.split(" at ");
        if (parts.length < 2) return;
        String teamA = parts[0].replaceAll("\\d+", "").trim();
        String teamH = parts[1].split("   ")[0].replaceAll("\\d+", "").trim();

        Team away = liveTeamMap.get(teamA);
        Team home = liveTeamMap.get(teamH);

        if (away != null && home != null) {
            for (Game g : away.getGameSchedule()) {
                if (g.homeTeam == home || g.awayTeam == home) {
                    GameBoxScoreView.show(this, g, leagueCore.userTeam);
                    return;
                }
            }
        }
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
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultListModel<String> headlineModel = new DefaultListModel<>();

        JTextArea storyArea = new JTextArea("Select a headline to read the full story.");
        storyArea.setEditable(false);
        storyArea.setLineWrap(true);
        storyArea.setWrapStyleWord(true);
        storyArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleTextContent(storyArea);

        JList<String> headlineList = new JList<>(headlineModel);
        headlineList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        DesktopTheme.styleListShell(headlineList);
        headlineList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                String fg = isSelected ? "rgb(255,255,255)" : DesktopTheme.cssRgb(DesktopTheme.textPrimary());
                l.setText("<html><body style='color:" + fg + ";'>"
                        + DesktopTheme.escapeForHtml(value.toString()) + "</body></html>");
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(buildScreenHeader("News", "Review weekly headlines and league storylines."), BorderLayout.NORTH);

        // Week navigation controls for news archive
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton prevBtn = new JButton("\u25C0 Prev Week");
        JButton nextBtn = new JButton("Next Week \u25B6");
        JButton latestBtn = new JButton("Latest");
        JLabel weekLabel = new JLabel();
        weekLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        weekLabel.setForeground(DesktopTheme.textPrimary());

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
                storyArea.setText("No stories were generated for this week.");
            } else {
                storyArea.setText("Select a headline to read the full story.");
            }
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
        DesktopTheme.styleToolbar(navPanel);
        topPanel.add(navPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

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

        JScrollPane headScroll = new JScrollPane(headlineList);
        headScroll.setBorder(DesktopTheme.titledBorder("Headlines"));
        headScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        headScroll.setOpaque(true);
        JScrollPane storyScroll = new JScrollPane(storyArea);
        storyScroll.setBorder(DesktopTheme.titledBorder("Story"));
        storyScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        storyScroll.setOpaque(true);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, headScroll, storyScroll);
        split.setDividerLocation(320);
        split.setOpaque(true);
        split.setBackground(DesktopTheme.windowBackground());
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
        DesktopTheme.styleTabRoot(panel);
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
        StripedRowRenderer.install(table);

        Runnable loadCoaches = () -> {
            int sel = categoryBox.getSelectedIndex();
            model.setRowCount(0);
            try {
                java.util.List<String> rankings = leagueCore.getCoachDatabase(sel);
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
        DesktopTheme.styleToolbar(topBar);
        panel.add(topBar, BorderLayout.NORTH);
        JScrollPane coachDbScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(coachDbScroll, table);
        panel.add(coachDbScroll, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Hall of Fame tab
    // =========================================================================

    private JPanel buildHallOfFamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        List<PlayerRecord> hof = currentRecord.leagueHoF();
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
                                        DesktopTheme.messageForDialog(
                                        pr.name() + "  (" + pr.position() + ")\n"
                                                + "Team: " + pr.teamName() + "\n"
                                                + "Overall: " + pr.ratOvr()),
                                        "Hall of Fame — " + pr.name(),
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

        JPanel bottom = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(bottom);
        JLabel count = new JLabel(hof.size() + " inductees", JLabel.RIGHT);
        count.setForeground(DesktopTheme.textSecondary());
        count.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        bottom.add(count, BorderLayout.EAST);
        JLabel hofHint = new JLabel("Double-click a player for details.");
        hofHint.setForeground(DesktopTheme.textSecondary());
        bottom.add(hofHint, BorderLayout.WEST);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // League Records tab
    // =========================================================================

    private JPanel buildLeagueRecordsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = {"Record", "Value", "Holder", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (DataRecord dr : currentRecord.leagueRecords()) {
            model.addRow(new Object[]{dr.key(), formatValue(dr.value()), formatHolder(dr.holder()), dr.year()});
        }
        JTable table = new JTable(model);
        table.setRowHeight(22);
        StripedRowRenderer.install(table);
        JScrollPane recordsScroll = new JScrollPane(table);
        DesktopTheme.styleDataTableInScroll(recordsScroll, table);
        panel.add(recordsScroll, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // Home Dashboard tab
    // =========================================================================

    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(true);
        panel.setBackground(DesktopTheme.windowBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        if (leagueCore.userTeam != null
                && leagueCore.currentWeek >= leagueCore.regSeasonWeeks + 13) {
            JPanel alert = new JPanel(new BorderLayout());
            alert.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DesktopTheme.nliBannerBorder(), 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            alert.setBackground(DesktopTheme.nliBannerBackground());
            JLabel nli = new JLabel("<html><b>National Letter of Intent period</b><br>"
                    + "Press <b>Space</b> or <b>Season \u2192 Play Next Week</b> once to load the board into the "
                    + "<b>Recruiting</b> tab, then click <b>Finish Recruiting</b> there to advance the league.</html>");
            nli.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            nli.setForeground(DesktopTheme.textPrimary());
            alert.add(nli, BorderLayout.CENTER);
            panel.add(alert, BorderLayout.NORTH);
        }

        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(true);
        grid.setBackground(DesktopTheme.windowBackground());

        // Left column: Season Progress & Headlines
        JPanel left = new JPanel(new BorderLayout(0, 12));
        left.setOpaque(true);
        left.setBackground(DesktopTheme.windowBackground());
        JPanel progress = new JPanel(new GridLayout(0, 1, 4, 4));
        progress.setOpaque(false);
        progress.setBorder(DesktopTheme.titledBorder("Season Status"));
        JLabel p1 = new JLabel("Current Period: " + decodeSeasonPeriod());
        p1.setForeground(DesktopTheme.textPrimary());
        progress.add(p1);
        JLabel p2 = new JLabel("League Year: " + leagueCore.getYear());
        p2.setForeground(DesktopTheme.textPrimary());
        progress.add(p2);
        JLabel p3 = new JLabel("Active Conferences: " + leagueCore.getConferences().size());
        p3.setForeground(DesktopTheme.textPrimary());
        progress.add(p3);
        left.add(progress, BorderLayout.NORTH);

        JPanel news = new JPanel(new BorderLayout());
        news.setOpaque(true);
        news.setBackground(DesktopTheme.windowBackground());
        news.setBorder(DesktopTheme.titledBorder("Latest Headlines"));
        DefaultListModel<String> newsModel = new DefaultListModel<>();
        if (leagueCore.getNewsHeadlines() != null) {
            leagueCore.getNewsHeadlines().stream().limit(10).forEach(newsModel::addElement);
        }
        if (newsModel.isEmpty()) {
            newsModel.addElement("No headlines yet. Advance the week to generate league news.");
        }
        JList<String> newsList = new JList<>(newsModel);
        newsList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        newsList.setVisibleRowCount(6);
        DesktopTheme.styleListShell(newsList);
        newsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                String fg = isSelected ? "rgb(255,255,255)" : DesktopTheme.cssRgb(DesktopTheme.textPrimary());
                l.setText("<html><body style='width:250px;color:" + fg + ";'>\u2022 "
                        + DesktopTheme.escapeForHtml(value.toString()) + "</body></html>");
                DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                return l;
            }
        });
        JScrollPane dashNewsScroll = new JScrollPane(newsList);
        dashNewsScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        dashNewsScroll.setOpaque(true);
        news.add(dashNewsScroll, BorderLayout.CENTER);
        left.add(news, BorderLayout.CENTER);

        // Right column: Top 5 & Heisman Race
        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(true);
        right.setBackground(DesktopTheme.windowBackground());
        JPanel top5 = new JPanel(new GridLayout(0, 1, 4, 2));
        top5.setOpaque(true);
        top5.setBackground(DesktopTheme.windowBackground());
        top5.setBorder(DesktopTheme.titledBorder("Poll Leaders"));
        leagueCore.getTeamList().stream()
                .sorted(Comparator.comparingInt(Team::getRankTeamPollScore))
                .limit(5)
                .forEach(t -> {
                    JLabel l = new JLabel(String.format(" #%d  %-20s  (%d-%d)",
                            t.getRankTeamPollScore(), t.getName(), t.getWins(), t.getLosses()));
                    l.setFont(new Font("SansSerif", Font.BOLD, 13));
                    l.setOpaque(true);
                    l.setBackground(DesktopTheme.pollLeaderCard());
                    l.setForeground(DesktopTheme.textPrimary());
                    l.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                    top5.add(l);
                });
        right.add(top5, BorderLayout.NORTH);

        JPanel awards = new JPanel(new BorderLayout());
        awards.setOpaque(true);
        awards.setBackground(DesktopTheme.windowBackground());
        awards.setBorder(DesktopTheme.titledBorder("Awards Race"));
        JTextArea awardsArea = new JTextArea();
        awardsArea.setEditable(false);
        String awardsText = leagueCore.getHeismanWinnerStrFull();
        if (awardsText == null || awardsText.trim().isEmpty()) {
            awardsText = "Awards tracking appears once the season has enough statistics.";
        }
        awardsArea.setText(awardsText);
        DesktopTheme.styleTextContent(awardsArea);
        JScrollPane awardsScroll = new JScrollPane(awardsArea);
        awardsScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        awards.add(awardsScroll, BorderLayout.CENTER);
        right.add(awards, BorderLayout.CENTER);

        grid.add(left);
        grid.add(right);
        panel.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(true);
        bottom.setBackground(DesktopTheme.windowBackground());
        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.setBorder(DesktopTheme.titledBorder("Quick navigation"));
        quick.add(mkNavButton("Standings"));
        quick.add(mkNavButton("Scoreboard"));
        quick.add(mkNavButton("Poll Rankings"));
        quick.add(mkNavButton("Player Stats"));
        quick.add(mkNavButton("News"));
        quick.add(mkNavButton("Recruiting"));
        if (leagueCore.userTeam != null) {
            JButton my = new JButton("My Program");
            my.setToolTipText("Roster, depth chart, and team tools (Ctrl+U)");
            my.addActionListener(e -> openUserTeamDetail());
            quick.add(my);
        }
        DesktopTheme.styleToolbar(quick);
        bottom.add(quick, BorderLayout.NORTH);

        JLabel hint = new JLabel("Welcome back, Coach. Use the league office navigation at left to explore the league. F1 for shortcuts.");
        hint.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 12));
        hint.setForeground(DesktopTheme.textSecondary());
        bottom.add(hint, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JButton mkNavButton(String tabTitle) {
        JButton b = new JButton(tabTitle);
        b.setToolTipText("Open " + tabTitle);
        b.addActionListener(e -> selectScreen(tabTitle));
        return b;
    }

    private String decodeSeasonPeriod() {
        int wk = leagueCore.currentWeek;
        int reg = leagueCore.regSeasonWeeks;
        if (wk <= 0) return "Pre-Season";
        if (wk <= reg) return "Regular Season";
        if (wk <= reg + 3) return "Post-Season";
        return "Off-Season";
    }

    // =========================================================================
    // My Coach tab
    // =========================================================================

    private JPanel buildCoachProfilePanel() {
        if (leagueCore.userTeam == null) {
            JPanel empty = new JPanel();
            DesktopTheme.styleTabRoot(empty);
            return empty;
        }
        Team ut = leagueCore.userTeam;
        staff.HeadCoach hc = ut.getHeadCoach();

        JPanel panel = new JPanel(new BorderLayout(16, 16));
        DesktopTheme.styleTabRoot(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new GridLayout(0, 2, 10, 6));
        header.setOpaque(true);
        header.setBackground(DesktopTheme.windowBackground());
        header.setBorder(DesktopTheme.titledBorder("Coach Career"));
        header.add(new JLabel("Name:")); header.add(new JLabel(hc.name, JLabel.LEFT) {{ setFont(new Font("SansSerif", Font.BOLD, 14)); }});
        header.add(new JLabel("Current Team:")); header.add(new JLabel(ut.getName()));
        header.add(new JLabel("Experience:")); header.add(new JLabel(hc.year + " years"));
        header.add(new JLabel("Career Record:")); header.add(new JLabel(hc.getWins() + "-" + hc.getLosses()));
        header.add(new JLabel("Championships:")); header.add(new JLabel(String.valueOf(hc.getNCWins())));
        DesktopTheme.styleLabelsDeep(header, DesktopTheme.textPrimary());

        JPanel attrs = new JPanel(new GridLayout(0, 2, 10, 6));
        attrs.setOpaque(true);
        attrs.setBackground(DesktopTheme.windowBackground());
        attrs.setBorder(DesktopTheme.titledBorder("Coach Attributes"));
        attrs.add(new JLabel("Overall:")); attrs.add(new JLabel(String.valueOf(hc.ratOvr)));
        attrs.add(new JLabel("Offense:")); attrs.add(new JLabel(String.valueOf(hc.ratOff)));
        attrs.add(new JLabel("Defense:")); attrs.add(new JLabel(String.valueOf(hc.ratDef)));
        attrs.add(new JLabel("Recruiting:")); attrs.add(new JLabel(String.valueOf(hc.ratTalent)));
        attrs.add(new JLabel("Discipline:")); attrs.add(new JLabel(String.valueOf(hc.ratDiscipline)));
        DesktopTheme.styleLabelsDeep(attrs, DesktopTheme.textPrimary());

        panel.add(header, BorderLayout.NORTH);
        panel.add(attrs, BorderLayout.CENTER);

        if (!hc.history.isEmpty()) {
            JTextArea hist = new JTextArea("History:\n\n");
            for (String s : hc.history) hist.append("  \u2022 " + s + "\n");
            DesktopTheme.styleTextContent(hist);
            JScrollPane histScroll = new JScrollPane(hist);
            histScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
            histScroll.setOpaque(true);
            panel.add(histScroll, BorderLayout.SOUTH);
        }

        return panel;
    }

    // =========================================================================
    // Settings tab
    // =========================================================================

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("League Settings");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(DesktopTheme.textPrimary());
        JLabel subtitle = new JLabel("Review active universe rules and open the settings dialog for changes.");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(DesktopTheme.textSecondary());
        JPanel titleBlock = new JPanel(new GridLayout(0, 1, 0, 2));
        titleBlock.setOpaque(false);
        titleBlock.add(title);
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.CENTER);
        JButton editTop = new JButton("Edit Settings...");
        editTop.addActionListener(e -> openSettingsDialog());
        header.add(editTop, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JPanel summary = new JPanel(new GridLayout(0, 2, 10, 8));
        summary.setOpaque(false);
        summary.setBorder(DesktopTheme.titledBorder("Active Options"));
        addOptionRow(summary, "Desktop theme", DesktopTheme.isDark() ? "Dark" : "Light");
        addOptionRow(summary, "Player potential", enabledLabel(leagueCore.showPotential));
        addOptionRow(summary, "Full game logs", enabledLabel(leagueCore.fullGameLog));
        addOptionRow(summary, "Game mode", leagueCore.careerMode ? "Career" : "Sandbox");
        addOptionRow(summary, "Never retire", enabledLabel(leagueCore.neverRetire));
        addOptionRow(summary, "TV contracts", enabledLabel(leagueCore.enableTV));
        addOptionRow(summary, "Expanded playoffs", enabledLabel(leagueCore.expPlayoffs));
        addOptionRow(summary, "Conference realignment", enabledLabel(leagueCore.confRealignment));
        addOptionRow(summary, "Advanced realignment", enabledLabel(leagueCore.advancedRealignment));
        addOptionRow(summary, "Promotion/relegation", enabledLabel(leagueCore.enableUnivProRel));
        panel.add(summary, BorderLayout.CENTER);

        JLabel note = new JLabel("<html><div style='width:620px'>Expanded playoffs lock once the regular season is underway. Promotion/relegation conversion is only available in Week 0. Save after applying changes to persist them.</div></html>");
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        note.setForeground(DesktopTheme.textSecondary());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(note);
        panel.add(actions, BorderLayout.SOUTH);

        DesktopTheme.styleLeagueSettingsPanel(panel);
        return panel;
    }

    private static void addOptionRow(JPanel panel, String label, String value) {
        JLabel left = new JLabel(label + ":");
        JLabel right = new JLabel(value);
        right.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        panel.add(left);
        panel.add(right);
    }

    private static String enabledLabel(boolean enabled) {
        return enabled ? "Enabled" : "Disabled";
    }

    // =========================================================================
    // Visual Polish: Striped Table Renderer
    // =========================================================================

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
            JPanel snapRoot = (JPanel) dialog.getContentPane();
            snapRoot.setOpaque(true);
            snapRoot.setBackground(DesktopTheme.windowBackground());

            DefaultListModel<LeagueRecord.TeamRecord> model = new DefaultListModel<>();
            record.conferences().stream()
                    .flatMap(c -> c.teams().stream())
                    .sorted(Comparator.comparingInt(LeagueRecord.TeamRecord::prestige).reversed())
                    .forEach(model::addElement);
            JList<LeagueRecord.TeamRecord> list = new JList<>(model);
            list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            DesktopTheme.styleListShell(list);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean isSelected, boolean cellHasFocus) {
                    LeagueRecord.TeamRecord t = (LeagueRecord.TeamRecord) value;
                    String label = "#" + (index + 1) + " " + t.name() + " (prestige " + t.prestige() + ")";
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);
                    l.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                    DesktopTheme.decorateListCellLabel(l, index, isSelected, null);
                    return l;
                }
            });
            JScrollPane snapScroll = new JScrollPane(list);
            snapScroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
            dialog.add(snapScroll, BorderLayout.CENTER);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        });
    }
}
