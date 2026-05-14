package desktop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;

import simulation.AudioEvent;
import simulation.AudioManager;
import simulation.PlatformLog;

public class DesktopAudioManager implements AudioManager {

    private static final String TAG = "DesktopAudioManager";

    private final Map<AudioEvent, byte[]> audioCache = new HashMap<>();
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
            clip = AudioSystem.getClip();
            clip.open(ais);

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = volume > 0 ? (float) (Math.log10(volume) * 20.0) : -80f;
                dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
                gain.setValue(dB);
            }

            final Clip toClose = clip;
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    try { toClose.close(); } catch (Exception ignored) {}
                }
            });
            clip.start();
            started = true;
        } catch (LineUnavailableException e) {
            PlatformLog.w(TAG, "Audio line unavailable for event: " + event.name() + " — " + e.getMessage());
        } catch (Exception e) {
            PlatformLog.e(TAG, "Failed to play sound: " + event.name(), e);
        } finally {
            if (ais != null) {
                try {
                    ais.close();
                } catch (IOException ignored) {
                }
            }
            if (clip != null && !started) {
                clip.close();
            }
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
    }
}
