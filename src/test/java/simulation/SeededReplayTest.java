package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import staff.HeadCoach;

import static org.junit.Assert.*;

/**
 * Phase 0 determinism gate (see docs/game-flow-audit-and-premium-plan.md, D10/D16).
 *
 * <p>Two leagues constructed under the same pinned {@link SimRandom} seed must play
 * out an identical full year — regular season, postseason, offseason, and the
 * recruiting rollover via {@link SeasonController#autoCompleteRecruiting()} — with
 * no UI bridge. Also verifies the seed survives a save/load round trip.
 */
public class SeededReplayTest {

    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    }

    private League newSeededLeague(long seed) {
        SimRandom.pinNextSeed(seed);
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        assertEquals("League constructor should adopt the pinned seed", seed, league.getRngSeed());
        return league;
    }

    /** Plays from week 0 through the recruiting gate, then rolls the year headlessly.
     * Returns the outcome fingerprint captured at end of season (before stat reset). */
    private String playFullYear(SeasonController controller, League league) {
        // advanceWeek until the recruiting gate latches (week >= R+13), then roll over.
        for (int steps = 0; steps < 40; steps++) {
            controller.advanceWeek();
            if (SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks)
                    && league.recruitingPhaseActive) {
                break;
            }
        }
        assertTrue("Should reach the recruiting gate within the step budget",
                SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks));
        String fingerprint = digest(league);
        assertTrue("autoCompleteRecruiting should roll the year over", controller.autoCompleteRecruiting());
        assertEquals("Year should have rolled over to preseason", 0, league.currentWeek);
        assertEquals("Seed must survive the rollover", league.getRngSeed(), SimRandom.currentSeed());
        return fingerprint;
    }

    /** Team name + W-L + points, sorted — the season outcome fingerprint. */
    private String digest(League league) {
        List<String> rows = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            rows.add(String.format("%s|%d-%d|%d", t.getName(), t.getWins(), t.getLosses(), t.getTeamPoints()));
        }
        Collections.sort(rows);
        return String.join("\n", rows);
    }

    @Test
    public void sameSeed_producesIdenticalFullYear() {
        long seed = 0xCF0801L;

        League a = newSeededLeague(seed);
        SeasonController ca = new SeasonController(a, GameUiBridge.NO_OP);
        String seasonA = playFullYear(ca, a);

        League b = newSeededLeague(seed);
        SeasonController cb = new SeasonController(b, GameUiBridge.NO_OP);
        String seasonB = playFullYear(cb, b);

        assertEquals("Same seed must replay the identical season", seasonA, seasonB);
        assertEquals(a.getYear(), b.getYear());
    }

    @Test
    public void differentSeeds_diverge() {
        League a = newSeededLeague(101L);
        SeasonController ca = new SeasonController(a, GameUiBridge.NO_OP);
        String seasonA = playFullYear(ca, a);

        League b = newSeededLeague(202L);
        SeasonController cb = new SeasonController(b, GameUiBridge.NO_OP);
        String seasonB = playFullYear(cb, b);

        assertNotEquals("Different seeds should (virtually always) diverge", seasonA, seasonB);
    }

    @Test
    public void counterStrategyAdjustments_doNotLeakBetweenGames() {
        League league = newSeededLeague(0xB00CAL);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);

        // Snapshot every team's season-long playbooks, then sim half a season of
        // games in which tactical HCs (ratOff/ratDef > 83) roll counter-adjustments.
        java.util.Map<String, int[]> books = new java.util.HashMap<>();
        for (Team t : league.getTeamList()) {
            books.put(t.getName(), new int[]{t.getPlaybookOffNum(), t.getPlaybookDefNum()});
        }
        for (Team t : league.getTeamList()) {
            HeadCoach hc = t.getHeadCoach();
            if (hc != null) {
                hc.ratOff = 99;
                hc.ratDef = 99;
            }
        }

        league.currentWeek = 1; // playWeek is an in-season step (week 0 is the preseason transition)
        for (int w = 0; w < league.regSeasonWeeks / 2; w++) {
            league.playWeek();
            league.currentWeek = Math.min(league.currentWeek + 1, league.regSeasonWeeks - 1);
        }

        for (Team t : league.getTeamList()) {
            int[] original = books.get(t.getName());
            assertEquals("Offensive playbook leaked for " + t.getName(),
                    original[0], t.getPlaybookOffNum());
            assertEquals("Defensive playbook leaked for " + t.getName(),
                    original[1], t.getPlaybookDefNum());
        }
    }

    @Test
    public void seed_survivesSaveLoadRoundTrip() throws Exception {
        long seed = 0xFEEDL;
        League league = newSeededLeague(seed);
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        controller.advanceWeek(); // preseason transition
        controller.advanceWeek(); // week 1 played

        File tmp = File.createTempFile("cfhc-seed-roundtrip", ".cfb");
        try {
            assertTrue(league.saveLeague(tmp));
            League loaded = new League(tmp,
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    GameUiBridge.NO_OP,
                    true);
            loaded.rebuildScheduleIfNeeded();
            assertEquals("Loaded league must restore the persisted RNG seed", seed, loaded.getRngSeed());
            assertEquals(SimRandom.currentSeed(), loaded.getRngSeed());
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }
}
