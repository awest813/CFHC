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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;

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

        setLayout(new BorderLayout());

        add(buildSidePanel(), BorderLayout.WEST);
        add(buildMainControlPanel(), BorderLayout.CENTER);
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

        JLabel info = new JLabel("<html><p style='text-align:center; color:#AAAAAA;'>The portable football management engine.<br><br>v1.4e [Alpha]</p></html>", SwingConstants.CENTER);
        side.add(info, BorderLayout.CENTER);

        return side;
    }

    private JPanel buildMainControlPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(60, 60, 60, 60));

        JPanel buttonGrid = new JPanel(new GridLayout(0, 1, 0, 15));
        buttonGrid.setOpaque(false);

        JButton newBtn = createStyledButton("New Career", "Start a fresh league with standard rosters.");
        newBtn.addActionListener(e -> launchNewLeague());
        buttonGrid.add(newBtn);

        JButton loadBtn = createStyledButton("Load Save", "Continue an existing simulation.");
        loadBtn.addActionListener(e -> launchLoadGame());
        buttonGrid.add(loadBtn);

        JButton helpBtn = createStyledButton("How to Play", "Basics of college football management.");
        helpBtn.addActionListener(e -> showHelp());
        buttonGrid.add(helpBtn);

        JButton exitBtn = createStyledButton("Exit", "Close the application.");
        exitBtn.addActionListener(e -> System.exit(0));
        buttonGrid.add(exitBtn);

        main.add(buttonGrid, BorderLayout.CENTER);

        JLabel footer = new JLabel("\u00a9 2026 Engine Audit & Polish Update", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(Color.LIGHT_GRAY);
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
            JOptionPane.showMessageDialog(this, "Failed to start new game: " + e.getMessage());
        }
    }

    private void launchLoadGame() {
        JFileChooser chooser = new JFileChooser(new File(System.getProperty("user.dir")));
        chooser.setFileFilter(new FileNameExtensionFilter("CFHC Save Files (*.sav)", "sav"));
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
                LeagueHomeView.show(league, file);
                this.dispose();
            } catch (Exception e) {
                PlatformLog.e(TAG, "Error loading save", e);
                JOptionPane.showMessageDialog(this, "Failed to load save: " + e.getMessage());
            }
        }
    }

    private void showHelp() {
        String msg = "Welcome to CFB Coach Desktop.\n\n"
                + "1. Advance Week: Simulate games for the current week.\n"
                + "2. Recruiting: During off-season (Week 15+), use the Recruiting tab to sign players.\n"
                + "3. Management: Use Team Detail (double-click standings) to manage depth charts.\n"
                + "4. Saving: The game auto-saves results, but manual saves can be triggered from the Home tab.";
        JOptionPane.showMessageDialog(this, msg, "How to Play", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new LauncherFrame().setVisible(true);
        });
    }
}
