package simulation;

/**
 * One volume curve for every platform and channel. A slider value in [0,1]
 * maps to a linear amplitude of {@code v²} (≈ perceptually even steps), and
 * to decibels for mixers that take gain in dB. Desktop and Android both go
 * through here so the same slider value sounds the same everywhere.
 */
public final class AudioLevels {

    /** Default SFX slider (≈ −3.9 dB). */
    public static final float DEFAULT_SFX_VOLUME = 0.8f;
    /**
     * Default music slider (≈ −5 dB before the per-track loudness trim in
     * {@link SoundtrackEngine.Track#getGainTrimDb()}) — music sits under the SFX.
     */
    public static final float DEFAULT_MUSIC_VOLUME = 0.75f;
    /** Floor used for "silent" on dB gain controls. */
    public static final float SILENT_DB = -80f;

    private AudioLevels() {
    }

    public static float clamp(float slider) {
        if (Float.isNaN(slider)) return 0f;
        return Math.max(0f, Math.min(1f, slider));
    }

    /** Linear amplitude multiplier for a slider value. */
    public static float amplitude(float slider) {
        float v = clamp(slider);
        return v * v;
    }

    /** Gain in dB for a slider value ({@link #SILENT_DB} at 0). */
    public static float decibels(float slider) {
        float a = amplitude(slider);
        return a > 0f ? (float) (20.0 * Math.log10(a)) : SILENT_DB;
    }
}
