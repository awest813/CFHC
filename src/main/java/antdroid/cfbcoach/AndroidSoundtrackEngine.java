package antdroid.cfbcoach;

import android.content.Context;
import android.media.MediaPlayer;

import simulation.SoundtrackEngine;

/**
 * Android soundtrack engine (BGM parity with the desktop shell). Plays the
 * bundled theme via {@link MediaPlayer} with native looping; the desktop
 * march recordings stay desktop-only to keep the APK lean — tracks without
 * a bundled file degrade to silence while honoring the state contract.
 *
 * <p>The activity owns the lifecycle: created in onCreate, paused/resumed
 * with the activity, released in onDestroy.
 */
public class AndroidSoundtrackEngine implements SoundtrackEngine {

    private final Context appContext;
    private MediaPlayer player;
    private volatile boolean paused = false;
    private volatile float volume = 0.4f;
    private volatile boolean muted = false;
    private volatile Track currentTrack;
    private volatile State state = State.STOPPED;

    public AndroidSoundtrackEngine(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void play(Track track) {
        if (track == currentTrack && state == State.PLAYING) return;
        releasePlayer();
        currentTrack = track;
        paused = false;
        int resId = resIdFor(track);
        if (resId == 0) {
            // Track not bundled on Android (APK size) — stay silent but keep
            // the state contract so screen code can switch tracks freely.
            state = State.PLAYING;
            return;
        }
        player = MediaPlayer.create(appContext, resId);
        if (player == null) {
            state = State.STOPPED;
            return;
        }
        player.setLooping(true);
        applyVolume();
        player.start();
        state = State.PLAYING;
    }

    @Override
    public void pause() {
        if (state == State.PLAYING && player != null && player.isPlaying()) {
            player.pause();
            paused = true;
            state = State.PAUSED;
        }
    }

    @Override
    public void resume() {
        if (state == State.PAUSED && player != null) {
            player.start();
            paused = false;
            state = State.PLAYING;
        }
    }

    @Override
    public void stop() {
        releasePlayer();
        currentTrack = null;
        state = State.STOPPED;
    }

    @Override
    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        applyVolume();
    }

    @Override public float getVolume() { return volume; }

    @Override
    public void setMuted(boolean m) {
        muted = m;
        applyVolume();
    }

    @Override public boolean isMuted() { return muted; }
    @Override public Track getCurrentTrack() { return currentTrack; }
    @Override public State getState() { return state; }
    @Override public float getAmplitude() { return state == State.PLAYING ? 0.5f : 0f; }

    @Override
    public void dispose() {
        releasePlayer();
        currentTrack = null;
        state = State.STOPPED;
    }

    private void applyVolume() {
        MediaPlayer p = player;
        if (p == null) return;
        float v = muted ? 0f : volume;
        try {
            p.setVolume(v, v);
        } catch (IllegalStateException ignored) {
            // Player already released.
        }
    }

    private void releasePlayer() {
        MediaPlayer p = player;
        player = null;
        paused = false;
        if (p == null) return;
        try {
            if (p.isPlaying()) p.stop();
        } catch (IllegalStateException ignored) {
        }
        try {
            p.release();
        } catch (Exception ignored) {
        }
    }

    /**
     * Raw resource per track. Only the Pixabay theme is bundled (2.2MB);
     * the march OGGs (~27MB) remain desktop-only. Adding a track on Android
     * = drop the file in res/raw and reference it here.
     */
    private static int resIdFor(Track track) {
        if (track == Track.DASHBOARD_ORGAN) return R.raw.marching_band;
        return 0;
    }
}
