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
 *   java -cp "build/desktop/classes;build/desktop/resources;libs/*" desktop.UiSnapshotTool outDir [weeksToSim]
 */
public final class UiSnapshotTool {

    public static void main(String[] args) throws Exception {
        simulation.PlatformLog.setDebugEnabled(false);
        String outDir = args.length > 0 ? args[0] : "build/ui-audit";
        int weeks = args.length > 1 ? Integer.parseInt(args[1]) : 8;
        new File(outDir).mkdirs();

        DesktopTheme.load();
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
        view.setSize(1600, 1000);
        view.setLocationRelativeTo(null);
        // The window must actually be realized on-screen for the LAF and
        // RepaintManager to paint correctly; printAll captures it without
        // needing a Robot. Briefly visible while snapshots are taken.
        view.setVisible(true);
        Thread.sleep(800); // let layout + LAF settle

        auditMenus(view, league);

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
        }

        view.setVisible(false);
        view.dispose();
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
