package desktop;

import simulation.PlatformLog;
import simulation.SoundtrackEngine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Desktop soundtrack engine. Plays the bundled music files — the MP3 theme
 * via mp3spi and the public-domain march OGGs via vorbisspi — looping on a
 * long-lived {@link Clip}. Both formats are fully decoded to PCM before the
 * Clip opens; decoding happens on a background loader thread (a ~3-minute
 * OGG is tens of MB of PCM), and a one-entry PCM cache makes repeat switches
 * instant. If a track's file is missing or the audio system can't open a
 * Clip, the engine stays silent — there is deliberately no placeholder
 * synthesis.
 *
 * <p>While a track is decoding the engine reports {@link State#PLAYING}
 * (nothing audible yet, zero amplitude); a decode failure reports
 * {@link State#STOPPED}.
 */
public class DesktopSoundtrackEngine implements SoundtrackEngine {

    private static final String TAG = "DesktopSoundtrackEngine";
    /** Nominal amplitude in clip mode (Clip exposes no meter) for visualizers. */
    private static final float CLIP_NOMINAL_AMPLITUDE = 0.55f;

    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userRoot().node("cfhc/desktop/audio");

    private volatile float volume = PREFS.getFloat("bgm_volume", 0.4f);
    private volatile boolean muted = PREFS.getBoolean("bgm_muted", false);
    private volatile Track currentTrack;
    private volatile State state = State.STOPPED;
    private volatile float amplitude = 0f;

    /** Written by the loader thread (handoff), read/stopped by the EDT. */
    private volatile Clip musicClip;
    private volatile boolean paused = false;
    private volatile boolean disposed = false;

    // ── Background decode (keeps multi-second OGG→PCM work off the EDT) ──

    private final ExecutorService loader =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "cfhc-soundtrack-loader");
                t.setDaemon(true);
                return t;
            });
    /** Bumped on every play()/stop()/dispose() — stale async loads are dropped. */
    private volatile int loadGeneration;
    /** One-entry decoded-PCM cache: track switches re-play instantly once a
     *  track has been decoded once. */
    private Track cachedPcmTrack;
    private byte[] cachedPcm;
    private AudioFormat cachedPcmFormat;

    // ── Public API ────────────────────────────────────────────────────────

    @Override
    public void play(Track track) {
        if (disposed) return;
        if (track == currentTrack && state == State.PLAYING) return;
        final int gen = ++loadGeneration;
        stopInternal();
        currentTrack = track;
        paused = false;
        if (openCachedClip(track)) {
            state = State.PLAYING;
            return;
        }
        // Nothing audible while the track decodes; the loader installs the
        // Clip when ready. PLAYING during the gap keeps the UI contract
        // stable (track switching logic keys off it).
        state = State.PLAYING;
        loader.execute(() -> {
            DecodedPcm pcm = decodeTrack(track);
            if (pcm == null) {
                // Decode failed: nothing will ever play for this request.
                synchronized (this) {
                    if (gen == loadGeneration && currentTrack == track) {
                        state = State.STOPPED;
                        PlatformLog.w(TAG, "Track decode failed: " + track);
                    }
                }
                return;
            }
            // Handoff under the engine lock, generation re-checked inside it:
            // play()/stop() on the EDT bump the generation — a stale decode
            // must never install an older track's clip over a newer request.
            synchronized (this) {
                if (gen != loadGeneration || currentTrack != track
                        || (state != State.PLAYING && state != State.PAUSED)) {
                    return;
                }
                cachedPcmTrack = track;
                cachedPcm = pcm.bytes;
                cachedPcmFormat = pcm.format;
                openPcmClip(pcm.bytes, pcm.format);
            }
        });
    }

    @Override
    public void pause() {
        if (state != State.PLAYING) {
            return;
        }
        paused = true;
        state = State.PAUSED;
        Clip c = musicClip;
        if (c != null && c.isRunning()) {
            c.stop(); // keeps position; resume() loops again
        }
    }

    @Override
    public void resume() {
        if (state != State.PAUSED) {
            return;
        }
        paused = false;
        state = State.PLAYING;
        Clip c = musicClip;
        if (c != null && c.isOpen()) {
            c.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    @Override
    public void stop() {
        loadGeneration++;
        stopInternal();
    }

    @Override
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        PREFS.putFloat("bgm_volume", this.volume);
        applyVolumeToClip();
    }

    @Override public float getVolume() { return volume; }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        PREFS.putBoolean("bgm_muted", muted);
        applyVolumeToClip();
    }

    @Override public boolean isMuted() { return muted; }
    @Override public Track getCurrentTrack() { return currentTrack; }
    @Override public State getState() { return state; }
    @Override public float getAmplitude() { return amplitude; }

    @Override
    public void dispose() {
        disposed = true;
        loadGeneration++;
        stopInternal();
        loader.shutdownNow();
    }

    // ── Bundled-file playback (MP3 theme + public-domain march OGGs) ──────

    private static String soundtrackResource(Track track) {
        switch (track) {
            case DASHBOARD_ORGAN: return "assets/sounds/soundtrack/marching_band.mp3";
            case FIGHT_SONG: return "assets/sounds/soundtrack/fight_song.ogg";
            case OFFSEASON_CALM: return "assets/sounds/soundtrack/offseason_calm.ogg";
            case RECRUITING_GROOVE: return "assets/sounds/soundtrack/recruiting_groove.ogg";
            default: return null;
        }
    }

    /** Decoded PCM payload ready to be opened into a Clip. */
    private static final class DecodedPcm {
        final byte[] bytes;
        final AudioFormat format;
        DecodedPcm(byte[] bytes, AudioFormat format) { this.bytes = bytes; this.format = format; }
    }

    /** Opens the cached PCM for {@code track} into the looping Clip. False on any failure. */
    private synchronized boolean openCachedClip(Track track) {
        if (cachedPcmTrack != track || cachedPcm == null) return false;
        return openPcmClip(cachedPcm, cachedPcmFormat);
    }

    private boolean openPcmClip(byte[] pcm, AudioFormat format) {
        try (javax.sound.sampled.AudioInputStream ais =
                     new javax.sound.sampled.AudioInputStream(
                             new ByteArrayInputStream(pcm), format, pcm.length / format.getFrameSize())) {
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            musicClip = clip;
            amplitude = CLIP_NOMINAL_AMPLITUDE;
            applyVolumeToClip();
            if (paused) return true; // paused mid-load: loaded, silent until resume()
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            return true;
        } catch (Exception e) {
            musicClip = null;
            amplitude = 0f;
            return false;
        }
    }

    /** Reads + fully decodes a bundled track to PCM. Null (no exception) on failure. */
    private static DecodedPcm decodeTrack(Track track) {
        String res = soundtrackResource(track);
        if (res == null) return null;
        byte[] bytes;
        try (InputStream in = DesktopSoundtrackEngine.class.getClassLoader()
                .getResourceAsStream(res)) {
            if (in == null) return null;
            // IoStreams helper (not InputStream.readAllBytes) — Android lint
            // scans the shared tree and readAllBytes needs API 33 > minSdk 24.
            bytes = simulation.IoStreams.readAllBytes(in);
        } catch (Exception e) {
            return null;
        }
        try (javax.sound.sampled.AudioInputStream raw = AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(bytes));
             javax.sound.sampled.AudioInputStream ais = AudioDecoding.toPcm(raw)) {
            return new DecodedPcm(simulation.IoStreams.readAllBytes(ais), ais.getFormat());
        } catch (Exception e) {
            return null;
        }
    }

    private void applyVolumeToClip() {
        Clip c = musicClip;
        if (c == null || !c.isOpen()) return;
        try {
            javax.sound.sampled.FloatControl ctrl =
                    (javax.sound.sampled.FloatControl) c.getControl(
                            javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float effectiveVol = muted ? 0f : volume;
            float dB = effectiveVol > 0 ? (float) (20 * Math.log10(effectiveVol)) : -80f;
            dB = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), dB));
            ctrl.setValue(dB);
        } catch (IllegalArgumentException ignored) {
            // No gain control on this line.
        }
    }

    private void stopInternal() {
        state = State.STOPPED;
        Clip c = musicClip;
        musicClip = null;
        if (c != null) {
            try {
                c.stop();
                c.close();
            } catch (Exception ignored) {
            }
        }
        amplitude = 0f;
    }
}
