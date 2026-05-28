package desktop;

import positions.Player;
import recruiting.RecruitingSessionData;
import simulation.AudioEvent;
import simulation.AudioManager;
import simulation.Conference;
import simulation.League;
import simulation.LeagueExportController;
import simulation.LeagueLaunchCoordinator;
import simulation.LeagueRecord;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.SeasonPresentation;
import simulation.SeasonController;
import simulation.SimulationFacade;
import simulation.Team;
import simulation.TeamColors;

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
import javax.swing.JTextArea;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Graphical 'League Home' view for the desktop app. Displays standings,
 * poll rankings, team/player statistics, scoreboard with week navigation, news,
 * hall of fame, league records, and coach database.
 *
 * <p>The view uses {@link SeasonController} for all week advancement so the full
 * season/offseason/new-season loop works correctly without any Android dependencies.
 */
public class LeagueHomeView extends JFrame {

    private static final String TAG = "LeagueHomeView";
    private static final int HEADER_HEIGHT = 96;
    private static final String SAVE_EXTENSION = "cfb";
    /** Maximum simulation steps for advanceFullYear() before surfacing an error. */
    private static final int MAX_FULL_YEAR_STEPS = 200;

    private final League leagueCore;
    private LeagueRecord currentRecord;
    private File lastSavePath;

    /** Lookup from team name to live Team for O(1) access. */
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

    private static final String[] NAV_ICONS = {
            "\u2302", "\u2666", "\u2630", "\u25A0", "\u2605",
            "\u2191", "\u2261", "\u2637", "\u2318", "\u2609",
            "\u263C", "\u265A", "\u2606", "\u25C9", "\u2699"
    };

    private String selectedScreen = "Home";
    private JPanel mainContentCards;
    private CardLayout mainCardLayout;
    private JList<String> navigationList;
    private RecruitingSessionData activeRecruitingSession;

    /** Retained UI shells to avoid full frame rebuilds on refresh. */
    private JPanel headerPanel;
    private JPanel mainContentShell;
    private JPanel statusBar;

    private LeagueScreenContext screenContext;
    private final Map<String, LeagueScreen> screens = new java.util.LinkedHashMap<>();

    /** Per-screen component to focus when the user presses Ctrl+F. */
    private final Map<String, JComponent> screenFocusTargets = new HashMap<>();

    private JLabel statusLabel;
    private JLabel playedIndicator;
    private AudioManager audioManager;

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
                if (audioManager != null) {
                    audioManager.dispose();
                }
            }
        });
        setLayout(new BorderLayout());

        bridge = new DesktopUiBridge(this, leagueCore);
        controller = new SeasonController(leagueCore, bridge);
        audioManager = new DesktopAudioManager();
        scoreboardWeek = Math.max(0, leagueCore.currentWeek);

        screenContext = new LeagueScreenContext(leagueCore, currentRecord, liveTeamMap,
                audioManager, bridge, this, new LeagueScreenContext.Navigation() {
            @Override public void openTeamDetail(Team team) { openTeamDialogFromLive(team); }
            @Override public void openUserTeamDetail() { LeagueHomeView.this.openUserTeamDetail(); }
            @Override public void selectScreen(String title) { LeagueHomeView.this.selectScreen(title); }
        });
        registerScreens();

        loadApplicationIcon();
        registerGlobalShortcuts();
        setJMenuBar(buildMenuBar());
        headerPanel = buildHeader();
        add(headerPanel, BorderLayout.NORTH);
        mainContentShell = buildMainContent();
        add(mainContentShell, BorderLayout.CENTER);
        statusBar = buildStatusBar();
        add(statusBar, BorderLayout.SOUTH);
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
        DesktopTheme.applyWindowIcon(this);
    }

    private String buildWindowTitle() {
        String suffix = lastSavePath != null ? " \u2014 " + lastSavePath.getName() : "";
        String dirtyMark = dirty ? " *" : "";
        return "CFHC \u2014 " + currentRecord.leagueName() + " (" + currentRecord.year() + ")" + suffix + dirtyMark;
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

        JMenuItem playbookItem = new JMenuItem("Schemes...");
        playbookItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK));
        playbookItem.addActionListener(e -> showPlaybookDialog());
        playbookItem.setEnabled(leagueCore.userTeam != null);
        team.add(playbookItem);

        JMenuItem coachProgramItem = new JMenuItem("Coach Program & NIL\u2026");
        coachProgramItem.addActionListener(e -> CoachProgramDialog.show(this, leagueCore.userTeam));
        coachProgramItem.setEnabled(leagueCore.userTeam != null);
        team.add(coachProgramItem);

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

        JMenu audio = new JMenu("Audio");
        audio.setMnemonic(KeyEvent.VK_A);

        JCheckBoxMenuItem muteItem = new JCheckBoxMenuItem("Mute sounds", audioManager.isMuted());
        muteItem.addActionListener(e -> {
            audioManager.setMuted(muteItem.isSelected());
        });
        audio.add(muteItem);

        JMenuItem volItem = new JMenuItem("Volume\u2026");
        volItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK));
        volItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this,
                    "Enter volume (0-100):", "Sound Volume",
                    JOptionPane.QUESTION_MESSAGE);
            if (input != null) {
                try {
                    int pct = Integer.parseInt(input.trim());
                    audioManager.setVolume(Math.max(0, Math.min(100, pct)) / 100f);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        audio.add(volItem);

        bar.add(audio);

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

        // Ctrl+1..Ctrl+9 jump to the first nine sidebar screens.
        int[] digitKeys = {
                KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8,
                KeyEvent.VK_9
        };
        int navLimit = Math.min(digitKeys.length, NAV_TITLES.length);
        for (int i = 0; i < navLimit; i++) {
            final int index = i;
            getRootPane().registerKeyboardAction(
                    e -> selectScreen(NAV_TITLES[index]),
                    KeyStroke.getKeyStroke(digitKeys[i], KeyEvent.CTRL_DOWN_MASK),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        // Ctrl+F focuses the current screen's search/filter field; falls back to Player Search.
        getRootPane().registerKeyboardAction(
                e -> focusActiveSearchField(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * Focuses the search/filter component registered for the current screen.
     * Falls back to the Player Search tab if the active screen has none.
     */
    private void focusActiveSearchField() {
        JComponent target = screenFocusTargets.get(selectedScreen);
        if (target == null) {
            target = screenFocusTargets.get("Player Search");
            if (target != null) {
                selectScreen("Player Search");
            }
        }
        if (target != null) {
            final JComponent finalTarget = target;
            SwingUtilities.invokeLater(() -> {
                finalTarget.requestFocusInWindow();
                if (finalTarget instanceof javax.swing.text.JTextComponent tc) {
                    tc.selectAll();
                }
            });
        }
    }

    // =========================================================================
    // Header with user-team info
    // =========================================================================

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                DesktopTheme.paintHeaderGradient(g, getWidth(), getHeight(),
                    leagueCore.userTeam != null
                        ? TeamColors.primary(leagueCore.userTeam.getAbbr())
                        : null);
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(getWidth(), HEADER_HEIGHT));

        // Left: league title + optional user-team summary
        JPanel leftPanel = new JPanel(new GridLayout(0, 1));
        leftPanel.setOpaque(false);
        leftPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

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
        JLabel phase = new JLabel(decodeSeasonPeriod() + " - " + buildHeaderFocusText());
        phase.setForeground(new Color(200, 210, 220));
        phase.setFont(new Font("SansSerif", Font.PLAIN, 12));
        leftPanel.add(phase);
        header.add(leftPanel, BorderLayout.WEST);

        // Right: action buttons
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 23));
        controls.setOpaque(false);

        JButton playWeekBtn = new JButton(playWeekLabel());
        playWeekBtn.setToolTipText("Simulate the next week (Space)");
        playWeekBtn.addActionListener(e -> playWeek());
        playWeekBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        JButton advanceBtn = new JButton(bulkAdvanceLabel());
        advanceBtn.setToolTipText(bulkAdvanceTooltip());
        advanceBtn.addActionListener(e -> runBulkAdvanceFromHeader());
        advanceBtn.setEnabled(canBulkAdvanceFromHeader());
        advanceBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save the current league (Ctrl+S)");
        saveBtn.addActionListener(e -> saveLeague(false));
        saveBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        controls.add(playWeekBtn);
        controls.add(advanceBtn);
        controls.add(saveBtn);
        header.add(controls, BorderLayout.EAST);

        return header;
    }

    private String playWeekLabel() {
        return SeasonPresentation.getPlayWeekLabel(leagueCore.currentWeek, leagueCore.regSeasonWeeks);
    }

    private String bulkAdvanceLabel() {
        int week = leagueCore.currentWeek;
        int reg = leagueCore.regSeasonWeeks;
        if (week < reg) return "Sim to Postseason";
        if (week < reg + 13) return "Advance Offseason";
        return "Open Recruiting";
    }

    private String bulkAdvanceTooltip() {
        int week = leagueCore.currentWeek;
        int reg = leagueCore.regSeasonWeeks;
        if (week < reg) return "Advance through remaining regular-season weeks.";
        if (week < reg + 13) return "Advance offseason stages until recruiting begins.";
        return "Open the recruiting board.";
    }

    private boolean canBulkAdvanceFromHeader() {
        return !bridge.isAwaitingDockedRecruiting();
    }

    private void runBulkAdvanceFromHeader() {
        int week = leagueCore.currentWeek;
        int reg = leagueCore.regSeasonWeeks;
        if (week < reg) {
            simulateToPostSeason(reg);
        } else if (week < reg + 13) {
            advanceFullYear();
        } else {
            playWeek();
        }
    }

    private String buildHeaderFocusText() {
        if (bridge != null && bridge.isAwaitingDockedRecruiting()) {
            return "finish recruiting to start the next season";
        }
        int week = leagueCore.currentWeek;
        int reg = leagueCore.regSeasonWeeks;
        if (week >= reg + 13) return "sign your recruiting class";
        if (week >= reg + 4) return "work through offseason decisions";
        if (week >= reg) return "settle postseason games";
        if (week <= 0) return "review your roster and begin the year";
        return "play, review, and adjust";
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
        if (bridge != null && bridge.isAwaitingDockedRecruiting()) weekLabel = "Recruiting (Awaiting Signing)";
        else if (week >= reg + 13) weekLabel = "Recruiting";
        else if (week >= reg + 4) weekLabel = "Offseason";
        else if (week == reg + 3) weekLabel = "National Championship";
        else if (week == reg + 2) weekLabel = "Semifinals / Bowl Week 3";
        else if (week == reg + 1) weekLabel = "Quarterfinals / Bowl Week 2";
        else if (week == reg) weekLabel = "First Round / Bowl Week 1";
        else if (week == reg - 1) weekLabel = "Conf. Championships";
        else if (week == 0) weekLabel = "Pre-Season";
        else weekLabel = "Week " + week;

        int teams = currentRecord.conferences().stream().mapToInt(c -> c.teams().size()).sum();
        String base = String.format(Locale.ROOT, "%s  \u2022  %d conferences  \u2022  %d teams",
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
            // CANCEL; do nothing, stay open
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
            audioManager.play(AudioEvent.CONFIRM);
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
        audioManager.play(AudioEvent.ADVANCE);
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
        java.util.List<simulation.Game> schedule = leagueCore.userTeam.getGameSchedule();
        if (week >= 0 && week < schedule.size()) {
            g = schedule.get(week);
        }

        if (g != null && g.hasPlayed && !"BYE WEEK".equals(g.gameName)) {
            String opp = g.homeTeam == leagueCore.userTeam ? g.awayTeam.getAbbr() : g.homeTeam.getAbbr();
            String site = g.homeTeam == leagueCore.userTeam ? "vs " : "at ";
            int score = g.homeTeam == leagueCore.userTeam ? g.homeScore : g.awayScore;
            int oppScore = g.homeTeam == leagueCore.userTeam ? g.awayScore : g.homeScore;

            if (score > oppScore) {
                audioManager.play(AudioEvent.WIN);
            } else {
                audioManager.play(AudioEvent.LOSS);
            }

            String result = score > oppScore ? "WIN" : (score < oppScore ? "LOSS" : "TIE");
            
            String msg = String.format(Locale.ROOT, "Week %d Result:\n\n%s %s %s\nFinal Score: %d - %d\n\nRecord: %d-%d",
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
        activeRecruitingSession = null;
        bridge = new DesktopUiBridge(this, leagueCore);
        controller = new SeasonController(leagueCore, bridge);
        scoreboardWeek = 0;
        refresh();
        JOptionPane.showMessageDialog(this,
                DesktopTheme.messageForDialog(
                "Season " + leagueCore.getYear() + " is ready!\n"
                        + "Press Space or click the Play button to begin."),
                "New Season", JOptionPane.INFORMATION_MESSAGE);
    }

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
                int played = 0;
                try {
                    played = get();
                    PlatformLog.i(TAG, "Simulated " + played + " weeks.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    PlatformLog.w(TAG, "Season simulation interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    PlatformLog.e(TAG, "Season simulation failed.", e.getCause() != null ? e.getCause() : e);
                    JOptionPane.showMessageDialog(LeagueHomeView.this,
                            DesktopTheme.messageForDialog("Season simulation encountered an error:\n" + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())),
                            "Simulation Error", JOptionPane.ERROR_MESSAGE);
                }

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
        SimulationProgressDialog dialog = new SimulationProgressDialog(this, "Full-Year Simulation");
        dialog.setIndeterminate(true);
        dialog.setStatus("Advancing " + decodeSeasonPeriod() + "...");
        bridge.clearNewSeasonPending();

        javax.swing.SwingWorker<Integer, String> worker = new javax.swing.SwingWorker<>() {
            private final long start = System.currentTimeMillis();
            private boolean limitReached = false;
            private boolean cancelled = false;

            @Override
            protected Integer doInBackground() {
                int played = 0;
                while (!bridge.isNewSeasonPending()) {
                    if (dialog.isCancelled()) {
                        cancelled = true;
                        break;
                    }
                    if (bridge.isAwaitingDockedRecruiting()) {
                        break;
                    }
                    controller.advanceWeek();
                    played++;
                    publish("Advancing " + decodeSeasonPeriod() + " (Week " + leagueCore.currentWeek + ")");
                    if (played >= MAX_FULL_YEAR_STEPS) {
                        limitReached = true;
                        break;
                    }
                }
                return played;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                dialog.setStatus(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                dialog.dispose();
                int played = 0;
                try {
                    played = get();
                } catch (InterruptedException ie) {
                    PlatformLog.w(TAG, "Full-year simulation interrupted while collecting result.", ie);
                    return;
                } catch (java.util.concurrent.ExecutionException ee) {
                    Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                    PlatformLog.e(TAG, "Full-year simulation failed.", cause);
                    return;
                }

                if (played > 0) {
                    markDirty();
                }

                PlatformLog.i(TAG, "Advanced full year (" + played + " steps) in "
                        + (System.currentTimeMillis() - start) + "ms");

                if (limitReached) {
                    JOptionPane.showMessageDialog(LeagueHomeView.this,
                            DesktopTheme.messageForDialog(
                                    "Simulation stopped after " + MAX_FULL_YEAR_STEPS + " steps without completing the season.\n"
                                            + "This may indicate a simulation bug. Save your league and report the issue."),
                            "Simulation Limit Reached", JOptionPane.WARNING_MESSAGE);
                } else if (cancelled) {
                    JOptionPane.showMessageDialog(LeagueHomeView.this,
                            DesktopTheme.messageForDialog("Full-year simulation was interrupted."),
                            "Simulation Interrupted",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                if (bridge.isNewSeasonPending()) {
                    startNewSeason();
                    return;
                }

                scoreboardWeek = leagueCore.currentWeek;
                refresh();
                if (bridge.isAwaitingDockedRecruiting()) {
                    selectRecruitingTab();
                    JOptionPane.showMessageDialog(LeagueHomeView.this,
                            DesktopTheme.messageForDialog(
                                    "Bulk advance stopped at recruiting.\n"
                                            + "Open Recruiting, click Finish Recruiting, then use Play Week or Save."),
                            "Recruiting",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };

        worker.execute();
        dialog.setVisible(true);
    }

    private void selectRecruitingTab() {
        selectScreen("Recruiting");
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
            chooser.setFileFilter(new FileNameExtensionFilter("CFHC save (*." + SAVE_EXTENSION + ")", SAVE_EXTENSION));
            chooser.setSelectedFile(new File(suggestedFilename()));
            int result = chooser.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            target = chooser.getSelectedFile();
            if (!target.getName().toLowerCase(Locale.ROOT).endsWith("." + SAVE_EXTENSION)) {
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
            audioManager.play(AudioEvent.CONFIRM);
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
                "CFHC save (*." + SAVE_EXTENSION + ", *.txt)",
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
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "cfhc_import");
        try {
            // Create temp files for parsed output
            if (!tempDir.exists()) tempDir.mkdirs();
            File confFile = new File(tempDir, "conferences.txt");
            File teamsFile = new File(tempDir, "teams.txt");
            File bowlsFile = new File(tempDir, "bowls.txt");

            java.io.FileInputStream fis = new java.io.FileInputStream(importFile);
            try {
            simulation.CustomUniverseParser.parse(fis, confFile, teamsFile, bowlsFile);
            } finally {
                fis.close();
            }

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
                    "No user team selected. Start a new game with a team to access schemes."),
                    "No Team", JOptionPane.WARNING_MESSAGE);
            return;
        }
        audioManager.play(AudioEvent.PLAY_SELECT);
        PlaybookDialog.show(this, leagueCore.userTeam);
    }

    // =========================================================================
    // View menu dialogs
    // =========================================================================

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                DesktopTheme.messageForDialog(
                "College Football Head Coach (CFHC) \u2014 Desktop\n"
                        + "Portable Java build of the College Football Head Coach simulation.\n\n"
                        + "Press F1 or Ctrl+/ for the full shortcut list."),
                "About CFHC",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showKeyboardShortcuts() {
        String text = """
                Navigation & windows
                  F1, Ctrl+/     This shortcut list
                  Ctrl+1..9      Jump to sidebar tab (1=Home, 2=Recruiting, ...)
                  Ctrl+F         Focus filter on current tab (or open Player Search)
                  Ctrl+U         My Program (roster, depth chart, facilities)
                  Ctrl+P         Schemes
                  Ctrl+,         Settings

                Season
                  Space          Play next week / advance phase
                  Ctrl+A         Simulate regular season through postseason
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
            sb.append(String.format(Locale.ROOT, "%3d. %s%n", i + 1, draft[i]));
        }
        showScrollableText("Mock Draft", sb.toString());
    }

    private void showScrollableText(String title, String text) {
        DesktopTheme.showScrollableText(this, title, text);
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

    private void registerScreens() {
        screens.clear();
        screens.put("Home", new DashboardPanel(leagueCore, bridge,
                new DashboardPanel.Callbacks(
                    this::playWeek, this::advanceFullYear,
                    this::selectRecruitingTab, this::openUserTeamDetail,
                    () -> saveLeague(false), this::showPlaybookDialog,
                    this::showBowlWatch,
                    () -> selectScreen("Scoreboard"), () -> selectScreen("News"),
                    () -> selectScreen("Poll Rankings"), () -> selectScreen("Player Stats"),
                    () -> selectScreen("Recruiting"), () -> selectScreen("Standings")
                )));
        screens.put("Scoreboard", new ScoreboardPanel());
        screens.put("News", new NewsPanel());
        screens.put("Player Search", new PlayerSearchPanel());
        screens.put("Standings", new StandingsPanel());
        screens.put("Poll Rankings", new PollRankingsPanel());
        screens.put("Team Rankings", new TeamRankingsPanel());
        screens.put("Player Stats", new PlayerStatsPanel());
        screens.put("League History", new LeagueHistoryPanel());
        screens.put("Coaches", new CoachDatabasePanel());
        screens.put("Hall of Fame", new HallOfFamePanel());
        screens.put("Records", new LeagueRecordsPanel());
        screens.put("My Coach", new CoachProfilePanel());
        screens.put("Settings", new SettingsPanel());
    }

    private void refresh() {
        this.currentRecord = leagueCore.toRecord();
        rebuildLiveTeamMap();
        setTitle(buildWindowTitle());

        setJMenuBar(buildMenuBar());
        rebuildHeader();
        rebuildContentCards();
        rebuildStatusBar();
        selectScreen(selectedScreen);

        revalidate();
        repaint();
        applyWindowTheme();
    }

    private void rebuildHeader() {
        remove(headerPanel);
        headerPanel = buildHeader();
        add(headerPanel, BorderLayout.NORTH);
    }

    private void rebuildStatusBar() {
        remove(statusBar);
        statusBar = buildStatusBar();
        add(statusBar, BorderLayout.SOUTH);
    }

    private void rebuildContentCards() {
        screenContext.updateRecord(currentRecord);
        mainContentCards.removeAll();
        for (Map.Entry<String, LeagueScreen> e : screens.entrySet()) {
            addScreenCard(e.getKey(), e.getValue().build(screenContext));
        }
        mainContentCards.add(buildRecruitingTab(), "Recruiting");
    }

    /** Targeted refresh for data model changes (avoids full UI rebuild). */
    public void refreshModels() {
        revalidate();
        repaint();
    }

    private JPanel buildMainContent() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(true);
        shell.setBackground(DesktopTheme.windowBackground());

        navigationList = buildNavigationList();
        JScrollPane navScroll = new JScrollPane(navigationList);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.getViewport().setBackground(DesktopTheme.sidebarBackground());

        JPanel sidebar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                DesktopTheme.paintSidebarGradient(g, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        sidebar.setOpaque(false);
        sidebar.setBackground(DesktopTheme.sidebarBackground());
        sidebar.setPreferredSize(new Dimension(205, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DesktopTheme.borderSubtle()));

        JPanel navHeader = new JPanel(new GridLayout(0, 1, 0, 2));
        navHeader.setOpaque(false);
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
        for (Map.Entry<String, LeagueScreen> e : screens.entrySet()) {
            addScreenCard(e.getKey(), e.getValue().build(screenContext));
        }
        mainContentCards.add(buildRecruitingTab(), "Recruiting");
        shell.add(mainContentCards, BorderLayout.CENTER);
        selectScreen(selectedScreen);
        return shell;
    }

    private JList<String> buildNavigationList() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 0; i < NAV_TITLES.length; i++) {
            model.addElement(NAV_ICONS[i] + "  " + NAV_TITLES[i]);
        }
        JList<String> list = new JList<>(model);
        list.setFixedCellHeight(36);
        list.setFont(new Font("SansSerif", Font.PLAIN, 12));
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
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 10));
                label.setOpaque(true);
                label.setBackground(isSelected ? DesktopTheme.sidebarSelectionBackground() : DesktopTheme.sidebarBackground());
                label.setForeground(isSelected ? Color.WHITE : DesktopTheme.sidebarText());
                if (isSelected) {
                    label.setFont(new Font("SansSerif", Font.BOLD, 12));
                } else {
                    label.setFont(new Font("SansSerif", Font.PLAIN, 12));
                }
                return label;
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String value = list.getSelectedValue();
                if (value != null) {
                    audioManager.play(AudioEvent.UI_CLICK);
                    int idx = list.getSelectedIndex();
                    String title = idx >= 0 && idx < NAV_TITLES.length ? NAV_TITLES[idx] : "Home";
                    selectScreen(title);
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
        if (navigationList != null) {
            int idx = -1;
            for (int i = 0; i < NAV_TITLES.length; i++) {
                if (NAV_TITLES[i].equals(title)) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                String prefixed = NAV_ICONS[idx] + "  " + NAV_TITLES[idx];
                if (!prefixed.equals(navigationList.getSelectedValue())) {
                    navigationList.setSelectedValue(prefixed, true);
                }
            }
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

        if (leagueCore.userTeam != null && leagueCore.userTeam.isUserControlled()) {
            if (activeRecruitingSession == null) {
                activeRecruitingSession = SimulationFacade.prepareRecruitingSession(leagueCore.userTeam);
            }
            boolean finalSigning = bridge.isAwaitingDockedRecruiting();
            String buttonText = finalSigning ? "Finish Recruiting" : "Keep Recruiting Progress";
            String title = finalSigning ? "Finish Recruiting?" : "Keep Recruiting Progress?";
            String message = finalSigning ? null
                    : "Keep working this recruiting board during the season?\n\n"
                    + "Your commitments stay in this open desktop session and will sign when final recruiting begins. "
                    + "They will not join the active roster during the regular season or persist after closing the app.";
            RecruitingPanel panel = new RecruitingPanel(leagueCore, activeRecruitingSession,
                    buttonText, title, message, data -> SwingUtilities.invokeLater(() -> {
                if (!bridge.isAwaitingDockedRecruiting()) {
                    markDirty();
                    JOptionPane.showMessageDialog(this,
                            DesktopTheme.messageForDialog(
                                    "Recruiting progress is saved on this board.\n"
                                            + "Keep this desktop window open and return before final recruiting to keep working the class."),
                            "Recruiting Progress",
                            JOptionPane.INFORMATION_MESSAGE);
                    selectScreen("Home");
                    return;
                }
                bridge.completeDockedRecruiting(data);
                activeRecruitingSession = null;
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
        } else {
            html = "<html><div style='width:520px'>Recruiting is only available for user-controlled schools.</div></html>";
        }
        info.setText(html);
        info.setForeground(DesktopTheme.textPrimary());
        outer.add(info, BorderLayout.NORTH);
        return outer;
    }

    private String decodeSeasonPeriod() {
        return SeasonPresentation.getSeasonCycleLabel(leagueCore);
    }

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
        return PlayerSearch.findInLeague(liveTeamMap, playerName, teamName);
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
            JDialog dialog = new JDialog((JFrame) null, "CFHC - " + record.leagueName()
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
