package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * The season-summary step must close the season's books (prestige change,
 * records) before the shell builds the summary text.
 */
public class SeasonSummaryOrderTest {

    @Test
    public void summaryIsBuiltAfterThePrestigeIsComputed() {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        league.setPlatformResourceProvider(r);
        Team user = league.getTeamList().get(0);
        user.setupUserCoach("Order Test");
        league.userTeam = user;

        final String[] summary = new String[1];
        GameUiBridge bridge = (GameUiBridge) java.lang.reflect.Proxy.newProxyInstance(
                GameUiBridge.class.getClassLoader(), new Class<?>[]{GameUiBridge.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("showSeasonSummary")) {
                        assertNotNull("prestige must be computed before the summary", user.prestigePts);
                        summary[0] = league.seasonSummaryStr(); // what the shells call
                    }
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
        SeasonController controller = new SeasonController(league, bridge);
        int summaryWeek = SeasonFlowOrder.seasonSummaryWeek(league.regSeasonWeeks);
        for (int i = 0; i < 40 && summary[0] == null && league.currentWeek <= summaryWeek; i++) {
            controller.advanceWeek();
        }
        assertNotNull("season summary should have been shown", summary[0]);
        assertFalse(summary[0].isEmpty());
    }
}
