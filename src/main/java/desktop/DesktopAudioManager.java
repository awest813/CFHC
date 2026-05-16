package desktop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import simulation.AudioEvent;
import simulation.AudioManager;
import simulation.PlatformLog;

public class DesktopAudioManager implements AudioManager {

    private static final String TAG = "DesktopAudioManager";

    private final Map<AudioEvent, byte[]> audioCache = new HashMap<>();
    private final List<Clip> activeClips = new ArrayList<>();
    private float volume = 0.7f;
    private volatile boolean muted = false;
    private volatile boolean available = false;

    public DesktopAudioManager() {
        preloadSounds();
    }

    private void preloadSounds() {
        int loaded = 0;
        for (AudioEvent event : AudioEvent.values()) {
            String fileName = fileNameFor(event);
            String resourcePath = "assets/sounds/" + fileName;
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(resourcePath)) {
                if (is == null) {
                    continue;
                }
                audioCache.put(event, readAllBytes(is));
                loaded++;
            } catch (Exception e) {
                PlatformLog.w(TAG, "Failed to load sound: " + fileName + " — " + e.getMessage());
            }
        }
        available = loaded > 0;
        if (!available) {
            PlatformLog.w(TAG, "No sound files loaded — audio is disabled");
        }
    }

    private static String fileNameFor(AudioEvent event) {
        return switch (event) {
            case UI_CLICK -> "click.ogg";
            case PLAY_SELECT -> "play.ogg";
            case FIRST_DOWN -> "firstdown.ogg";
            default -> event.name().toLowerCase() + ".ogg";
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
        if (muted || !available) return;
        byte[] data = audioCache.get(event);
        if (data == null) {
            PlatformLog.w(TAG, "No audio data for event: " + event.name());
            return;
        }

        AudioInputStream ais = null;
        Clip clip = null;
        boolean started = false;
        try {
            ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
            AudioFormat format = ais.getFormat();
            try {
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
                clip.start();
                started = true;
                synchronized (activeClips) {
                    activeClips.add(clip);
                }
            } catch (LineUnavailableException clipEx) {
                if (clip != null) clip.close();
                playViaSourceDataLine(data, format);
            }
        } catch (Exception e) {
            PlatformLog.e(TAG, "Failed to play sound: " + event.name(), e);
        } finally {
            if (ais != null) {
                try { ais.close(); } catch (IOException ignored) {}
            }
            if (clip != null && !started) {
                clip.close();
            }
        }
    }

    private void playViaSourceDataLine(byte[] data, AudioFormat format) {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))) {
            AudioFormat fmt = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                PlatformLog.w(TAG, "SourceDataLine not supported for format: " + format);
                return;
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = volume > 0 ? (float) (Math.log10(volume) * 20.0) : -80f;
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }
            line.start();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = ais.read(buffer)) != -1) {
                line.write(buffer, 0, bytesRead);
            }
            line.drain();
            line.stop();
            line.close();
        } catch (Exception e) {
            PlatformLog.e(TAG, "SourceDataLine fallback also failed", e);
        }
    }

    private void applyVolume(Clip clip) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = volume > 0 ? (float) (Math.log10(volume) * 20.0) : -80f;
            dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
            gain.setValue(dB);
        }
    }

    @Override
    public void setVolume(float vol) {
        this.volume = Math.max(0, Math.min(1, vol));
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void dispose() {
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
