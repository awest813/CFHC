package antdroid.cfbcoach;

import android.content.Context;
import android.widget.Toast;

import java.io.File;

import simulation.League;

public final class LeagueExportController {
    private LeagueExportController() {
    }

    public static File exportLeagueFile(Context context, League league, String filename, String successMessage) {
        File output = new File(LeagueSaveStorage.getExternalSaveDir(context, "CFBCOACH"), filename);
        league.saveLeague(output);
        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show();
        return output;
    }

    public static File exportPrimarySave(Context context, League league) {
        return exportLeagueFile(context, league, "CFB_SAVE.txt", "Exported Save to Storage");
    }

    public static File exportTeams(Context context, League league) {
        return exportLeagueFile(context, league, "CFB_TEAMS.txt", "Saved league!");
    }

    public static File exportBowls(Context context, League league) {
        return exportLeagueFile(context, league, "CFB_BOWLS.txt", "Saved league!");
    }

    public static File exportPlayers(Context context, League league) {
        return exportLeagueFile(context, league, "CFB_PLAYERS.txt", "Saved league!");
    }

    public static File exportConferences(Context context, League league) {
        return exportLeagueFile(context, league, "CFB_CONF.txt", "Saved league!");
    }
}
