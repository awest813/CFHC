package simulation;

import org.junit.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Conference.playWeek plays schedule index (currentWeek - 1) for every team,
 * so a game must sit at the same index in both teams' schedules. OOC games
 * used to be appended after conference games, which put about a quarter of
 * all games at different indexes: teams played two games some weeks and none
 * in others.
 */
public class ScheduleConsistencyTest {

    private static League league(long seed) {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(seed);
        League l = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        l.setPlatformResourceProvider(r);
        return l;
    }

    @Test
    public void everyGameSitsAtTheSameWeekForBothTeams() {
        for (long seed : new long[]{1L, 42L, 0xC0FFEEL, 987654321L}) {
            League l = league(seed);
            Set<Game> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            int games = 0;
            for (Team t : l.getTeamList()) {
                List<Game> s = t.getGameSchedule();
                assertEquals("seed " + seed + " " + t.getName() + " schedule slots",
                        League.REGULAR_SEASON_GAMES, s.size());
                for (int i = 0; i < s.size(); i++) {
                    Game g = s.get(i);
                    if (g.isByeWeek() || !seen.add(g)) continue;
                    games++;
                    Team other = g.homeTeam == t ? g.awayTeam : g.homeTeam;
                    int j = other.getGameSchedule().indexOf(g);
                    if (j >= 0) {
                        assertEquals("seed " + seed + ": " + t.getName() + " vs " + other.getName()
                                + " (" + g.gameName + ")", i, j);
                    }
                }
            }
            assertTrue(games > 700);
        }
    }

    @Test
    public void aWeekPlaysOneGamePerTeam() {
        League l = league(7L);
        l.currentWeek = 1;
        l.playWeek();
        for (Team t : l.getTeamList()) {
            int played = 0;
            for (Game g : t.getGameSchedule()) if (g.hasPlayed && !g.isByeWeek()) played++;
            assertTrue(t.getName() + " played " + played + " games in week 1", played <= 1);
        }
    }
}
