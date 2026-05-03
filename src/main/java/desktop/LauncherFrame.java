package desktop;

import simulation.League;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual entry point for the desktop client. Replaces the CLI-only launcher.
 */
public class LauncherFrame extends JFrame {

    private static final String TAG = "LauncherFrame";
    private static final Color BRAND_PANEL = new Color(24, 34, 48);

    private final List<JButton> launcherHubButtons = new ArrayList<>();
    private JPanel launcherMainPanel;
    private JLabel launcherHeaderLabel;
    private JLabel launcherSideBlurb;
    private JLabel launcherFooterLabel;

    public LauncherFrame() {
        super("CFB Coach - Front Office");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 560);
        setMinimumSize(new Dimension(820, 500));
        setLocationRelativeTo(null);
        loadWindowIcon();

        setLayout(new BorderLayout());

        add(buildSidePanel(), BorderLayout.WEST);
        launcherMainPanel = buildMainControlPanel();
        add(launcherMainPanel, BorderLayout.CENTER);
        getContentPane().setBackground(DesktopTheme.launcherMainPanel());
    }

    private void loadWindowIcon() {
        try (InputStream iconStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("assets/cfhc_icon.png")) {
            if (iconStream != null) {
                java.awt.Image icon = javax.imageio.ImageIO.read(iconStream);
                if (icon != null) {
                    setIconImage(icon);
                }
            }
        } catch (Exception ignored) {
            // optional branding
        }
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(315, 500));
        side.setBackground(BRAND_PANEL);
        side.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel title = new JLabel("<html><body style='text-align:center;'>CFB COACH<br><span style='font-size:12pt;font-weight:normal;'>Front Office</span></body></html>", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        side.add(title, BorderLayout.NORTH);

        launcherSideBlurb = new JLabel(sideBlurbHtml(), SwingConstants.CENTER);
        side.add(launcherSideBlurb, BorderLayout.CENTER);

        return side;
    }

    private JPanel buildMainControlPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(true);
        main.setBackground(DesktopTheme.launcherMainPanel());
        main.setBorder(BorderFactory.createEmptyBorder(42, 56, 42, 56));

        launcherHeaderLabel = new JLabel("<html><b>Career Hub</b><br><span style='font-size:10pt;'>Start, load, and manage your college football universe.</span></html>");
        launcherHeaderLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        launcherHeaderLabel.setForeground(DesktopTheme.textPrimary());
        main.add(launcherHeaderLabel, BorderLayout.NORTH);

        JPanel buttonGrid = new JPanel(new GridLayout(0, 1, 0, 15));
        buttonGrid.setOpaque(false);

        JButton newBtn = createStyledButton("New Career", "Start a fresh league with standard rosters.");
        newBtn.setMnemonic('N');
        newBtn.addActionListener(e -> launchNewLeague());
        buttonGrid.add(newBtn);

        JButton loadBtn = createStyledButton("Load Save", "Continue an existing simulation (.cfb or .sav).");
        loadBtn.setMnemonic('L');
        loadBtn.addActionListener(e -> launchLoadGame());
        buttonGrid.add(loadBtn);

        JButton helpBtn = createStyledButton("How to Play", "Basics of college football management.");
        helpBtn.setMnemonic('H');
        helpBtn.addActionListener(e -> showHelp());
        buttonGrid.add(helpBtn);

        JButton exitBtn = createStyledButton("Exit", "Close the application.");
        exitBtn.setMnemonic('E');
        exitBtn.addActionListener(e -> System.exit(0));
        buttonGrid.add(exitBtn);

        JPanel centerWrap = new JPanel(new BorderLayout(0, 14));
        centerWrap.setOpaque(false);
        centerWrap.setBorder(BorderFactory.createEmptyBorder(28, 0, 0, 0));
        centerWrap.add(buttonGrid, BorderLayout.CENTER);
        JCheckBox darkToggle = new JCheckBox("Dark mode", DesktopTheme.isDark());
        darkToggle.setOpaque(false);
        darkToggle.setForeground(DesktopTheme.textPrimary());
        darkToggle.setMnemonic('D');
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        toggleRow.setOpaque(false);
        toggleRow.add(darkToggle);
        darkToggle.addActionListener(e -> {
            DesktopTheme.setDark(darkToggle.isSelected());
            SwingUtilities.updateComponentTreeUI(this);
            refreshLauncherChrome(darkToggle);
        });
        centerWrap.add(toggleRow, BorderLayout.SOUTH);
        main.add(centerWrap, BorderLayout.CENTER);

        launcherFooterLabel = new JLabel("Desktop build - simulation core shared with Android", SwingConstants.CENTER);
        launcherFooterLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        launcherFooterLabel.setForeground(DesktopTheme.launcherFooter());
        main.add(launcherFooterLabel, BorderLayout.SOUTH);

        return main;
    }

    private static String sideBlurbHtml() {
        String muted = DesktopTheme.isDark() ? "#99A0AA" : "#666666";
        return "<html><p style='text-align:center; color:" + muted
                + ";'>College Football Head Coach (CFHC)<br>Desktop career management<br><br>v1.4e</p></html>";
    }

    private void refreshLauncherChrome(JCheckBox darkToggle) {
        getContentPane().setBackground(DesktopTheme.launcherMainPanel());
        if (launcherMainPanel != null) {
            launcherMainPanel.setBackground(DesktopTheme.launcherMainPanel());
        }
        if (launcherFooterLabel != null) {
            launcherFooterLabel.setForeground(DesktopTheme.launcherFooter());
        }
        if (launcherHeaderLabel != null) {
            launcherHeaderLabel.setForeground(DesktopTheme.textPrimary());
        }
        if (launcherSideBlurb != null) {
            launcherSideBlurb.setText(sideBlurbHtml());
        }
        if (darkToggle != null) {
            darkToggle.setForeground(DesktopTheme.textPrimary());
        }
        for (JButton b : launcherHubButtons) {
            DesktopTheme.styleLauncherHubButton(b);
        }
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(300, 45));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setToolTipText(tooltip);
        DesktopTheme.styleLauncherHubButton(btn);
        launcherHubButtons.add(btn);
        return btn;
    }

    private DesktopResourceProvider createResourceProvider() {
        return new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    private void launchNewLeague() {
        try {
            DesktopResourceProvider resources = createResourceProvider();
            League league = NewGameWizard.showWizard(this, resources);
            if (league != null) {
                LeagueHomeView.show(league);
                this.dispose();
            }
        } catch (Exception e) {
            PlatformLog.e(TAG, "Error in NewGameWizard", e);
            JOptionPane.showMessageDialog(this,
                    DesktopTheme.messageForDialog("Failed to start new game: " + e.getMessage()));
        }
    }

    private void launchLoadGame() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        DesktopTheme.styleFileChooser(chooser);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "CFHC saves (*.cfb, *.sav)", "cfb", "sav"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                DesktopResourceProvider resources = createResourceProvider();
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
                LeagueHomeView.show(league, file);
                this.dispose();
            } catch (Exception e) {
                PlatformLog.e(TAG, "Error loading save", e);
                JOptionPane.showMessageDialog(this,
                        DesktopTheme.messageForDialog("Failed to load save: " + e.getMessage()));
            }
        }
    }

    private void showHelp() {
        String msg = """
                Welcome to CFB Coach Desktop (CFHC).

                Getting started
                1. New Career walks you through universe and team selection.
                2. Load Save opens .cfb (desktop) or .sav exports from the Android build.

                In the league window
                - The League Office navigation opens standings, scoreboard, stats, news, settings, and more.
                - Space plays the next week or the next offseason step. Use the header buttons for longer sims.
                - Double-click any team in Standings to open rosters, depth chart, coordinators, and facilities.
                - F1 lists every keyboard shortcut.

                Recruiting
                After the final offseason step before signing day, press Space once. The signing board appears
                in Recruiting. Finish recruiting there to roll into the next season.

                Saving
                Use File > Save (Ctrl+S). Unsaved leagues prompt on exit.""";
        JTextArea area = new JTextArea(msg);
        area.setEditable(false);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        DesktopTheme.styleTextContent(area);
        area.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(area);
        scroll.getViewport().setBackground(DesktopTheme.textAreaEditorBackground());
        scroll.setPreferredSize(new Dimension(520, 340));
        JOptionPane.showMessageDialog(this, scroll, "How to Play", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        DesktopTheme.load();

        SwingUtilities.invokeLater(() -> {
            new LauncherFrame().setVisible(true);
        });
    }
}
