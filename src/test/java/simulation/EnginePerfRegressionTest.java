package simulation;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Coarse performance guard for the engine, with limits several times the
 * measured cost so a slow or busy CI machine does not trip it. It is meant to
 * catch an accidental O(n^2) loop or a save that balloons, not small drifts:
 * offseason player progression once rebuilt every roster in the league for
 * every player (a 4-6 s step, now ~0.2-0.4 s).
 *
 * <p>Measured on a 4-core cloud VM (two seasons, seed 0x5A7EDE7): new league
 * ~0.25 s, slowest step ~0.4 s, two seasons ~4 s, week-16 save ~0.15-0.3 s,
 * ~5.7 MB, load ~0.15-0.35 s. Run {@code java -jar CFHC-desktop.jar bench} for
 * the full table.
 */
public class EnginePerfRegressionTest {

    @Test
    public void twoSeasonsSaveAndLoadStayWithinBudget() throws Exception {
        PerfBenchmark.Result r = new PerfBenchmark(new FileSystemResourceProvider(System.getProperty("user.dir")))
                .run(0x5A7EDE7L, 2);
        r.print(System.out);
        assertTrue("new league took " + r.newLeagueMs + " ms", r.newLeagueMs < 5_000);
        assertTrue("slowest single step took " + r.maxStepMs + " ms", r.maxStepMs < 3_000);
        assertTrue("two seasons took " + r.allSeasonsMs + " ms", r.allSeasonsMs < 40_000);
        assertTrue("week-" + r.midWeek + " save took " + r.midSerializeMs + " ms", r.midSerializeMs < 3_000);
        assertTrue("week-" + r.midWeek + " load took " + r.midLoadMs + " ms", r.midLoadMs < 4_000);
        assertTrue("week-" + r.midWeek + " save is " + r.midSaveBytes + " bytes", r.midSaveBytes < 12 * 1024 * 1024);
        assertTrue("year-end save is " + r.saveBytes + " bytes", r.saveBytes < 12 * 1024 * 1024);
    }
}
