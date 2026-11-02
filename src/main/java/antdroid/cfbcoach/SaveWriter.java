package antdroid.cfbcoach;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import simulation.LeagueSaveStorage;
import simulation.PlatformLog;

/**
 * Writes save bytes to storage on one background thread, so slow storage
 * cannot freeze the UI (ANR). Callers snapshot the league on the UI thread
 * with {@code League.serializeSave()} first, because the league is not
 * thread-safe (docs/THREADING.md). Only the file I/O happens here.
 *
 * <p>One process-wide queue keeps writes in order and lets a recreated
 * activity wait for the write its predecessor started before reading the file
 * back.
 */
final class SaveWriter {

    interface Callback {
        /** Runs on the main thread. */
        void onWritten(boolean ok);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CFHC-save-io");
        t.setDaemon(false); // let an in-flight save finish
        return t;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SaveWriter() {
    }

    static void write(final File target, final byte[] data, final Callback callback) {
        IO.execute(() -> {
            boolean ok = LeagueSaveStorage.writeAtomically(target, data);
            if (callback != null) {
                MAIN.post(() -> callback.onWritten(ok));
            }
        });
    }

    /** Blocks until every write queued so far has finished, or the timeout passes. */
    static void awaitPendingWrites(long timeoutMs) {
        Future<?> marker = IO.submit(() -> { });
        try {
            marker.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            PlatformLog.w("SaveWriter", "Timed out waiting for pending saves: " + e);
        }
    }
}
