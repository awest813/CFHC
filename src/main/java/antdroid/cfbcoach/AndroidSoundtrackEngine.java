package antdroid.cfbcoach;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import simulation.PlatformLog;
import simulation.SoundtrackEngine;

/**
 * Android soundtrack engine (BGM parity with the desktop shell). Plays the
 * bundled theme via {@link MediaPlayer} with native looping; the desktop
 * march recordings stay desktop-only to keep the APK lean — tracks without
 * a bundled file degrade to silence while honoring the state contract.
 *
 * <p>Two layers of state:
 * <ul>
 *   <li>{@link #getState()} is the logical contract the screens see
 *       (PLAYING / PAUSED / STOPPED), driven only by play/pause/resume/stop.</li>
 *   <li>Whether the MediaPlayer is actually running is derived from that state
 *       plus mute and audio focus in {@link #updateOutput()}.</li>
 * </ul>
 *
 * <p>Audio focus is only held while music is actually audible: a muted
 * soundtrack, a paused activity, or a track with no bundled file never
 * requests focus, so the game doesn't pause the player's own podcast or music.
 * Focus is re-requested on every resume/unmute (after a permanent loss the
 * system no longer calls us back), and delayed focus gain covers a phone call
 * in progress at launch.
 *
 * <p>All calls arrive on the main thread (activity lifecycle and focus
 * callbacks both use the main looper).
 */
public class AndroidSoundtrackEngine implements SoundtrackEngine {

    private static final String TAG = "AndroidSoundtrackEngine";
    /** Volume multiplier while another app holds transient "may duck" focus. */
    private static final float DUCK_FACTOR = 0.25f;

    private final Context appContext;
    private final AudioManager audioManager;
    private MediaPlayer player;
    private float volume = simulation.AudioLevels.DEFAULT_MUSIC_VOLUME;
    private boolean muted = false;
    private Track currentTrack;
    private State state = State.STOPPED;

    /** We currently hold audio focus. */
    private boolean focusGranted = false;
    /** A focus request is outstanding (AUDIOFOCUS_REQUEST_DELAYED). */
    private boolean focusPending = false;
    /** Transient loss: hold position silently until AUDIOFOCUS_GAIN. */
    private boolean focusSuspended = false;
    private boolean ducked = false;
    private AudioFocusRequest focusRequest; // API 26+

    private final AudioManager.OnAudioFocusChangeListener focusListener = this::onFocusChange;

    public AndroidSoundtrackEngine(Context context) {
        this.appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
    }

    // ── Contract ─────────────────────────────────────────────────────────

    @Override
    public void play(Track track) {
        if (track == currentTrack && state == State.PLAYING) return;
        releasePlayer();
        playerErrors = 0;
        pausedByNoisyDevice = false;
        currentTrack = track;
        state = State.PLAYING;
        updateOutput();
    }

    @Override
    public void pause() {
        if (state != State.PLAYING) return;
        state = State.PAUSED;
        updateOutput();
    }

    @Override
    public void resume() {
        pausedByNoisyDevice = false;
        if (state != State.PAUSED) return;
        state = State.PLAYING;
        updateOutput();
    }

    @Override
    public void stop() {
        releasePlayer();
        currentTrack = null;
        state = State.STOPPED;
        updateOutput();
    }

    @Override
    public void setVolume(float v) {
        volume = simulation.AudioLevels.clamp(v);
        applyVolume();
    }

    @Override public float getVolume() { return volume; }

    @Override
    public void setMuted(boolean m) {
        boolean wasMuted = muted;
        muted = m;
        if (!m && pausedByNoisyDevice && state == State.PAUSED) {
            // "Music on" in settings after unplugging headphones: play again.
            state = State.PLAYING;
        }
        if (wasMuted != m || state == State.PLAYING) updateOutput();
    }

    @Override public boolean isMuted() { return muted; }
    @Override public Track getCurrentTrack() { return currentTrack; }
    @Override public State getState() { return state; }

    @Override
    public float getAmplitude() {
        MediaPlayer p = player;
        try {
            return p != null && p.isPlaying() ? 0.5f : 0f;
        } catch (IllegalStateException e) {
            return 0f;
        }
    }

    @Override
    public void dispose() {
        releasePlayer();
        currentTrack = null;
        state = State.STOPPED;
        abandonFocus();
    }

    /**
     * Headphones unplugged (AUDIO_BECOMING_NOISY): silence the speaker. It stays
     * paused (Android convention) until the app returns to the foreground or the
     * player turns music on again in settings.
     */
    public void pauseForNoisyDevice() {
        if (state == State.PLAYING) {
            pausedByNoisyDevice = true;
            pause();
        }
    }

    /** Paused because headphones were unplugged; switching music back on resumes it. */
    private boolean pausedByNoisyDevice = false;

    // ── Output: reconcile player + focus with the logical state ─────────

    /** True when the current state wants sound out of the speaker. */
    private boolean wantsSound() {
        return state == State.PLAYING && !muted && currentTrack != null
                && resIdFor(currentTrack) != 0;
    }

    private void updateOutput() {
        if (!wantsSound()) {
            pausePlayer();
            if (!focusSuspended) {
                // Not audible by choice (muted / paused / stopped): give focus
                // back so other apps' audio isn't held off by a silent game.
                abandonFocus();
            }
            return;
        }
        if (!focusGranted) {
            if (focusPending) return; // AUDIOFOCUS_GAIN will call back
            if (!requestFocus()) return; // refused/delayed: stay silent
        }
        if (focusSuspended) return;
        if (player == null && !preparePlayer(currentTrack)) return;
        applyVolume();
        try {
            if (!player.isPlaying()) player.start();
        } catch (IllegalStateException e) {
            // Player errored while idle — rebuild once and retry.
            PlatformLog.w(TAG, "start hit a dead player; rebuilding", e);
            releasePlayer();
            if (preparePlayer(currentTrack)) {
                try {
                    player.start();
                } catch (IllegalStateException e2) {
                    PlatformLog.w(TAG, "rebuilt player failed to start", e2);
                    releasePlayer();
                }
            }
        }
    }

    private void onFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_GAIN:
                focusGranted = true;
                focusPending = false;
                focusSuspended = false;
                ducked = false;
                updateOutput();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                ducked = true;
                applyVolume();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                focusSuspended = true;
                pausePlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Permanent: the system will not call back. Release our
                // request; the next resume()/unmute re-requests it.
                focusSuspended = false;
                ducked = false;
                pausePlayer();
                abandonFocus();
                break;
            default:
                break;
        }
    }

    // ── MediaPlayer ─────────────────────────────────────────────────────

    /** Creates + configures the MediaPlayer. False on failure. */
    private boolean preparePlayer(Track track) {
        int resId = resIdFor(track);
        if (resId == 0) return false;
        MediaPlayer p = MediaPlayer.create(appContext, resId);
        if (p == null) {
            PlatformLog.w(TAG, "MediaPlayer.create failed for track " + track);
            return false;
        }
        p.setLooping(true);
        p.setOnErrorListener((mp, what, extra) -> {
            // Mid-play codec/media-server error: drop the player; the next
            // updateOutput() (resume / unmute / focus gain) rebuilds it.
            PlatformLog.w(TAG, "MediaPlayer error what=" + what + " extra=" + extra);
            if (player == mp) {
                releasePlayer();
                onPlayerLost();
            }
            return true; // handled — no default error dialog
        });
        p.setOnCompletionListener(mp -> {
            // Looping players should never complete; some OEM builds drop the
            // loop flag — restart rather than sit in a dead state.
            if (player == mp && wantsSound()) {
                try {
                    mp.seekTo(0);
                    mp.start();
                    return;
                } catch (IllegalStateException e) {
                    PlatformLog.w(TAG, "completion restart failed", e);
                }
            }
            if (player == mp) {
                releasePlayer();
                onPlayerLost();
            }
        });
        player = p;
        applyVolume();
        return true;
    }

    /** Media errors since the last play(): one rebuild, then stay silent. */
    private int playerErrors = 0;

    /**
     * The player died while music should be playing. Rebuild it once; if that
     * fails too, give audio focus back, since holding focus for a silent game
     * kept other apps' audio paused.
     */
    private void onPlayerLost() {
        if (!wantsSound()) return;
        if (++playerErrors <= 1) {
            updateOutput();
        } else {
            abandonFocus();
        }
    }

    private void pausePlayer() {
        MediaPlayer p = player;
        if (p == null) return;
        try {
            if (p.isPlaying()) p.pause();
        } catch (IllegalStateException ignored) {
            // Raced a media error — the error listener cleans up.
        }
    }

    private void applyVolume() {
        MediaPlayer p = player;
        if (p == null) return;
        Track t = currentTrack;
        float v = simulation.AudioLevels.amplitude(volume)
                * (t != null ? t.getGainTrim() : 1f)
                * (ducked ? DUCK_FACTOR : 1f);
        try {
            p.setVolume(v, v);
        } catch (IllegalStateException ignored) {
            // Player already released.
        }
    }

    private void releasePlayer() {
        MediaPlayer p = player;
        player = null;
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

    // ── Audio focus ─────────────────────────────────────────────────────

    /** True when focus is granted now; a delayed grant arrives via the listener. */
    private boolean requestFocus() {
        if (audioManager == null) {
            focusGranted = true; // no audio service (rare) — play anyway
            return true;
        }
        int result;
        if (Build.VERSION.SDK_INT >= 26) {
            if (focusRequest == null) {
                focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAcceptsDelayedFocusGain(true)
                        .setWillPauseWhenDucked(false)
                        .setOnAudioFocusChangeListener(focusListener)
                        .build();
            }
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            //noinspection deprecation
            result = audioManager.requestAudioFocus(focusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        focusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        focusPending = Build.VERSION.SDK_INT >= 26
                && result == AudioManager.AUDIOFOCUS_REQUEST_DELAYED;
        focusSuspended = false;
        ducked = false;
        return focusGranted;
    }

    private void abandonFocus() {
        boolean held = focusGranted || focusPending;
        focusGranted = false;
        focusPending = false;
        focusSuspended = false;
        ducked = false;
        if (!held || audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                //noinspection deprecation
                audioManager.abandonAudioFocus(focusListener);
            }
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
