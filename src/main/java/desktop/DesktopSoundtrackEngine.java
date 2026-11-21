package desktop;

import simulation.AudioLevels;
import simulation.PlatformLog;
import simulation.SoundtrackEngine;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Desktop soundtrack engine. Streams the bundled music — the MP3 theme via
 * mp3spi and the public-domain march OGGs via vorbisspi — through a
 * {@link SourceDataLine}, decoding on the fly and looping by reopening the
 * stream at end of file.
 *
 * <p>Each {@link #play} starts a new {@link Session} with its own daemon
 * thread; the previous session is cancelled first (its line is stopped and
 * flushed from the caller's thread, so the cut is immediate). Only the
 * session's own thread ever writes to its line, and a cancelled session can
 * never be revived, so two tracks can't end up playing at once.
 *
 * <p>Memory: only the compressed files (≈2–10 MB each) are cached; nothing
 * is fully decoded (a 3.5-minute march is ~38 MB of PCM).
 *
 * <p>Gain (slider curve × per-track loudness trim × mute) is applied in
 * software with a short ramp, so mute/volume changes are click-free and work
 * on mixers without a MASTER_GAIN control; the same pass measures RMS for
 * {@link #getAmplitude()}.
 *
 * <p>If a track's file is missing or no line can be opened the engine stays
 * silent; {@link #getState()} keeps reporting the requested state.
 */
public class DesktopSoundtrackEngine implements SoundtrackEngine {

    private static final String TAG = "DesktopSoundtrackEngine";

    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userRoot().node("cfhc/desktop/audio");

    /** Frames per write (~93 ms at 44.1 kHz) — bounds cancel/pause latency. */
    private static final int CHUNK_FRAMES = 4096;
    /** Per-sample gain step: a full 0→1 ramp takes ~50 ms at 44.1 kHz. */
    private static final float GAIN_STEP = 1f / 2205f;

    private volatile float volume = DesktopAudioPrefs.readLevel(PREFS, "bgm_level", "bgm_volume",
            AudioLevels.DEFAULT_MUSIC_VOLUME);
    private volatile boolean muted = PREFS.getBoolean("bgm_muted", false);
    private volatile Track currentTrack;
    private volatile State state = State.STOPPED;
    private volatile float amplitude = 0f;
    private volatile boolean disposed = false;

    /** The live session (null when stopped). Guarded by {@code this}. */
    private Session session;

    /** Compressed file bytes per track (loaded lazily, kept for instant switches). */
    private final Map<Track, byte[]> compressed = new EnumMap<>(Track.class);

    // ── Public API ────────────────────────────────────────────────────────

    @Override
    public synchronized void play(Track track) {
        if (disposed || track == null) return;
        if (track == currentTrack && state == State.PLAYING) return;
        cancelSession();
        currentTrack = track;
        state = State.PLAYING;
        session = new Session(track);
        session.start();
    }

    @Override
    public synchronized void pause() {
        if (state != State.PLAYING) return;
        state = State.PAUSED;
        if (session != null) session.setPaused(true);
    }

    @Override
    public synchronized void resume() {
        if (state != State.PAUSED) return;
        state = State.PLAYING;
        if (session != null) session.setPaused(false);
    }

    @Override
    public synchronized void stop() {
        cancelSession();
        state = State.STOPPED;
    }

    @Override
    public void setVolume(float volume) {
        this.volume = AudioLevels.clamp(volume);
        PREFS.putFloat("bgm_level", this.volume);
    }

    @Override public float getVolume() { return volume; }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        PREFS.putBoolean("bgm_muted", muted);
    }

    @Override public boolean isMuted() { return muted; }
    @Override public Track getCurrentTrack() { return currentTrack; }
    @Override public State getState() { return state; }
    @Override public float getAmplitude() { return amplitude; }

    @Override
    public synchronized void dispose() {
        disposed = true;
        cancelSession();
        state = State.STOPPED;
        synchronized (compressed) {
            compressed.clear();
        }
    }

    private void cancelSession() {
        if (session != null) {
            session.cancel();
            session = null;
        }
        amplitude = 0f;
    }

    /** Target linear gain for {@code track} right now. */
    private float targetGain(Track track) {
        return muted ? 0f : AudioLevels.amplitude(volume) * track.getGainTrim();
    }

    // ── Resources ─────────────────────────────────────────────────────────

    static String soundtrackResource(Track track) {
        switch (track) {
            case DASHBOARD_ORGAN: return "assets/sounds/soundtrack/marching_band.mp3";
            case FIGHT_SONG: return "assets/sounds/soundtrack/fight_song.ogg";
            case OFFSEASON_CALM: return "assets/sounds/soundtrack/offseason_calm.ogg";
            case RECRUITING_GROOVE: return "assets/sounds/soundtrack/recruiting_groove.ogg";
            default: return null;
        }
    }

    private byte[] compressedBytes(Track track) {
        synchronized (compressed) {
            byte[] cached = compressed.get(track);
            if (cached != null) return cached;
        }
        String res = soundtrackResource(track);
        if (res == null) return null;
        byte[] bytes;
        try (InputStream in = DesktopSoundtrackEngine.class.getClassLoader().getResourceAsStream(res)) {
            if (in == null) return null;
            // IoStreams helper (not InputStream.readAllBytes) — Android lint
            // scans the shared tree and readAllBytes needs API 33 > minSdk 24.
            bytes = simulation.IoStreams.readAllBytes(in);
        } catch (Exception e) {
            return null;
        }
        synchronized (compressed) {
            if (!disposed) compressed.put(track, bytes);
        }
        return bytes;
    }

    /** Opens a fresh 16-bit little-endian PCM stream over the track. */
    static AudioInputStream openPcm(byte[] bytes) throws Exception {
        AudioInputStream raw = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
        return AudioDecoding.toPcm16(raw);
    }

    // ── Playback session ─────────────────────────────────────────────────

    /** One track's playback: owns its thread and line; cancelled for good on switch/stop. */
    private final class Session implements Runnable {
        private final Track track;
        private final Object lock = new Object();
        private volatile boolean cancelled = false;
        private boolean paused = false; // guarded by lock
        private volatile SourceDataLine line;
        private float gain = 0f; // session thread only (ramps from silence)

        Session(Track track) {
            this.track = track;
        }

        void start() {
            Thread t = new Thread(this, "cfhc-soundtrack-" + track.name().toLowerCase(java.util.Locale.ROOT));
            t.setDaemon(true);
            t.start();
        }

        void setPaused(boolean p) {
            synchronized (lock) {
                paused = p;
                lock.notifyAll();
            }
            SourceDataLine l = line;
            if (p && l != null) {
                l.stop(); // keeps buffered position; write() blocks until resumed
            }
        }

        void cancel() {
            cancelled = true;
            synchronized (lock) {
                lock.notifyAll();
            }
            SourceDataLine l = line;
            if (l != null) {
                try {
                    l.stop();
                    l.flush(); // unblocks a pending write() at once
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public void run() {
            try {
                byte[] bytes = compressedBytes(track);
                if (bytes == null) {
                    PlatformLog.w(TAG, "Soundtrack resource missing: " + track);
                    return;
                }
                while (!cancelled) {
                    try (AudioInputStream pcm = openPcm(bytes)) {
                        if (line == null && !openLine(pcm.getFormat())) {
                            return;
                        }
                        if (!pump(pcm)) {
                            return;
                        }
                    }
                    // End of file: loop by reopening the stream; the line
                    // keeps running, so the seam is just the file's own edges.
                }
            } catch (Exception e) {
                if (!cancelled) PlatformLog.w(TAG, "Soundtrack playback failed: " + track, e);
            } finally {
                SourceDataLine l = line;
                line = null;
                if (l != null) {
                    try {
                        l.stop();
                        l.flush();
                        l.close();
                    } catch (Exception ignored) {
                    }
                }
                if (!cancelled) amplitude = 0f;
            }
        }

        private boolean openLine(AudioFormat format) {
            try {
                SourceDataLine l = (SourceDataLine) AudioSystem.getLine(
                        new DataLine.Info(SourceDataLine.class, format));
                // ~280 ms buffer: deep enough not to underrun, and mute/volume
                // changes (applied as chunks are written) land within it.
                l.open(format, CHUNK_FRAMES * format.getFrameSize() * 3);
                line = l;
                if (cancelled) return false;
                synchronized (lock) {
                    if (!paused) l.start();
                }
                return true;
            } catch (Exception e) {
                PlatformLog.w(TAG, "No audio line for soundtrack: " + e.getMessage());
                return false;
            }
        }

        /** Streams one pass of the file. False once the session should end. */
        private boolean pump(AudioInputStream pcm) throws Exception {
            SourceDataLine l = line;
            int frameSize = pcm.getFormat().getFrameSize();
            byte[] buf = new byte[CHUNK_FRAMES * frameSize];
            while (!cancelled) {
                if (!waitWhilePaused(l)) return false;
                int n = readFully(pcm, buf);
                if (n <= 0) return true; // EOF → caller loops
                n -= n % frameSize;
                applyGain(buf, n);
                int off = 0;
                while (off < n) {
                    // A pause stops the line, which makes write() return early;
                    // re-check before every write so we park instead of spinning.
                    if (!waitWhilePaused(l)) return false;
                    off += Math.max(0, l.write(buf, off, n - off));
                }
            }
            return false;
        }

        private boolean waitWhilePaused(SourceDataLine l) throws InterruptedException {
            synchronized (lock) {
                if (!paused) {
                    if (!cancelled && !l.isRunning()) l.start();
                    return !cancelled;
                }
                l.stop(); // in case we restarted it just before setPaused(true)
                amplitude = 0f;
                while (paused && !cancelled) {
                    lock.wait();
                }
                if (cancelled) return false;
                gain = 0f; // ramp back in — no click on resume
                l.start();
                return true;
            }
        }

        /** Software gain with a per-sample ramp; also updates the RMS meter. */
        private void applyGain(byte[] buf, int len) {
            float target = targetGain(track);
            double sumSq = 0;
            int samples = len / 2;
            for (int i = 0; i + 1 < len; i += 2) {
                if (gain < target) gain = Math.min(target, gain + GAIN_STEP);
                else if (gain > target) gain = Math.max(target, gain - GAIN_STEP);
                int s = (short) ((buf[i] & 0xff) | (buf[i + 1] << 8));
                int v = Math.round(s * gain);
                if (v > Short.MAX_VALUE) v = Short.MAX_VALUE;
                else if (v < Short.MIN_VALUE) v = Short.MIN_VALUE;
                buf[i] = (byte) v;
                buf[i + 1] = (byte) (v >> 8);
                sumSq += (double) v * v;
            }
            if (!cancelled && samples > 0) {
                double rms = Math.sqrt(sumSq / samples) / 32768.0;
                // ×3 so typical program material (~-20 dBFS RMS) reads mid-scale.
                amplitude = (float) Math.min(1.0, rms * 3.0);
            }
        }
    }

    private static int readFully(InputStream in, byte[] buf) throws java.io.IOException {
        int total = 0;
        int idle = 0;
        while (total < buf.length) {
            int r = in.read(buf, total, buf.length - total);
            if (r < 0) break;
            // The vorbis SPI returns 0-byte reads between pages; tolerate
            // them, but a decoder stuck at 0 is treated as end of file.
            if (r == 0 && ++idle > 10_000) break;
            total += r;
        }
        return total == 0 ? -1 : total;
    }
}
