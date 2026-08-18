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

        Method select = LeagueHomeView.class.getDeclaredMethod("selectScreen", String.class);
        select.setAccessible(true);

        String[] screens = {"Home", "My Coach", "Scoreboard", "News", "Standings", "Settings"};
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
