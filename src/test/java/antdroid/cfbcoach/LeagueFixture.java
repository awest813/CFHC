package antdroid.cfbcoach;

import android.content.Context;
import android.content.Intent;

import java.io.File;

import simulation.FileSystemResourceProvider;
import simulation.League;
import simulation.LeagueLaunchCoordinator;
import simulation.LeagueSaveStorage;
import simulation.PlatformResourceProvider;
import simulation.SimRandom;
import simulation.Team;

/** A saved dynasty in the test app's files dir, so MainActivity opens straight to Team Home. */
final class LeagueFixture {

    static final String SLOT = "saveFile0.cfb";

    private LeagueFixture() {
    }

    static String userTeamName;

    static Intent mainIntent(Context context, int theme) {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0xCF4CL);
        League league = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        Team user = league.getTeamList().get(0);
        user.setupUserCoach("Test Coach");
        league.userTeam = user;
        userTeamName = user.getName();
        File slot = LeagueSaveStorage.getNamedInternalFile(context.getFilesDir(), SLOT);
        if (!league.saveLeague(slot)) throw new IllegalStateException("fixture save failed");
        return GameNavigation.createMainIntent(context, LeagueLaunchCoordinator.LaunchRequest.loadInternal(SLOT), theme);
    }
}
