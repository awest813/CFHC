package desktop;

import simulation.PlatformLog;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.desktop.QuitResponse;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

/**
 * macOS Aqua / Desktop API hooks: screen menu bar, About / Preferences / Quit,
 * and open-file handling for {@code .cfb} saves.
 *
 * <p>Safe to call on any OS — no-ops when the Desktop API feature is unsupported.
 */
public final class MacDesktopIntegration {

    private static final String TAG = "MacDesktopIntegration";

    private static volatile WeakReference<LeagueHomeView> activeLeagueHome = new WeakReference<>(null);
    private static volatile WeakReference<JFrame> activeFrame = new WeakReference<>(null);
    private static volatile Consumer<File> openSaveHandler;

    private MacDesktopIntegration() {}

    /**
     * Must run before installing the look-and-feel so the screen menu bar is used on macOS.
     */
    public static void installEarly() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("mac")) {
            return;
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.application.name", "CFHC");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "CFHC");
    }

    /**
     * Registers About / Preferences / Quit / OpenFile handlers after Swing is ready.
     *
     * @param onOpenSave invoked on the EDT when the OS asks to open a save file
     */
    public static void installHandlers(Consumer<File> onOpenSave) {
        openSaveHandler = onOpenSave;
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        try {
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(e -> SwingUtilities.invokeLater(
                        () -> showAbout(activeFrame.get())));
            }
            if (desktop.isSupported(Desktop.Action.APP_PREFERENCES)) {
                desktop.setPreferencesHandler(e -> SwingUtilities.invokeLater(
                        MacDesktopIntegration::openPreferences));
            }
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((e, response) -> SwingUtilities.invokeLater(
                        () -> handleQuit(response)));
            }
            if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                desktop.setOpenFileHandler(e -> {
                    List<File> files = e.getFiles();
                    if (files == null || files.isEmpty()) {
                        return;
                    }
                    File first = files.get(0);
                    SwingUtilities.invokeLater(() -> {
                        Consumer<File> handler = openSaveHandler;
                        if (handler != null) {
                            handler.accept(first);
                        }
                    });
                });
            }
            PlatformLog.i(TAG, "Desktop handlers installed (About/Preferences/Quit/OpenFile as available)");
        } catch (Exception ex) {
            PlatformLog.w(TAG, "Could not install Desktop handlers: " + ex.getMessage());
        }
    }

    /** Track the front-most league window for Preferences / Quit. */
    public static void setActiveLeagueHome(LeagueHomeView view) {
        activeLeagueHome = new WeakReference<>(view);
        if (view != null) {
            activeFrame = new WeakReference<>(view);
        }
    }

    /** Track any top-level frame (launcher or league) for About parentage. */
    public static void setActiveFrame(JFrame frame) {
        activeFrame = new WeakReference<>(frame);
    }

    public static void showAbout(java.awt.Component parent) {
        String savesHint;
        try {
            savesHint = DesktopAppPaths.chooserStartDir().getAbsolutePath();
        } catch (Exception e) {
            savesHint = "~/.cfhc/saves";
        }
        JOptionPane.showMessageDialog(parent,
                DesktopTheme.messageForDialog(
                        "College Football Head Coach (CFHC) — " + DesktopVersion.DISPLAY + "\n"
                                + "Portable Java build of the College Football Head Coach simulation.\n\n"
                                + "Saves folder:\n" + savesHint + "\n\n"
                                + "Releases:\n" + DesktopVersion.RELEASES_URL + "\n\n"
                                + "Press F1 or Ctrl+/ for the full shortcut list.\n"
                                + "Help → Check for Updates compares against GitHub (manual download only).\n"
                                + "Help → Licenses & attribution for sound and library notices."),
                "About CFHC",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static void openPreferences() {
        LeagueHomeView home = activeLeagueHome.get();
        if (home != null) {
            home.openSettingsFromOs();
            return;
        }
        JFrame frame = activeFrame.get();
        // Shell-only prefs when no league is loaded (dark mode toggle).
        int choice = JOptionPane.showConfirmDialog(
                frame,
                DesktopTheme.messageForDialog(
                        "No league is open.\n\nToggle dark mode for the desktop shell?"),
                "CFHC Preferences",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            DesktopTheme.setDark(!DesktopTheme.isDark());
            if (frame != null) {
                SwingUtilities.updateComponentTreeUI(frame);
            }
        }
    }

    private static void handleQuit(QuitResponse response) {
        LeagueHomeView home = activeLeagueHome.get();
        if (home != null) {
            boolean exiting = home.requestQuitFromOs();
            if (exiting) {
                response.performQuit();
            } else {
                response.cancelQuit();
            }
            return;
        }
        response.performQuit();
    }
}
