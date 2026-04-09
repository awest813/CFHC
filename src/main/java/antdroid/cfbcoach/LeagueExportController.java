package antdroid.cfbcoach;

import java.io.File;

import simulation.League;

public final class LeagueExportController {
    private LeagueExportController() {
    }

    public static File exportLeagueFile(File exportDir, League league, String filename) {
        File output = new File(exportDir, filename);
        league.saveLeague(output);
        return output;
    }

    public static File exportPrimarySave(File exportDir, League league) {
        return exportLeagueFile(exportDir, league, "CFB_SAVE.txt");
    }

    public static File exportTeams(File exportDir, League league) {
        return exportLeagueFile(exportDir, league, "CFB_TEAMS.txt");
    }

    public static File exportBowls(File exportDir, League league) {
        return exportLeagueFile(exportDir, league, "CFB_BOWLS.txt");
    }

    public static File exportPlayers(File exportDir, League league) {
        return exportLeagueFile(exportDir, league, "CFB_PLAYERS.txt");
    }

    public static File exportConferences(File exportDir, League league) {
        return exportLeagueFile(exportDir, league, "CFB_CONF.txt");
    }
}
