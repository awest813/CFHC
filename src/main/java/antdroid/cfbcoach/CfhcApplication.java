package antdroid.cfbcoach;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import java.util.concurrent.CopyOnWriteArrayList;

import simulation.PlatformLog;

/**
 * Application entry. Uses the fixed navy / gold dynasty palette from {@code styles.xml}
 * so the UI stays cohesive (management-sim readability). Dynamic Material You theming
 * is intentionally disabled to preserve that consistent sports-dashboard look end-to-end.
 */
public class CfhcApplication extends Application {

    /** Told when the whole app (not just one screen) enters or leaves the foreground. */
    public interface ForegroundListener {
        void onAppForegroundChanged(boolean foreground);
    }

    private final CopyOnWriteArrayList<ForegroundListener> foregroundListeners =
            new CopyOnWriteArrayList<>();
    private int startedActivities = 0;

    /** The app instance, or null if the process uses another Application class (tests). */
    public static CfhcApplication from(Context context) {
        Context app = context.getApplicationContext();
        return app instanceof CfhcApplication ? (CfhcApplication) app : null;
    }

    public boolean isInForeground() {
        return startedActivities > 0;
    }

    public void addForegroundListener(ForegroundListener l) {
        foregroundListeners.addIfAbsent(l);
    }

    public void removeForegroundListener(ForegroundListener l) {
        foregroundListeners.remove(l);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Count started activities: moving between our own screens (e.g. into
        // Recruiting) keeps the count above zero, so background music keeps
        // playing; only leaving the app takes it to zero.
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityStarted(Activity a) {
                if (startedActivities++ == 0) notifyForeground(true);
            }
            @Override public void onActivityStopped(Activity a) {
                if (startedActivities > 0 && --startedActivities == 0) notifyForeground(false);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) { }
            @Override public void onActivityResumed(Activity a) { }
            @Override public void onActivityPaused(Activity a) { }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
            @Override public void onActivityDestroyed(Activity a) { }
        });
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

    private void notifyForeground(boolean foreground) {
        for (ForegroundListener l : foregroundListeners) {
            l.onAppForegroundChanged(foreground);
        }
    }
}
