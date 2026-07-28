package simulation;

/**
 * Shared user-facing save/load copy and slot-summary classifiers.
 * Keeps Android home, desktop open, and slot lists aligned on invalid vs
 * incompatible vs empty vs unreadable saves.
 */
public final class SaveLoadMessages {
    public static final String EMPTY = "EMPTY";
    public static final String UNREADABLE = "Unreadable save";
    public static final String LEGACY_INCOMPATIBLE = "Legacy Save  Incompatible";
    public static final String UNSUPPORTED_VERSION_PREFIX = "Unsupported Version:";

    public static final String TOAST_EMPTY = "Cannot load an empty save slot.";
    public static final String TOAST_INCOMPATIBLE =
            "Incompatible save — this file cannot be loaded in this version.";
    public static final String TOAST_UNREADABLE =
            "Save file is unreadable or corrupt.";
    public static final String SAVE_FAILED = "Error: Failed to save league!";
    public static final String SAVE_OK = "Saved league!";

    private SaveLoadMessages() {
    }

    public static boolean isEmptySlot(String info) {
        return info == null || info.equals(EMPTY) || info.trim().isEmpty();
    }

    public static boolean isUnreadable(String info) {
        return info != null && info.contains(UNREADABLE);
    }

    public static boolean isIncompatible(String info) {
        if (info == null) {
            return false;
        }
        return info.contains("Legacy Save")
                || info.contains("Incompatible")
                || info.contains(UNSUPPORTED_VERSION_PREFIX)
                || info.contains("Old Save");
    }

    /** True when the slot summary represents a file the UI should attempt to open. */
    public static boolean isLoadable(String info) {
        return !isEmptySlot(info) && !isUnreadable(info) && !isIncompatible(info);
    }

    public static String toastForSlot(String info) {
        if (isEmptySlot(info)) {
            return TOAST_EMPTY;
        }
        if (isUnreadable(info)) {
            return TOAST_UNREADABLE;
        }
        if (isIncompatible(info)) {
            return TOAST_INCOMPATIBLE;
        }
        return TOAST_INCOMPATIBLE;
    }

    /**
     * Maps a load failure to a player-facing explanation.
     * Prefers the exception message when it already names the problem;
     * otherwise falls back to a clear generic load failure.
     */
    public static String loadFailureMessage(Throwable error) {
        if (error == null) {
            return "Unable to load this save. The file may be invalid, unsupported, or missing required data.";
        }
        String msg = error.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return "Unable to load this save. The file may be invalid, unsupported, or missing required data.";
        }
        String lower = msg.toLowerCase();
        if (lower.contains("unsupported") || lower.contains("schema version")) {
            return msg;
        }
        if (lower.contains("missing") && (lower.contains("header") || lower.contains("user team")
                || lower.contains("resource") || lower.contains("stream"))) {
            return msg;
        }
        if (lower.contains("corrupt") || lower.contains("invalid") || lower.contains("truncat")
                || lower.contains("parse") || lower.contains("format")) {
            return "Save file is invalid or corrupt: " + msg;
        }
        if (lower.contains("not found") || lower.contains("no such file")) {
            return "Save file not found.";
        }
        return "Unable to load this save: " + msg;
    }
}
