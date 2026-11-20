package desktop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import simulation.AudioEvent;
import simulation.AudioLevels;
import simulation.AudioManager;
import simulation.PlatformLog;

public class DesktopAudioManager implements AudioManager {

    private static final String TAG = "DesktopAudioManager";

    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userRoot().node("cfhc/desktop/audio");

    /** Overlapping cues beyond this cut the oldest (UI mashing stays tidy). */
    static final int MAX_ACTIVE_CLIPS = 6;

    /** Decoded PCM per event (SFX are short WAVs — decoding once is cheap). */
    private final Map<AudioEvent, Pcm> audioCache = new EnumMap<>(AudioEvent.class);
    private final Deque<Clip> activeClips = new ArrayDeque<>();
    private final java.util.Set<AudioEvent> warnedMissing =
            java.util.Collections.synchronizedSet(EnumSet.noneOf(AudioEvent.class));
    private volatile float volume = DesktopAudioPrefs.readLevel(PREFS, "sfx_level", "sfx_volume",
            AudioLevels.DEFAULT_SFX_VOLUME);
    private volatile boolean muted = PREFS.getBoolean("sfx_muted", false);
    private volatile boolean available = false;
    private volatile boolean disposed = false;

    /** Opening a Clip can take tens of ms on some mixers — never on the EDT. */
    private final ExecutorService player = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cfhc-sfx");
        t.setDaemon(true);
        return t;
    });

    private static final class Pcm {
        final byte[] bytes;
        final AudioFormat format;
        Pcm(byte[] bytes, AudioFormat format) { this.bytes = bytes; this.format = format; }
    }

    public DesktopAudioManager() {
        preloadSounds();
    }

    private void preloadSounds() {
        for (AudioEvent event : AudioEvent.values()) {
            String base = fileBaseFor(event);
            // Prefer the generated WAV SFX (clean, well-levelled, no SPI
            // needed); fall back to an OGG if the WAV is absent.
            byte[] data = readSoundResource(base + ".wav");
            if (data == null) {
                data = readSoundResource(base + ".ogg");
            }
            if (data == null) continue;
            Pcm pcm = decode(event, data);
            if (pcm != null) audioCache.put(event, pcm);
        }
        available = !audioCache.isEmpty();
        if (!available) {
            PlatformLog.w(TAG, "No sound files loaded — audio is disabled");
        }
    }

    /** Decodes one cue; a bad file only loses that cue, never the whole session. */
    private static Pcm decode(AudioEvent event, byte[] data) {
        try (AudioInputStream ais = AudioDecoding.toPcm(
                AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)))) {
            return new Pcm(readAllBytes(ais), ais.getFormat());
        } catch (UnsupportedAudioFileException e) {
            PlatformLog.e(TAG, "Unsupported audio format for: " + event.name()
                    + " (is vorbisspi/jorbis/tritonus on the classpath?)", e);
        } catch (Exception e) {
            PlatformLog.e(TAG, "Failed to decode sound: " + event.name(), e);
        }
        return null;
    }

    private static byte[] readSoundResource(String fileName) {
        String resourcePath = "assets/sounds/" + fileName;
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            return is == null ? null : readAllBytes(is);
        } catch (Exception e) {
            PlatformLog.w(TAG, "Failed to load sound: " + fileName + " — " + e.getMessage());
            return null;
        }
    }

    /** Whether any sound assets were loaded. */
    boolean isAvailable() {
        return available;
    }

    /** Whether {@code event} has a decoded sound. */
    boolean hasSound(AudioEvent event) {
        return audioCache.containsKey(event);
    }

    static String fileNameFor(AudioEvent event) {
        return fileBaseFor(event) + ".ogg";
    }

    /** Resource base name (no extension) for an event's sound file. */
    static String fileBaseFor(AudioEvent event) {
        return switch (event) {
            case UI_CLICK -> "click";
            case UI_BACK -> "ui_back";
            case UI_TOGGLE -> "ui_toggle";
            case PLAY_SELECT -> "play";
            case FIRST_DOWN -> "firstdown";
            case FIGHT_SONG -> "fightsong";
            case TOUCHDOWN_CHEER -> "touchdown";
            case CROWD_ROAR -> "crowd_roar";
            case STADIUM_ORGAN -> "organ";
            default -> event.name().toLowerCase();
        };
    }

    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    @Override
    public void play(AudioEvent event) {
        if (muted || !available || disposed || event == null) return;
        Pcm pcm = audioCache.get(event);
        if (pcm == null) {
            // Warn once per event — a cue with no asset is log noise per week.
            if (warnedMissing.add(event)) {
                PlatformLog.w(TAG, "No audio data for event: " + event.name());
            }
            return;
        }
        try {
            player.execute(() -> playNow(pcm));
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // Disposed concurrently.
        }
    }

    private void playNow(Pcm pcm) {
        if (disposed || muted) return;
        Clip clip = null;
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(pcm.bytes),
                pcm.format, pcm.bytes.length / pcm.format.getFrameSize())) {
            clip = AudioSystem.getClip();
            clip.open(ais);
            applyVolume(clip);
            final Clip toClose = clip;
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    try { toClose.close(); } catch (Exception ignored) {}
                    synchronized (activeClips) {
                        activeClips.remove(toClose);
                    }
                }
            });
            Clip evicted = null;
            synchronized (activeClips) {
                if (activeClips.size() >= MAX_ACTIVE_CLIPS) {
                    evicted = activeClips.pollFirst();
                }
                activeClips.addLast(clip);
            }
            if (evicted != null) {
                try { evicted.stop(); evicted.close(); } catch (Exception ignored) {}
            }
            clip.start();
            clip = null; // owned by the listener now
        } catch (LineUnavailableException clipEx) {
            // No Clip available: stream it (we are already off the EDT).
            playViaSourceDataLine(pcm);
        } catch (Exception e) {
            // Transient mixer/line issues must not permanently mute the session.
            PlatformLog.e(TAG, "Failed to play sound", e);
        } finally {
            if (clip != null) {
                try { clip.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void playViaSourceDataLine(Pcm pcm) {
        try {
            AudioFormat format = pcm.format;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                PlatformLog.w(TAG, "SourceDataLine not supported for format: " + format);
                return;
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            setGain(line);
            line.start();
            line.write(pcm.bytes, 0, pcm.bytes.length - pcm.bytes.length % format.getFrameSize());
            line.drain();
            line.stop();
            line.close();
        } catch (Exception e) {
            PlatformLog.e(TAG, "SourceDataLine fallback also failed", e);
        }
    }

    private void applyVolume(Clip clip) {
        setGain(clip);
    }

    private void setGain(javax.sound.sampled.Line line) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = AudioLevels.decibels(volume);
            dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
            gain.setValue(dB);
        }
    }

    @Override
    public void setVolume(float vol) {
        this.volume = AudioLevels.clamp(vol);
        PREFS.putFloat("sfx_level", this.volume);
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        PREFS.putBoolean("sfx_muted", muted);
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void dispose() {
        disposed = true;
        player.shutdownNow();
        audioCache.clear();
        synchronized (activeClips) {
            for (Clip c : activeClips) {
                try {
                    if (c.isRunning()) c.stop();
                    c.close();
                } catch (Exception ignored) {}
            }
            activeClips.clear();
        }
    }
}
