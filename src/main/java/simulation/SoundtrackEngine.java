package simulation;

/**
 * Background-music / soundtrack engine. Separate from {@link AudioManager}
 * (which handles one-shot sound effects) because BGM has different lifecycle
 * needs: long-running looping playback, pause/resume, and an independent
 * volume channel.
 *
 * <p>Implementations should be safe to call from any thread; desktop Swing
 * code calls these from the EDT and the engine manages its own audio thread.
 *
 * <p>The {@link #NO_OP} singleton is used when audio is unavailable (tests,
 * headless environments).
 */
public interface SoundtrackEngine {

    /** Music tracks — bundled recordings (Pixabay / public-domain marches). */
    enum Track {
        /** "Marching Band" (stereo_color, Pixabay) — default dashboard ambience. */
        DASHBOARD_ORGAN("Marching Band \u2014 stereo_color (Pixabay)", -5.7f),
        /** "The Stars and Stripes Forever" (Sousa) — postseason / scoreboard. */
        FIGHT_SONG("Stars and Stripes Forever \u2014 USMC Band", 0f),
        /** "National Emblem" (Bagley) — offseason menu music. */
        OFFSEASON_CALM("National Emblem March \u2014 U.S. Army Band", -3.4f),
        /** "Semper Fidelis" (Sousa) — recruiting screen. */
        RECRUITING_GROOVE("Semper Fidelis March \u2014 U.S. Navy Band", -1.8f);

        private final String displayName;
        private final float gainTrimDb;

        Track(String displayName, float gainTrimDb) {
            this.displayName = displayName;
            this.gainTrimDb = gainTrimDb;
        }

        public String getDisplayName() { return displayName; }

        /**
         * Loudness-matching trim in dB. The recordings span −12.1 to −17.8 LUFS
         * and all peak near 0 dBFS, so the louder ones are trimmed down to the
         * quietest (−17.8 LUFS) — switching screens no longer jumps ~6 LU.
         */
        public float getGainTrimDb() { return gainTrimDb; }

        /** {@link #getGainTrimDb()} as a linear amplitude multiplier. */
        public float getGainTrim() { return (float) Math.pow(10.0, gainTrimDb / 20.0); }
    }

    enum State { PLAYING, PAUSED, STOPPED }

    /**
     * Start playing the given track. If a different track is already playing,
     * it is stopped first (hard cut — no crossfade yet). If the same track is
     * already playing, this is a no-op.
     */
    void play(Track track);

    /** Pause playback (silences output but keeps the track position). */
    void pause();

    /** Resume from pause. No-op if not paused. */
    void resume();

    /** Fully stop playback and reset position. */
    void stop();

    /** Music volume, independent of SFX. Range 0.0–1.0. */
    void setVolume(float volume);
    float getVolume();

    void setMuted(boolean muted);
    boolean isMuted();

    Track getCurrentTrack();
    State getState();

    /** Release all audio resources. */
    void dispose();

    /** Returns the current output amplitude (0.0–1.0) for visualizer UIs. */
    default float getAmplitude() { return 0f; }

    /** Null-object implementation for tests / headless environments. */
    SoundtrackEngine NO_OP = new SoundtrackEngine() {
        @Override public void play(Track track) {}
        @Override public void pause() {}
        @Override public void resume() {}
        @Override public void stop() {}
        @Override public void setVolume(float volume) {}
        @Override public float getVolume() { return AudioLevels.DEFAULT_MUSIC_VOLUME; }
        @Override public void setMuted(boolean muted) {}
        @Override public boolean isMuted() { return false; }
        @Override public Track getCurrentTrack() { return null; }
        @Override public State getState() { return State.STOPPED; }
        @Override public void dispose() {}
        @Override public float getAmplitude() { return 0f; }
    };
}
