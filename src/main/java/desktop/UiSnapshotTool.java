package desktop;

import simulation.GameUiBridge;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;

import javax.imageio.ImageIO;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;

/**
 * Dev utility: renders the real app UI offscreen to PNGs for visual audit.
 * No window is shown; components are realized, laid out, and painted into
 * buffered images.
 *
 * Run from the repo root (needs desktop classes + resources on classpath):
 *   java -cp "build/desktop/classes;build/desktop/resources;libs/*" desktop.UiSnapshotTool outDir [weeksToSim] [theme] [WxH]
 */
public final class UiSnapshotTool {

    public static void main(String[] args) throws Exception {
        simulation.PlatformLog.setDebugEnabled(false);
        String outDir = args.length > 0 ? args[0] : "build/ui-audit";
        int weeks = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        // Optional 3rd arg: force a theme variant for color-scheme audits
        // ("dark" | "light" | "hc"). Default: whatever the user prefs hold.
        String themeArg = args.length > 2 ? args[2] : null;
        // Optional 4th arg: window size as WIDTHxHEIGHT (default 1600x1000).
        // Audit at 1200x850 too — that is the size a new LeagueHomeView opens at.
        int frameW = 1600;
        int frameH = 1000;
        if (args.length > 3) {
            String[] wh = args[3].toLowerCase(java.util.Locale.ROOT).split("x");
            frameW = Integer.parseInt(wh[0].trim());
            frameH = Integer.parseInt(wh[1].trim());
        }
        new File(outDir).mkdirs();

        DesktopTheme.load();
        // Theme forcing for color-scheme audits (default = persisted prefs):
        // dark / light / dark-hc / light-hc.
        if ("dark".equalsIgnoreCase(themeArg)) { DesktopTheme.setDark(true); DesktopTheme.setHighContrast(false); }
        else if ("light".equalsIgnoreCase(themeArg)) { DesktopTheme.setDark(false); DesktopTheme.setHighContrast(false); }
        else if ("dark-hc".equalsIgnoreCase(themeArg)) { DesktopTheme.setDark(true); DesktopTheme.setHighContrast(true); }
        else if ("light-hc".equalsIgnoreCase(themeArg)) { DesktopTheme.setDark(false); DesktopTheme.setHighContrast(true); }
        DesktopResourceProvider resources =
                new DesktopResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.userControlled = true;
        league.careerMode = true;

        // Advance a few weeks so the dashboard shows real scores/news/records.
        SeasonController controller = new SeasonController(league, silentBridge());
        for (int i = 0; i < weeks; i++) {
            controller.advanceWeek();
        }

        LeagueHomeView view = new LeagueHomeView(league);
        view.setSize(frameW, frameH);
        view.setLocationRelativeTo(null);
        // The window must actually be realized on-screen for the LAF and
        // RepaintManager to paint correctly; printAll captures it without
        // needing a Robot. Briefly visible while snapshots are taken.
        view.setVisible(true);
        Thread.sleep(800); // let layout + LAF settle

        auditMenus(view, league);

        // Full-frame shot INCLUDING the JMenuBar (getContentPane() captures
        // exclude it, which hid the menu bar from every earlier audit).
        Thread.sleep(300);
        capture(view, outDir + "/_full_window.png");
        System.out.println("captured: _full_window (frame + menu bar)");

        Method select = LeagueHomeView.class.getDeclaredMethod("selectScreen", String.class);
        select.setAccessible(true);

        String[] screens = {
                "Home", "Recruiting", "Standings", "Scoreboard", "My Coach",
                "Poll Rankings", "Team Rankings", "Player Stats", "Player Search",
                "League History", "News", "Coaches", "Hall of Fame", "Records",
                "Settings"};
        for (String screen : screens) {
            select.invoke(view, screen);
            Thread.sleep(300);
            capture(view.getContentPane(), outDir + "/" + screen.replace(' ', '_').toLowerCase() + ".png");
            System.out.println("captured: " + screen);
            if ("Home".equals(screen)) {
                // The dashboard grid scrolls when its rows don't fit; capture the lower half too.
                javax.swing.JScrollPane gridScroll = findGridScroll(view.getContentPane());
                if (gridScroll != null) {
                    // Whole grid in one image, regardless of the viewport.
                    capture(gridScroll.getViewport().getView(), outDir + "/home_grid.png");
                    javax.swing.JScrollBar bar = gridScroll.getVerticalScrollBar();
                    javax.swing.SwingUtilities.invokeAndWait(() -> bar.setValue(bar.getMaximum()));
                    Thread.sleep(300);
                    capture(view.getContentPane(), outDir + "/home_scrolled.png");
                    javax.swing.SwingUtilities.invokeAndWait(() -> bar.setValue(0));
                    System.out.println("captured: Home (scrolled)");
                }
            }
        }

        view.setVisible(false);
        view.dispose();

        // Launcher (Career Hub / front office), the first window players see.
        LauncherFrame launcher = new LauncherFrame();
        launcher.setLocationRelativeTo(null);
        launcher.setVisible(true);
        Thread.sleep(600);
        capture(launcher, outDir + "/_launcher.png");
        System.out.println("captured: Launcher");
        launcher.setVisible(false);
        launcher.dispose();

        System.out.println("done -> " + outDir);
        System.exit(0);
    }

    /**
     * Menu audit: dumps the full menu tree (including the Phase 5 additions)
     * and smoke-tests the Interactive Coaching toggle end to end.
     */
    private static void auditMenus(javax.swing.JFrame view, League league) {
        System.out.println("== MENU AUDIT ==");
        javax.swing.JMenuBar bar = view.getJMenuBar();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            javax.swing.JMenu menu = bar.getMenu(i);
            if (menu != null) {
                System.out.println("MENU: " + menu.getText());
                walkMenu(menu, "  ", league);
            }
        }
        System.out.println("== MENU AUDIT DONE ==");
    }

    private static void walkMenu(javax.swing.JMenu menu, String indent, League league) {
        for (java.awt.Component c : menu.getMenuComponents()) {
            if (c instanceof javax.swing.JMenu) {
                javax.swing.JMenu sub = (javax.swing.JMenu) c;
                System.out.println(indent + "MENU: " + sub.getText());
                walkMenu(sub, indent + "  ", league);
            } else if (c instanceof javax.swing.JMenuItem) {
                javax.swing.JMenuItem item = (javax.swing.JMenuItem) c;
                String flags = item.isEnabled() ? "" : " [disabled]";
                if (c instanceof javax.swing.JCheckBoxMenuItem) {
                    flags += " [selected=" + ((javax.swing.JCheckBoxMenuItem) c).isSelected() + "]";
                }
                System.out.println(indent + "ITEM: " + item.getText() + flags);

                if ("Interactive Coaching".equals(item.getText())
                        && c instanceof javax.swing.JCheckBoxMenuItem) {
                    javax.swing.JCheckBoxMenuItem toggle = (javax.swing.JCheckBoxMenuItem) c;
                    toggle.doClick(); // fires the action synchronously
                    System.out.println(indent + "  -> coach listener installed: "
                            + (league.getGameCoachListener() != null));
                    toggle.doClick();
                    System.out.println(indent + "  -> coach listener removed: "
                            + (league.getGameCoachListener() == null));
                }
                if ("Offseason Hub".equals(item.getText())) {
                    System.out.println(indent + "  -> offseason hub enabled: " + item.isEnabled()
                            + " (step " + simulation.SeasonFlowOrder.offseasonStepIndex(
                                    league.currentWeek, league.regSeasonWeeks) + ")");
                }
            }
        }
    }

    /** Scroll pane whose view is the dashboard {@link DashboardCardGrid}, or null. */
    private static javax.swing.JScrollPane findGridScroll(java.awt.Container root) {
        for (java.awt.Component c : root.getComponents()) {
            if (c instanceof javax.swing.JScrollPane sp
                    && sp.getViewport().getView() instanceof DashboardCardGrid
                    && sp.isShowing()) {
                return sp;
            }
            if (c instanceof java.awt.Container inner) {
                javax.swing.JScrollPane found = findGridScroll(inner);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static GameUiBridge silentBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File f, simulation.Team t) {}
            @Override public void transferPlayer(positions.Player p) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player p, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {
                // never reached within a few regular-season weeks
            }
        };
    }

    private static void capture(Component c, String path) throws Exception {
        BufferedImage img = new BufferedImage(
                Math.max(1, c.getWidth()), Math.max(1, c.getHeight()),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // printAll (the printing pathway) renders the full component tree.
        c.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", new File(path));
    }

    private UiSnapshotTool() {}
}
