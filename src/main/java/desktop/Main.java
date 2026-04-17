package desktop;

import simulation.League;
import simulation.LeagueRecord;
import simulation.SaveManager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A rudimentary Java entry point for testing the simulation core outside of Android.
 * This demonstrates the portability achieved through' the LeagueRecord hierarchy.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("CFB Coach - Desktop Prototype (Early Alpha)");
        System.out.println("===========================================");

        if (args.length == 0) {
            System.out.println("Usage: java desktop.Main <command> [file]");
            System.out.println("Commands:");
            System.out.println("  inspect <savefile> - Print save metadata to console");
            System.out.println("  view    <savefile> - Launch graphical league home");
            return;
        }

        String command = args[0];
        
        if (command.equals("inspect")) {
            if (args.length < 2) {
                System.out.println("Error: Specify a save file to inspect.");
                return;
            }
            inspectSaveFile(args[1]);
        } else if (command.equals("play")) {
            if (args.length < 2) {
                System.out.println("Error: Specify a save file to play.");
                return;
            }
            launchPlayMode(args[1]);
        }
    }

    private static void launchPlayMode(String filePath) {
        try {
            String projectRoot = System.getProperty("user.dir");
            DesktopResourceProvider resProvider = new DesktopResourceProvider(projectRoot);
            
            File saveFile = new File(filePath);
            League league = new League(
                saveFile, 
                resProvider.getString("league_player_names"), 
                resProvider.getString("league_last_names"), 
                false
            );
            league.setPlatformResourceProvider(resProvider);
            
            LeagueHomeView.show(league);
        } catch (Exception e) {
            System.err.println("Error launching Play Mode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void launchGui(String filePath) {

        try (FileInputStream fis = new FileInputStream(filePath)) {
            LeagueRecord league = SaveManager.load(fis);
            LeagueHomeView.show(league);
        } catch (Exception e) {
            System.err.println("Error launching GUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    }

    private static void inspectSaveFile(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            LeagueRecord league = SaveManager.load(fis);
            
            System.out.println("League: " + league.leagueName());
            System.out.println("Year: " + league.year());
            System.out.println("Current Week: " + league.currentWeek());
            System.out.println("Total Conferences: " + league.conferences().size());
            
            System.out.println("\n--- Top Teams ---");
            league.conferences().stream()
                .flatMap(c -> c.teams().stream())
                .sorted((t1, t2) -> t2.prestige() - t1.prestige())
                .limit(10)
                .forEach(t -> System.out.printf("[%20s] Prestige: %d\n", t.name(), t.prestige()));

        } catch (IOException e) {
            System.err.println("Error reading save file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing save file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
