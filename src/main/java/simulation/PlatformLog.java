package simulation;

/**
 * Simple logger shim so the simulation package no longer depends on Android's Log API.
 */
public final class PlatformLog {
    private PlatformLog() {
    }

    private static volatile boolean debugEnabled = true;

    /** Gates only {@link #d}; warnings and errors always print. Dev tools use this to quiet schedule spam. */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void d(String tag, String message) {
        if (!debugEnabled) {
            return;
        }
        System.out.println("[D/" + tag + "] " + message);
    }

    public static void i(String tag, String message) {
        System.out.println("[I/" + tag + "] " + message);
    }

    public static void w(String tag, String message) {
        System.err.println("[W/" + tag + "] " + message);
    }

    public static void w(String tag, String message, Throwable t) {
        System.err.println("[W/" + tag + "] " + message);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    public static void e(String tag, String message) {
        System.err.println("[E/" + tag + "] " + message);
    }

    public static void e(String tag, String message, Throwable t) {
        System.err.println("[E/" + tag + "] " + message);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }
}
