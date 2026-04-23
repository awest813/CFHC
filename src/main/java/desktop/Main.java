package desktop;

import simulation.League;
import simulation.LeagueRecord;
import simulation.PlatformLog;
import simulation.PlatformResourceProvider;
import simulation.SaveManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A rudimentary Java entry point for testing the simulation core outside of Android.
 * This demonstrates the portability achieved through the LeagueRecord hierarchy.
 */
public class Main {

    private static final String TAG = "desktop.Main";

    private static final String HEADER =
            "CFB Coach - Desktop Prototype (Early Alpha)\n" +
            "===========================================";

    public static void main(String[] args) {
        // Enable HiDPI scaling and system look-and-feel for a native desktop appearance
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default Metal LAF if system LAF is not available
        }

        PlatformLog.i(TAG, HEADER.replace("\n", " | "));
        System.out.println(HEADER);

        if (args.length == 0) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                new LauncherFrame().setVisible(true);
            });
            return;
        }

        String command = args[0];
        switch (command) {
            case "new":
                launchNewLeague();
                break;
            case "inspect":
                if (args.length < 2) {
                    fail("Specify a save file to inspect.");
                    return;
                }
                inspectSaveFile(args[1]);
                break;
            case "play":
            case "view":
                if (args.length < 2) {
                    fail("Specify a save file to play.");
                    return;
                }
                launchPlayMode(args[1]);
                break;
            case "help":
            case "--help":
            case "-h":
                printUsage();
                break;
            default:
                fail("Unknown command: " + command);
                printUsage();
                break;
        }
    }

    private static void fail(String message) {
        PlatformLog.e(TAG, message);
        System.err.println("Error: " + message);
    }

    private static void printUsage() {
        System.out.println("Usage: java desktop.Main <command> [file]");
        System.out.println("Commands:");
        System.out.println("  new                - Launch a new desktop league from bundled resources");
        System.out.println("  inspect <savefile> - Print save metadata to console");
        System.out.println("  play    <savefile> - Launch graphical league home from an existing save");
        System.out.println("  help               - Show this message");
    }

    private static DesktopResourceProvider createResourceProvider() {
        return new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    private static void launchNewLeague() {
        try {
            DesktopResourceProvider resources = createResourceProvider();
            League league = NewGameWizard.showWizard(null, resources);
            if (league == null) {
                PlatformLog.i(TAG, "New-game wizard cancelled");
                System.out.println("New-game wizard cancelled.");
                return;
            }
            PlatformLog.i(TAG, "Launching new league UI - user team: "
                    + (league.userTeam != null ? league.userTeam.getName() : "none"));
            LeagueHomeView.show(league);
        } catch (Exception e) {
            PlatformLog.e(TAG, "Error launching new desktop league", e);
            System.err.println("Error launching new desktop league: " + e.getMessage());
        }
    }

    private static void launchPlayMode(String filePath) {
        File saveFile = new File(filePath);
        if (!saveFile.isFile()) {
            fail("save file not found: " + filePath);
            return;
        }
        try {
            DesktopResourceProvider resources = createResourceProvider();
            League league = new League(
                    saveFile,
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    false
            );
            league.setPlatformResourceProvider(resources);
            PlatformLog.i(TAG, "Launching play UI for " + saveFile.getAbsolutePath());
            LeagueHomeView.show(league, saveFile);
        } catch (Exception e) {
            PlatformLog.e(TAG, "Error launching play mode", e);
            System.err.println("Error launching play mode: " + e.getMessage());
        }
    }

    private static void inspectSaveFile(String filePath) {
        File saveFile = new File(filePath);
        if (!saveFile.isFile()) {
            fail("save file not found: " + filePath);
            return;
        }
        try (FileInputStream fis = new FileInputStream(saveFile)) {
            LeagueRecord league = SaveManager.load(fis);

            System.out.println("League: " + league.leagueName());
            System.out.println("Year: " + league.year());
            System.out.println("Current Week: " + league.currentWeek());
            System.out.println("Total Conferences: " + league.conferences().size());

            int teams = league.conferences().stream().mapToInt(c -> c.teams().size()).sum();
            System.out.println("Total Teams: " + teams);
            System.out.println("Hall of Fame: " + league.leagueHoF().size() + " players");

            System.out.println();
            System.out.println("--- Top Teams ---");
            league.conferences().stream()
                    .flatMap(c -> c.teams().stream())
                    .sorted((t1, t2) -> t2.prestige() - t1.prestige())
                    .limit(10)
                    .forEach(t -> System.out.printf("[%-20s] Prestige: %d, Roster: %d%n",
                            t.name(), t.prestige(), t.roster().size()));
        } catch (IOException e) {
            PlatformLog.e(TAG, "Error reading save file " + filePath, e);
            System.err.println("Error reading save file: " + e.getMessage());
        } catch (RuntimeException e) {
            PlatformLog.e(TAG, "Error parsing save file " + filePath, e);
            System.err.println("Error parsing save file: " + e.getMessage());
        }
    }
}
