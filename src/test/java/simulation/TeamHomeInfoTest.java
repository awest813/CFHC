package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Team Home's "last game" text after the preseason step: the week is 1 but no
 * game has been played. Found by the Android Robolectric screen test, where
 * the first Play Week of a season crashed Team Home.
 */
public class TeamHomeInfoTest {

    @Test
    public void weekOneBeforeKickoff_hasNoLastGame() {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        league.setPlatformResourceProvider(r);
        league.currentWeek = 1; // what SeasonController's preseason step leaves behind
        for (Team t : league.getTeamList()) {
            String info = t.getTeamHomeInfo(); // threw IndexOutOfBounds for teams without a week-1 bye
            assertFalse(info.isEmpty());
        }
    }
}
