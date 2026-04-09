package simulation;

/**
 * Simple logger shim so the simulation package no longer depends on Android's Log API.
 */
public final class PlatformLog {
    private PlatformLog() {
    }

    public static void d(String tag, String message) {
        System.out.println("[" + tag + "] " + message);
    }
}
