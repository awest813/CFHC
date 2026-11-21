package simulation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Headless timing harness for the engine: new-league construction, simulated
 * games and weeks, whole seasons (offseason and recruiting included), save
 * serialization and load, save size and retained heap.
 *
 * <p>It drives a league the way the apps do, through {@link SeasonController}
 * with the recruiting gate cleared as if the user signed nobody, so its numbers
 * match what a player sees. It changes no engine state beyond the league it
 * builds. Used by the desktop {@code bench} command and by
 * {@code EnginePerfRegressionTest}.
 */
public final class PerfBenchmark {

    /** Timings in milliseconds unless noted. */
    public static final class Result {
        public long seed;
        public int seasons;
        public double newLeagueMs;
        public double perGameMs;
        public double perRegularWeekMs;
        public double maxStepMs;
        public double firstSeasonMs;
        public double allSeasonsMs;
        public long gamesPlayed;
        public double serializeMs;
        public double loadMs;
        public int saveBytes;
        public long heapAfterBytes;
        /** The same, for a save taken in the postseason (box scores at their largest). */
        public double midSerializeMs;
        public double midLoadMs;
        public int midSaveBytes;
        public int midWeek;

        public void print(PrintStream out) {
            out.printf(Locale.US, "seed                 %d%n", seed);
            out.printf(Locale.US, "new league           %8.1f ms%n", newLeagueMs);
            out.printf(Locale.US, "per game             %8.3f ms  (%d games)%n", perGameMs, gamesPlayed);
            out.printf(Locale.US, "per regular week     %8.1f ms%n", perRegularWeekMs);
            out.printf(Locale.US, "slowest step         %8.1f ms%n", maxStepMs);
            out.printf(Locale.US, "first season         %8.1f ms  (incl. offseason + recruiting)%n", firstSeasonMs);
            out.printf(Locale.US, "%2d seasons           %8.1f ms%n", seasons, allSeasonsMs);
            out.printf(Locale.US, "serialize save       %8.1f ms%n", serializeMs);
            out.printf(Locale.US, "load save            %8.1f ms%n", loadMs);
            out.printf(Locale.US, "save size            %8.1f KB%n", saveBytes / 1024.0);
            out.printf(Locale.US, "week-%d save: serialize %.1f ms, load %.1f ms, %.1f KB%n",
                    midWeek, midSerializeMs, midLoadMs, midSaveBytes / 1024.0);
            out.printf(Locale.US, "heap after %2d seasons %7.1f MB  (league retained, after GC)%n",
                    seasons, heapAfterBytes / (1024.0 * 1024.0));
        }
    }

    private final PlatformResourceProvider resources;

    public PerfBenchmark(PlatformResourceProvider resources) {
        this.resources = resources;
    }

    public League newLeague(long seed) {
        SimRandom.pinNextSeed(seed);
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

    /** One headless step; the recruiting gate is cleared as the app does when the user signs nobody. */
    public static void step(League league, SeasonController controller) {
        if (league.recruitingPhaseActive
                && SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks)) {
            league.recruitPlayers();
            league.finishRecruitingSeason("");
        } else {
            controller.advanceWeek();
        }
    }

    /** Steps until the league rolls over into a new season (week returns to 0). */
    public static int playSeason(League league, SeasonController controller, double[] maxStepMs) {
        int steps = 0;
        while (true) {
            int before = league.currentWeek;
            long t0 = System.nanoTime();
            step(league, controller);
            double ms = (System.nanoTime() - t0) / 1e6;
            if (maxStepMs != null && ms > maxStepMs[0]) {
                maxStepMs[0] = ms;
            }
            steps++;
            if (before != 0 && league.currentWeek == 0) {
                return steps;
            }
            if (steps > 400) {
                throw new IllegalStateException("season did not roll over");
            }
        }
    }

    private static long gamesPlayed(League league) {
        long played = 0;
        for (Team t : league.getTeamList()) {
            played += t.getWins() + t.getLosses();
        }
        return played / 2;
    }

    public Result run(long seed, int seasons) throws IOException {
        PlatformLog.setDebugEnabled(false);
        Result r = new Result();
        r.seed = seed;
        r.seasons = seasons;

        long t0 = System.nanoTime();
        League league = newLeague(seed);
        r.newLeagueMs = (System.nanoTime() - t0) / 1e6;

        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        double[] maxStep = {0};

        // Regular-season weeks: time each game week on its own.
        double regularMs = 0;
        int regularWeeks = 0;
        long games = 0;
        long seasonStart = System.nanoTime();
        long allStart = seasonStart;
        while (league.currentWeek < league.regSeasonWeeks - 1) {
            boolean gameWeek = league.currentWeek >= 1;
            long g0 = gamesPlayed(league);
            long w0 = System.nanoTime();
            step(league, controller);
            double ms = (System.nanoTime() - w0) / 1e6;
            maxStep[0] = Math.max(maxStep[0], ms);
            if (gameWeek) {
                regularMs += ms;
                regularWeeks++;
                games += gamesPlayed(league) - g0;
            }
        }
        // Run to the postseason, where a weekly autosave is at its largest.
        r.midWeek = league.regSeasonWeeks + 2;
        while (league.currentWeek < r.midWeek) {
            long w0 = System.nanoTime();
            step(league, controller);
            maxStep[0] = Math.max(maxStep[0], (System.nanoTime() - w0) / 1e6);
        }
        long pause = System.nanoTime();
        byte[] midData = league.serializeSave();
        long m0 = System.nanoTime();
        for (int i = 0; i < 3; i++) {
            midData = league.serializeSave();
        }
        r.midSerializeMs = (System.nanoTime() - m0) / 1e6 / 3;
        r.midSaveBytes = midData.length;
        long paused = System.nanoTime() - pause;
        seasonStart += paused;
        allStart += paused;
        playSeason(league, controller, maxStep);
        r.firstSeasonMs = (System.nanoTime() - seasonStart) / 1e6;
        r.perRegularWeekMs = regularWeeks > 0 ? regularMs / regularWeeks : 0;
        r.gamesPlayed = games;
        r.perGameMs = games > 0 ? regularMs / games : 0;

        for (int s = 1; s < seasons; s++) {
            playSeason(league, controller, maxStep);
        }
        r.allSeasonsMs = (System.nanoTime() - allStart) / 1e6;
        r.maxStepMs = maxStep[0];

        // Save: warm up, then average a few serializations.
        byte[] data = league.serializeSave();
        int reps = 5;
        long s0 = System.nanoTime();
        for (int i = 0; i < reps; i++) {
            data = league.serializeSave();
        }
        r.serializeMs = (System.nanoTime() - s0) / 1e6 / reps;
        r.saveBytes = data.length;

        File f = File.createTempFile("cfhc-bench", ".cfb");
        try {
            Files.write(f.toPath(), data);
            String rng = SimRandom.exportState();
            load(f);
            long l0 = System.nanoTime();
            for (int i = 0; i < 3; i++) {
                load(f);
            }
            r.loadMs = (System.nanoTime() - l0) / 1e6 / 3;
            SimRandom.restoreState(rng);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }

        File mf = File.createTempFile("cfhc-bench-mid", ".cfb");
        try {
            Files.write(mf.toPath(), midData);
            String rng = SimRandom.exportState();
            long l0 = System.nanoTime();
            for (int i = 0; i < 3; i++) {
                load(mf);
            }
            r.midLoadMs = (System.nanoTime() - l0) / 1e6 / 3;
            SimRandom.restoreState(rng);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            mf.delete();
        }
        midData = null;

        r.heapAfterBytes = retainedHeap();
        // Keep the league reachable until the heap has been measured.
        if (league.getTeamList().isEmpty()) {
            throw new IllegalStateException();
        }
        return r;
    }

    private League load(File f) {
        League loaded = new League(f,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                GameUiBridge.NO_OP, true);
        loaded.setPlatformResourceProvider(resources);
        loaded.rebuildScheduleIfNeeded();
        loaded.refreshAfterLoad();
        return loaded;
    }

    private static long retainedHeap() {
        Runtime rt = Runtime.getRuntime();
        long used = Long.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            used = Math.min(used, rt.totalMemory() - rt.freeMemory());
        }
        return used;
    }
}
