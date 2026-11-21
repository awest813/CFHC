package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import positions.Player;
import staff.HeadCoach;
import staff.OC;
import staff.Staff;

import static org.junit.Assert.*;

/**
 * Career arrays (stats, awards, history) must not be shared between two objects.
 * A coach copied into a new job or the pool kept the original's arrays, and a Hall
 * of Fame entry kept the departing player's: the original (still held as Coach of
 * the Year, a rising star or the leaving player) then changed along with the copy
 * in an unbroken run, but not after a reload, where the save had written them
 * apart. Each case here makes the copy, saves and reloads, changes the copy the
 * way the season does in both leagues, and requires the two saves to match.
 */
public class SaveCareerAliasingTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider resources =
            new FileSystemResourceProvider(System.getProperty("user.dir"));

    private League newLeague() {
        SimRandom.pinNextSeed(0xA11A5L);
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        Team user = league.getTeamList().get(0);
        user.setUserControlled(true);
        user.getHeadCoach().user = true;
        league.userTeam = user;
        return league;
    }

    private League load(byte[] save) throws Exception {
        File f = tmp.newFile();
        Files.write(f.toPath(), save);
        League loaded = new League(f,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                GameUiBridge.NO_OP, true);
        loaded.setPlatformResourceProvider(resources);
        loaded.rebuildScheduleIfNeeded();
        loaded.refreshAfterLoad();
        return loaded;
    }

    private static String save(League l) throws java.io.IOException {
        return new String(l.serializeSave(), StandardCharsets.UTF_8);
    }

    private static Team cpuTeam(League l) {
        return l.getTeamList().get(5);
    }

    /** What the season does to a head coach after a hire: a result, an award, a history line. */
    private static void coachSeason(League l) {
        HeadCoach hc = cpuTeam(l).getHeadCoach();
        hc.recordWins(9);
        hc.recordLosses(3);
        hc.awards[0]++;
        cpuTeam(l).updateCoachHistory(hc);
    }

    @Test
    public void rehiredCoach_doesNotShareCareerWithTheOriginal() throws Exception {
        League a = newLeague();
        Team t = cpuTeam(a);
        HeadCoach original = t.getHeadCoach();
        original.recordWins(5);
        // The original stays referenced as Coach of the Year and a rising star while
        // the carousel hires a copy of him (coachCarousel / hireRisingStar).
        a.coachWinner = original;
        a.coachStarList.add(original);
        t.setHeadCoach(new HeadCoach(original, t));
        int[] statsBefore = original.stats.clone();

        League b = load(a.serializeSave());
        assertEquals("the saves match right after loading", save(a), save(b));

        coachSeason(a);
        coachSeason(b);
        assertEquals("unbroken and reloaded runs save the same", save(a), save(b));
        assertEquals(SaveDeterminismTest.fingerprint(a), SaveDeterminismTest.fingerprint(b));
        assertArrayEquals("the Coach of the Year keeps his own record", statsBefore, a.coachWinner.stats);
        assertNotSame(a.coachWinner.stats, cpuTeam(a).getHeadCoach().stats);
    }

    @Test
    public void coordinatorCopies_doNotShareCareer() throws Exception {
        League a = newLeague();
        Team t = cpuTeam(a);
        OC oc = t.getOC();
        oc.recordWins(4);
        // The pool gets a copy of a released coordinator while the team still holds
        // him until the replacement is hired (releaseCoordinator).
        a.releaseCoordinator(oc, t);
        Staff pooled = a.coachFreeAgents.get(a.coachFreeAgents.size() - 1);
        assertNotSame(oc.stats, pooled.stats);
        assertNotSame(oc.history, pooled.history);

        // A pool coach hired as a coordinator (OCCarousel) is a copy too.
        OC hired = new OC(pooled, t);
        assertNotSame(pooled.stats, hired.stats);
        assertNotSame(pooled.awards, hired.awards);
        assertEquals(Arrays.toString(pooled.stats), Arrays.toString(hired.stats));
        assertEquals(pooled.history, hired.history);
    }

    @Test
    public void hallOfFameEntry_doesNotShareStatsWithTheLeavingPlayer() throws Exception {
        League a = newLeague();
        Team t = cpuTeam(a);
        Player star = t.getAllPlayers().get(0);
        star.year = 4;
        star.awards[0] += 4; // enough credentials for the Hall of Fame
        star.awards[1] += 4;
        t.removePlayer(star);
        t.addPlayerLeaving(star);
        t.checkHallofFame();
        assertEquals(1, a.getLeagueHoF().size());
        PlayerRecord entry = a.getLeagueHoF().get(0);
        String entryBefore = Arrays.toString(entry.stats()) + Arrays.toString(entry.awards());

        League b = load(a.serializeSave());
        assertEquals(save(a), save(b));
        // The departing player is still in the league's lists until the next season.
        for (League l : new League[]{a, b}) {
            Player leaving = cpuTeam(l).getPlayersLeaving().get(0);
            leaving.stats[0] += 100;
            leaving.awards[0]++;
        }
        assertEquals("unbroken and reloaded runs save the same", save(a), save(b));
        assertEquals(SaveDeterminismTest.fingerprint(a), SaveDeterminismTest.fingerprint(b));
        assertEquals("the entry keeps the record it was inducted with",
                entryBefore, Arrays.toString(entry.stats()) + Arrays.toString(entry.awards()));
        assertNotSame(star.stats, entry.stats());
        assertNotSame(star.careerStats, entry.careerStats());
    }
}
