package desktop;

import simulation.League;
import simulation.LeagueRecord;
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

    private static final String HEADER =
            "CFB Coach - Desktop Prototype (Early Alpha)\n" +
            "===========================================";

    public static void main(String[] args) {
        System.out.println(HEADER);

        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];
        switch (command) {
            case "new":
                launchNewLeague();
                break;
            case "inspect":
                if (args.length < 2) {
                    System.err.println("Error: Specify a save file to inspect.");
                    return;
                }
                inspectSaveFile(args[1]);
                break;
            case "play":
            case "view":
                if (args.length < 2) {
                    System.err.println("Error: Specify a save file to play.");
                    return;
                }
                launchPlayMode(args[1]);
                break;
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                break;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java desktop.Main <command> [file]");
        System.out.println("Commands:");
        System.out.println("  new                - Launch a new desktop league from bundled resources");
        System.out.println("  inspect <savefile> - Print save metadata to console");
        System.out.println("  play    <savefile> - Launch graphical league home from an existing save");
    }

    private static DesktopResourceProvider createResourceProvider() {
        return new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    private static void launchNewLeague() {
        try {
            DesktopResourceProvider resources = createResourceProvider();
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
            LeagueHomeView.show(league);
        } catch (Exception e) {
            System.err.println("Error launching new desktop league: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void launchPlayMode(String filePath) {
        File saveFile = new File(filePath);
        if (!saveFile.isFile()) {
            System.err.println("Error: save file not found: " + filePath);
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
            LeagueHomeView.show(league);
        } catch (Exception e) {
            System.err.println("Error launching play mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void inspectSaveFile(String filePath) {
        File saveFile = new File(filePath);
        if (!saveFile.isFile()) {
            System.err.println("Error: save file not found: " + filePath);
            return;
        }
        try (FileInputStream fis = new FileInputStream(saveFile)) {
            LeagueRecord league = SaveManager.load(fis);

            System.out.println("League: " + league.leagueName());
            System.out.println("Year: " + league.year());
            System.out.println("Current Week: " + league.currentWeek());
            System.out.println("Total Conferences: " + league.conferences().size());

            System.out.println();
            System.out.println("--- Top Teams ---");
            league.conferences().stream()
                    .flatMap(c -> c.teams().stream())
                    .sorted((t1, t2) -> t2.prestige() - t1.prestige())
                    .limit(10)
                    .forEach(t -> System.out.printf("[%20s] Prestige: %d%n", t.name(), t.prestige()));
        } catch (IOException e) {
            System.err.println("Error reading save file: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error parsing save file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
