package simulation;

import java.util.List;
import java.util.Random;

/**
 * League-scoped deterministic RNG for the simulation engine.
 *
 * <p>All engine randomness flows through this single stream so a season can be
 * replayed exactly: the seed is chosen when a league is created, persisted in
 * the save header (see {@link SaveManager}), and re-bound on load. Under the
 * {@code docs/THREADING.md} single-thread contract there is one active league
 * per process, so a process-wide stream is safe.
 */
public final class SimRandom {

    private static final Random RANDOM = new Random();
    private static long seed;

    private SimRandom() {
    }

    /** Binds the stream to a league seed and restarts the sequence. */
    public static void bind(long leagueSeed) {
        seed = leagueSeed;
        RANDOM.setSeed(leagueSeed);
    }

    /** The seed currently bound to this stream (0 until a league binds one). */
    public static long currentSeed() {
        return seed;
    }

    public static double nextDouble() {
        return RANDOM.nextDouble();
    }

    public static int nextInt(int bound) {
        return RANDOM.nextInt(bound);
    }

    public static long nextLong() {
        return RANDOM.nextLong();
    }

    public static boolean nextBoolean() {
        return RANDOM.nextBoolean();
    }

    public static double nextGaussian() {
        return RANDOM.nextGaussian();
    }

    /** Deterministic in-place shuffle using the league stream. */
    public static void shuffle(List<?> list) {
        java.util.Collections.shuffle(list, RANDOM);
    }

    /** Generates a new league seed from environmental entropy. */
    public static long freshSeed() {
        if (pinnedSeed != null) {
            long s = pinnedSeed;
            pinnedSeed = null;
            return s;
        }
        return new Random().nextLong();
    }

    /**
     * Forces the next {@link #freshSeed()} to return the given value. League
     * constructors call {@code freshSeed()}, so pinning before construction makes
     * the entire league (rosters included) deterministic — used by replay tests.
     */
    public static void pinNextSeed(long leagueSeed) {
        pinnedSeed = leagueSeed;
    }

    private static Long pinnedSeed;
}
