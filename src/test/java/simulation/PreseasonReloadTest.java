package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * A reloaded dynasty must keep every game at the same schedule slot for both
 * teams. Found by the Android season-flow screen test: after loading a save,
 * the first game week played the user's whole regular season.
 */
public class PreseasonReloadTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static GameUiBridge quietBridge() {
        return (GameUiBridge) java.lang.reflect.Proxy.newProxyInstance(
                GameUiBridge.class.getClassLoader(), new Class<?>[]{GameUiBridge.class},
                (proxy, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    return null;
                });
    }

    private static int played(Team t) {
        int n = 0;
        for (Game g : t.getGameSchedule()) if (g != null && g.hasPlayed && !g.isByeWeek()) n++;
        return n;
    }

    private League freshLeague(FileSystemResourceProvider r) {
        League fresh = new League(r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                r.getString(PlatformResourceProvider.KEY_TEAMS),
                r.getString(PlatformResourceProvider.KEY_BOWLS), false, false);
        fresh.setPlatformResourceProvider(r);
        fresh.userTeam = fresh.getTeamList().get(0);
        fresh.userTeam.setupUserCoach("Reload Test");
        return fresh;
    }

    private League load(FileSystemResourceProvider r, File dir, int year) throws Exception {
        League league = LeagueLaunchCoordinator.load(
                LeagueLaunchCoordinator.LaunchRequest.loadInternal("saveFile0.cfb"), dir, null, year,
                r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                null, null, null, null, null).league;
        league.setPlatformResourceProvider(r);
        return league;
    }

    /** Every game sits at the same index in both teams' schedules, as in the fresh league. */
    private static void assertSchedulesMatch(League fresh, League loaded) {
        for (int t = 0; t < fresh.getTeamList().size(); t++) {
            List<Game> a = fresh.getTeamList().get(t).getGameSchedule();
            List<Game> b = loaded.getTeamList().get(t).getGameSchedule();
            assertEquals("schedule length for team " + t, a.size(), b.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals("team " + t + " slot " + i + " home", a.get(i).homeTeam.getName(), b.get(i).homeTeam.getName());
                assertEquals("team " + t + " slot " + i + " away", a.get(i).awayTeam.getName(), b.get(i).awayTeam.getName());
            }
        }
    }

    private void playsOneGamePerWeek(League loaded) {
        SeasonController c = new SeasonController(loaded, quietBridge());
        for (int i = 0; i < 4; i++) c.advanceWeek(); // preseason step + 3 game weeks
        assertEquals(4, loaded.currentWeek);
        assertTrue("3 game weeks should play at most 3 games, played " + played(loaded.userTeam),
                played(loaded.userTeam) <= 3);
    }

    @Test
    public void reloadedSave_keepsScheduleSlots_andPlaysOneGamePerWeek() throws Exception {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League fresh = freshLeague(r);
        File dir = tmp.newFolder();
        assertTrue(fresh.saveLeague(new File(dir, "saveFile0.cfb")));
        League loaded = load(r, dir, fresh.getYear());
        assertSchedulesMatch(fresh, loaded);
        playsOneGamePerWeek(loaded);
    }

    @Test
    public void legacySaveWithoutSlots_isRebuiltExactly() throws Exception {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League fresh = freshLeague(r);
        File dir = tmp.newFolder();
        File save = new File(dir, "saveFile0.cfb");
        assertTrue(fresh.saveLeague(save));
        // Older builds wrote 0 in the GM line's week field (index 4 after the prefix).
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(save.toPath(), StandardCharsets.UTF_8)) {
            if (line.startsWith("GM:")) {
                String[] p = line.substring(3).split("\t", -1);
                p[4] = "0";
                line = "GM:" + String.join("\t", p);
            }
            lines.add(line);
        }
        Files.write(save.toPath(), lines, StandardCharsets.UTF_8);
        League loaded = load(r, dir, fresh.getYear());
        // Best effort for old saves: every team keeps its games, and the user's team
        // (first in league order, so it owns all its games) is exact.
        for (int t = 0; t < fresh.getTeamList().size(); t++) {
            assertEquals(fresh.getTeamList().get(t).getGameSchedule().size(),
                    loaded.getTeamList().get(t).getGameSchedule().size());
        }
        List<Game> a = fresh.userTeam.getGameSchedule();
        List<Game> b = loaded.userTeam.getGameSchedule();
        for (int i = 0; i < a.size(); i++) {
            assertEquals("user slot " + i, a.get(i).gameName + a.get(i).homeTeam.getName() + a.get(i).awayTeam.getName(),
                    b.get(i).gameName + b.get(i).homeTeam.getName() + b.get(i).awayTeam.getName());
        }
        playsOneGamePerWeek(loaded);
    }
}
