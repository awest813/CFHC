package simulation;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * {@link League#snapshotSave()} defers compressing the box scores to
 * {@link SaveSnapshot#finish()}, which the Android app runs on its save thread.
 * The finished bytes must equal {@link League#serializeSave()} exactly, and
 * taking a snapshot must not disturb the league or its random stream.
 */
public class SaveSnapshotTest {

    private final PerfBenchmark bench =
            new PerfBenchmark(new FileSystemResourceProvider(System.getProperty("user.dir")));

    @Test
    public void finishedSnapshotMatchesSerializeSave_inPreseasonAndMidSeason() throws Exception {
        League league = bench.newLeague(0xB0B5L);
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        // Preseason: no box scores yet, nothing is deferred.
        assertArrayEquals(league.serializeSave(), league.snapshotSave().finish());

        while (league.currentWeek < 4) {
            PerfBenchmark.step(league, controller);
        }
        String rng = SimRandom.exportState();
        byte[] expected = league.serializeSave();
        SaveSnapshot snapshot = league.snapshotSave();
        assertEquals("snapshot must not draw from the stream", rng, SimRandom.exportState());

        ExecutorService io = Executors.newSingleThreadExecutor();
        try {
            byte[] finished = io.submit(snapshot::finish).get();
            assertArrayEquals(expected, finished);
        } finally {
            io.shutdownNow();
        }
        // And the league still saves the same afterwards.
        assertArrayEquals(expected, league.serializeSave());
    }

    @Test
    public void indexOfOnly_rejectsMissingAndRepeatedTokens() {
        byte[] token = "XY".getBytes();
        assertEquals(2, SaveSnapshot.indexOfOnly("abXYc".getBytes(), token));
        assertEquals(-1, SaveSnapshot.indexOfOnly("abc".getBytes(), token));
        assertEquals(-1, SaveSnapshot.indexOfOnly("XYabXY".getBytes(), token));
    }
}
