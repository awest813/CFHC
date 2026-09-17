package antdroid.cfbcoach;

import android.app.Application;

import simulation.PlatformLog;

/**
 * Application entry. Uses the fixed navy / gold dynasty palette from {@code styles.xml}
 * so the UI stays cohesive (management-sim readability). Dynamic Material You theming
 * is intentionally disabled to preserve that consistent sports-dashboard look end-to-end.
 */
public class CfhcApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler systemHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Diagnostics only: log the crash (with the engine's last log lines)
            // before handing back to the system handler so the normal crash
            // flow — dialog, restart, Play Console tombstone — still happens.
            PlatformLog.e("CFHC", "Uncaught exception on thread " + thread.getName(), throwable);
            if (systemHandler != null) {
                systemHandler.uncaughtException(thread, throwable);
            }
        });
    }
}
