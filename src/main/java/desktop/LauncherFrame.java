package desktop;

import simulation.League;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.SaveManager;

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

/**
 * Visual entry point for the desktop client. Replaces the CLI-only launcher.
 */
public class LauncherFrame extends JFrame {

    private static final String TAG = "LauncherFrame";
    private static final Color DARK_BLUE = new Color(20, 30, 48);
    private static final Color ACCENT_BLUE = new Color(50, 100, 180);

    public LauncherFrame() {
        super("CFB Coach - Desktop Hub");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);
        setMinimumSize(new Dimension(760, 460));
        setLocationRelativeTo(null);
        loadWindowIcon();

        setLayout(new BorderLayout());

        add(buildSidePanel(), BorderLayout.WEST);
        add(buildMainControlPanel(), BorderLayout.CENTER);
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
        side.setPreferredSize(new Dimension(300, 500));
        side.setBackground(DARK_BLUE);
        side.setBorder(BorderFactory.createEmptyBorder(40, 20, 40, 20));

        JLabel title = new JLabel("<html><body style='text-align:center;'>CFB COACH<br><span style='font-size:12pt;font-weight:normal;'>Desktop Prototype</span></body></html>", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        side.add(title, BorderLayout.NORTH);

        JLabel info = new JLabel("<html><p style='text-align:center; color:"
                + (DesktopTheme.isDark() ? "#99A0AA" : "#AAAAAA")
                + ";'>College Football Head Coach (CFHC)<br>Desktop management shell<br><br>v1.4e [Alpha]</p></html>",
                SwingConstants.CENTER);
        side.add(info, BorderLayout.CENTER);

        return side;
    }

    private JPanel buildMainControlPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(true);
        main.setBackground(DesktopTheme.launcherMainPanel());
        main.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

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
        centerWrap.add(buttonGrid, BorderLayout.CENTER);
        JCheckBox darkToggle = new JCheckBox("Dark mode", DesktopTheme.isDark());
        darkToggle.setOpaque(false);
        darkToggle.setForeground(DesktopTheme.textPrimary());
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        toggleRow.setOpaque(false);
        toggleRow.add(darkToggle);
        darkToggle.addActionListener(e -> {
            DesktopTheme.setDark(darkToggle.isSelected());
            SwingUtilities.updateComponentTreeUI(this);
        });
        centerWrap.add(toggleRow, BorderLayout.SOUTH);
        main.add(centerWrap, BorderLayout.CENTER);

        JLabel footer = new JLabel("\u00a9 2026 Engine Audit & Polish Update", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(DesktopTheme.launcherFooter());
        main.add(footer, BorderLayout.SOUTH);

        return main;
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(300, 45));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setBackground(ACCENT_BLUE);
        btn.setForeground(Color.WHITE);
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
                • Tabs along the left edge jump between standings, scoreboard, stats, news, and more.
                • Space plays the next week (or the next offseason step). Use the header buttons for longer sims.
                • Double-click any team in Standings to open rosters, depth chart, coordinators, and facilities.
                • F1 lists every keyboard shortcut.

                Recruiting
                After the final offseason step before signing day, press Space once. The signing board appears
                in the Recruiting tab (left). Finish recruiting there to roll into the next season.

                Saving
                Use File → Save (Ctrl+S). Unsaved leagues prompt on exit.""";
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
