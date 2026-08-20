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

    /** Music tracks — public-domain march recordings when OGGs are bundled. */
    enum Track {
        /** "Washington Post March" (Sousa) — default dashboard ambience. */
        DASHBOARD_ORGAN("Washington Post March \u2014 U.S. Army Band"),
        /** "The Stars and Stripes Forever" (Sousa) — postseason / scoreboard. */
        FIGHT_SONG("Stars and Stripes Forever \u2014 USMC Band"),
        /** "National Emblem" (Bagley) — offseason menu music. */
        OFFSEASON_CALM("National Emblem March \u2014 U.S. Army Band"),
        /** "Semper Fidelis" (Sousa) — recruiting screen. */
        RECRUITING_GROOVE("Semper Fidelis March \u2014 U.S. Navy Band");

        private final String displayName;
        Track(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
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
        @Override public float getVolume() { return 0.7f; }
        @Override public void setMuted(boolean muted) {}
        @Override public boolean isMuted() { return false; }
        @Override public Track getCurrentTrack() { return null; }
        @Override public State getState() { return State.STOPPED; }
        @Override public void dispose() {}
        @Override public float getAmplitude() { return 0f; }
    };
}
