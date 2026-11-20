package desktop;

import java.util.prefs.Preferences;

import simulation.AudioLevels;

/** Desktop audio preference helpers. */
final class DesktopAudioPrefs {

    private DesktopAudioPrefs() {
    }

    /**
     * Reads a volume slider level. Levels used to be stored under
     * {@code legacyKey} on a linear-amplitude curve; the shared
     * {@link AudioLevels} curve squares the slider, so an old value {@code v}
     * maps to {@code sqrt(v)} to keep the same loudness after the upgrade.
     */
    static float readLevel(Preferences prefs, String key, String legacyKey, float def) {
        try {
            float level = prefs.getFloat(key, Float.NaN);
            if (!Float.isNaN(level)) return AudioLevels.clamp(level);
            float legacy = prefs.getFloat(legacyKey, Float.NaN);
            if (!Float.isNaN(legacy)) {
                float migrated = (float) Math.sqrt(AudioLevels.clamp(legacy));
                prefs.putFloat(key, migrated);
                return migrated;
            }
        } catch (Exception ignored) {
            // Preferences backing store unavailable — fall back to default.
        }
        return def;
    }
}
