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
 *
 * <p>The generator is SplitMix64 (Steele, Lea and Flood): its whole state is one
 * 64-bit counter, plus the spare value of the last Gaussian pair, so
 * {@link #exportState()} captures it exactly and a save resumes the stream
 * where it stopped instead of restarting from the seed. ({@code java.util.Random}
 * hides its Gaussian spare, so it could not be saved.) A given seed always
 * produces the same sequence.
 */
public final class SimRandom {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;
    private static final double DOUBLE_UNIT = 0x1.0p-53;

    private static long seed;
    private static long state = mixSeed(0L);
    private static boolean haveSpareGaussian;
    private static double spareGaussian;

    private SimRandom() {
    }

    /** Spreads the seed's bits so nearby seeds (1, 2, 3...) start far apart. */
    private static long mixSeed(long s) {
        return mix64(s ^ 0x6A09E667F3BCC909L);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Binds the stream to a league seed and restarts the sequence. */
    public static void bind(long leagueSeed) {
        seed = leagueSeed;
        state = mixSeed(leagueSeed);
        haveSpareGaussian = false;
        spareGaussian = 0;
    }

    /** The seed currently bound to this stream (0 until a league binds one). */
    public static long currentSeed() {
        return seed;
    }

    /**
     * The stream's full position: {@code state:spare} in hex, where spare is the
     * pending Gaussian's raw bits or {@code -} when none is pending. Saved with the
     * league so a reload continues the sequence exactly.
     */
    public static String exportState() {
        return Long.toHexString(state) + ":"
                + (haveSpareGaussian ? Long.toHexString(Double.doubleToRawLongBits(spareGaussian)) : "-");
    }

    /**
     * Restores a position from {@link #exportState()} without changing the bound seed.
     *
     * @return false (stream unchanged) when the text is missing or unreadable
     */
    public static boolean restoreState(String exported) {
        if (exported == null) {
            return false;
        }
        String[] p = exported.trim().split(":", -1);
        if (p.length != 2 || p[0].isEmpty()) {
            return false;
        }
        try {
            long s = Long.parseUnsignedLong(p[0], 16);
            boolean spare = !"-".equals(p[1]);
            double g = spare ? Double.longBitsToDouble(Long.parseUnsignedLong(p[1], 16)) : 0;
            state = s;
            haveSpareGaussian = spare;
            spareGaussian = g;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static long nextLong() {
        state += GOLDEN_GAMMA;
        return mix64(state);
    }

    public static double nextDouble() {
        return (nextLong() >>> 11) * DOUBLE_UNIT;
    }

    /** Uniform in {@code [0, bound)}; rejection sampling keeps it unbiased. */
    public static int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r = (int) (nextLong() >>> 33); // 31 bits
        int m = bound - 1;
        if ((bound & m) == 0) {
            return (int) ((bound * (long) r) >> 31);
        }
        for (int u = r; u - (r = u % bound) + m < 0; ) {
            u = (int) (nextLong() >>> 33);
        }
        return r;
    }

    public static boolean nextBoolean() {
        return nextLong() < 0;
    }

    /** Standard normal (Marsaglia polar method; the pair's second value is kept as state). */
    public static double nextGaussian() {
        if (haveSpareGaussian) {
            haveSpareGaussian = false;
            return spareGaussian;
        }
        double v1;
        double v2;
        double s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        spareGaussian = v2 * multiplier;
        haveSpareGaussian = true;
        return v1 * multiplier;
    }

    /** Deterministic in-place shuffle using the league stream (Fisher-Yates, as Collections.shuffle). */
    public static void shuffle(List<?> list) {
        shuffleTyped(list);
    }

    private static <T> void shuffleTyped(List<T> list) {
        for (int i = list.size(); i > 1; i--) {
            int j = nextInt(i);
            T tmp = list.get(i - 1);
            list.set(i - 1, list.get(j));
            list.set(j, tmp);
        }
    }

    /**
     * Runs {@code body} on a side stream derived from the bound seed and
     * {@code streamKey}, then puts the league stream back exactly where it was.
     * For content a screen may build at any time (the recruiting board): it comes
     * out the same whenever it is built, and building it never moves the season's
     * stream (so a reload still matches an unbroken run).
     */
    public static <T> T onSideStream(long streamKey, java.util.function.Supplier<T> body) {
        long savedState = state;
        boolean savedHave = haveSpareGaussian;
        double savedSpare = spareGaussian;
        state = mix64(mixSeed(seed) ^ mix64(streamKey + GOLDEN_GAMMA));
        haveSpareGaussian = false;
        spareGaussian = 0;
        try {
            return body.get();
        } finally {
            state = savedState;
            haveSpareGaussian = savedHave;
            spareGaussian = savedSpare;
        }
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
