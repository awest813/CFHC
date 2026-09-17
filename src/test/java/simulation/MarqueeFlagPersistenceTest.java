package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Regression tests for marquee-game flag persistence: senior day, homecoming,
 * and rivalry flags must survive a save/load round trip, so a mid-season save
 * does not silently drop the crowd modifiers, banners, attendance bonuses, and
 * rivalry trophy bookkeeping of the remaining unplayed games.
 */
public class MarqueeFlagPersistenceTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private League original;
    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        resources = new FileSystemResourceProvider(projectRoot);

        original = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        original.setPlatformResourceProvider(resources);
    }

    @Test
    public void roundTrip_marqueeFlags_arePreserved() throws Exception {
        // Tag a fresh schedule, then force every marquee flag on so the test
        // does not depend on which matchups the generated schedule produced.
        int tagged = 0;
        for (Team t : original.getTeamList()) {
            for (Game g : t.getGameSchedule()) {
                if (g == null || g.isByeWeek()) continue;
                g.seniorDay = true;
                g.homecomingGame = true;
                g.rivalryGame = true;
                tagged++;
            }
            break; // flags are per Game object (shared across both teams); one team's schedule is enough
        }
        assertTrue("Expected scheduled games to tag", tagged > 0);

        League loaded = saveAndLoad();

        int checked = 0;
        for (Team t : loaded.getTeamList()) {
            for (Game g : t.getGameSchedule()) {
                if (g == null || g.isByeWeek()) continue;
                assertTrue("seniorDay lost on load", g.seniorDay);
                assertTrue("homecomingGame lost on load", g.homecomingGame);
                assertTrue("rivalryGame lost on load", g.rivalryGame);
                checked++;
            }
            break;
        }
        assertEquals("Game count changed across round trip", tagged, checked);
    }

    @Test
    public void roundTrip_flagPattern_isPreservedExactly() throws Exception {
        // A fresh schedule already carries real marquee tags (tagMarqueeGames
        // runs at schedule build). Whatever pattern exists must round-trip
        // exactly — not all-true, not all-false, just identical.
        java.util.Map<String, String> before = flagPattern(original);
        assertTrue("Expected marquee tags on a fresh schedule",
                before.values().stream().anyMatch(v -> v.contains("1")));

        java.util.Map<String, String> after = flagPattern(saveAndLoad());
        assertEquals(before, after);
    }

    /** Keyed by week|home|away|gameName, value = three flags as 0/1 string. */
    private java.util.Map<String, String> flagPattern(League league) {
        java.util.Map<String, String> out = new java.util.TreeMap<>();
        for (Team t : league.getTeamList()) {
            for (Game g : t.getGameSchedule()) {
                if (g == null || g.isByeWeek()) continue;
                String key = g.week + "|" + g.homeTeam.getName() + "|" + g.awayTeam.getName() + "|" + g.gameName;
                String flags = (g.seniorDay ? "1" : "0") + (g.homecomingGame ? "1" : "0") + (g.rivalryGame ? "1" : "0");
                out.put(key, flags);
            }
        }
        return out;
    }

    @Test
    public void gameRecordSaveLine_oldFormat_withoutFlagFields_stillLoads() {
        // A GM line written before the flags existed (8 fields) must load with
        // all flags false instead of throwing.
        LeagueRecord.GameRecord rec = LeagueRecord.GameRecord.fromSaveLine(
                "3\tAlabama\tAuburn\tIron Bowl\t9\t0\t0\t0");
        assertFalse(rec.seniorDay());
        assertFalse(rec.homecomingGame());
        assertFalse(rec.rivalryGame());
    }

    @Test
    public void gameRecordSaveLine_flagFields_roundTripThroughString() {
        LeagueRecord.GameRecord rec = new LeagueRecord.GameRecord(
                1, "Alabama", "Auburn", "Iron Bowl", 9, false, 0, 0, true, false, true);
        LeagueRecord.GameRecord parsed = LeagueRecord.GameRecord.fromSaveLine(rec.toSaveLine());
        assertEquals(rec, parsed);
        assertTrue(parsed.seniorDay());
        assertTrue(parsed.rivalryGame());
        assertFalse(parsed.homecomingGame());
    }

    private League saveAndLoad() throws Exception {
        File save = tmp.newFile("league_marquee_test.cfb");
        assertTrue("League must save successfully", original.saveLeague(save));

        League loaded = new League(save,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        loaded.setPlatformResourceProvider(resources);
        return loaded;
    }
}
