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
 * <p>The activity owns the lifecycle: created in onCreate, paused/resumed
 * with the activity, released in onDestroy. The engine manages its own
 * audio focus (BGM ducks out for phone calls and other apps' audio) —
 * {@link MainActivity} should also register the becoming-noisy receiver via
 * {@link #pauseForNoisyDevice()} / {@link #setNoisyReceiverActive(boolean)}
 * around onResume/onPause so unplugging headphones silences the speaker.
 */
public class AndroidSoundtrackEngine implements SoundtrackEngine {

    private static final String TAG = "AndroidSoundtrackEngine";

    private final Context appContext;
    private final AudioManager audioManager;
    private MediaPlayer player;
    private volatile boolean paused = false;
    private volatile float volume = 0.4f;
    private volatile boolean muted = false;
    private volatile Track currentTrack;
    private volatile State state = State.STOPPED;

    /** Audio-focus bookkeeping: only auto-resume what focus loss paused. */
    private volatile boolean focusGranted = false;
    private volatile boolean pausedByFocusLoss = false;
    private AudioFocusRequest focusRequest; // API 26+
    private final AudioManager.OnAudioFocusChangeListener focusListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int change) {
                    switch (change) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            if (state == State.PLAYING) {
                                pausedByFocusLoss = true;
                                pauseInternal();
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            if (pausedByFocusLoss) {
                                pausedByFocusLoss = false;
                                resumeInternal();
                            }
                            break;
                        default:
                            break;
                    }
                }
            };

    public AndroidSoundtrackEngine(Context context) {
        this.appContext = context.getApplicationContext();
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void play(Track track) {
        if (track == currentTrack && state == State.PLAYING) return;
        releasePlayer();
        currentTrack = track;
        paused = false;
        pausedByFocusLoss = false;
        if (!requestFocus()) {
            // Focus refused (phone call in progress): honor the contract, wait
            // for AUDIOFOCUS_GAIN to actually start.
            state = State.PAUSED;
            pausedByFocusLoss = true;
            preparePlayerFor(track);
            return;
        }
        startPlayback(track);
    }

    private void startPlayback(Track track) {
        int resId = resIdFor(track);
        if (resId == 0) {
            // Track not bundled on Android (APK size) — stay silent but keep
            // the state contract so screen code can switch tracks freely.
            state = State.PLAYING;
            return;
        }
        if (!preparePlayerFor(track) || player == null) {
            return;
        }
        player.start();
        state = State.PLAYING;
    }

    /** Creates + configures the MediaPlayer. False (state=STOPPED) on failure. */
    private boolean preparePlayerFor(Track track) {
        int resId = resIdFor(track);
        if (resId == 0) {
            return true; // contract-only track handled by the caller
        }
        player = MediaPlayer.create(appContext, resId);
        if (player == null) {
            PlatformLog.w(TAG, "MediaPlayer.create failed for track " + track);
            state = State.STOPPED;
            return false;
        }
        player.setLooping(true);
        player.setOnErrorListener((mp, what, extra) -> {
            // Mid-play codec/media-server error: without this the player sits
            // in Error state while `state` claims PLAYING, and the next
            // resume() would throw IllegalStateException.
            PlatformLog.w(TAG, "MediaPlayer error what=" + what + " extra=" + extra);
            releasePlayer();
            state = State.STOPPED;
            return true; // handled — do not invoke the default error dialog
        });
        player.setOnCompletionListener(mp -> {
            // Looping players should never complete; if they do (looping lost
            // on some OEM builds), restart rather than sit in a dead state.
            if (state == State.PLAYING && currentTrack != null) {
                try {
                    mp.seekTo(0);
                    mp.start();
                    return;
                } catch (IllegalStateException e) {
                    PlatformLog.w(TAG, "completion restart failed", e);
                }
            }
            releasePlayer();
            state = State.STOPPED;
        });
        applyVolume();
        return true;
    }

    @Override
    public void pause() {
        pausedByFocusLoss = false; // explicit pause: focus gain must not resume
        pauseInternal();
    }

    private void pauseInternal() {
        if (state != State.PLAYING) {
            return;
        }
        paused = true;
        state = State.PAUSED;
        MediaPlayer p = player;
        if (p == null) return;
        try {
            if (p.isPlaying()) p.pause();
        } catch (IllegalStateException ignored) {
            // Raced a media error — the error listener already cleaned up.
        }
    }

    @Override
    public void resume() {
        if (!focusGranted) {
            // Focus is held elsewhere (call in progress): stay paused; the
            // focus listener resumes when focus returns.
            return;
        }
        resumeInternal();
    }

    private void resumeInternal() {
        if (state != State.PAUSED || player == null) {
            return;
        }
        try {
            player.start();
            paused = false;
            state = State.PLAYING;
        } catch (IllegalStateException e) {
            // Player errored while paused — rebuild and restart the track.
            PlatformLog.w(TAG, "resume hit a dead player; restarting track", e);
            Track track = currentTrack;
            releasePlayer();
            if (track != null) {
                state = State.PLAYING;
                if (!preparePlayerFor(track)) {
                    return;
                }
                player.start();
            }
        }
    }

    @Override
    public void stop() {
        abandonFocus();
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
        abandonFocus();
        releasePlayer();
        currentTrack = null;
        state = State.STOPPED;
    }

    /**
     * Headphones unplugged (AUDIO_BECOMING_NOISY): silence the speaker.
     * Called from MainActivity's receiver around onResume/onPause.
     */
    public void pauseForNoisyDevice() {
        pausedByFocusLoss = false;
        pauseInternal();
    }

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
                        .setOnAudioFocusChangeListener(focusListener)
                        .build();
            }
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            // Legacy pre-O path (deprecated on O+, functional everywhere).
            //noinspection deprecation
            result = audioManager.requestAudioFocus(focusListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        focusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return focusGranted;
    }

    private void abandonFocus() {
        focusGranted = false;
        pausedByFocusLoss = false;
        if (audioManager == null) return;
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
