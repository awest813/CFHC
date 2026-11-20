package antdroid.cfbcoach;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import simulation.AudioEvent;
import simulation.AudioLevels;
import simulation.AudioManager;

/**
 * One-shot SFX via {@link SoundPool}. Samples decode asynchronously after
 * construction; a cue requested before its sample finishes loading is played
 * as soon as it is ready (if that is still within {@link #LATE_PLAY_WINDOW_MS}),
 * so the first taps after launch are not silently dropped.
 */
public class AndroidAudioManager implements AudioManager {

    /** A cue that arrives later than this after the tap would feel detached. */
    private static final long LATE_PLAY_WINDOW_MS = 400;

    private SoundPool soundPool;
    private final Map<AudioEvent, Integer> soundIds = new EnumMap<>(AudioEvent.class);
    private final Set<Integer> loadedSamples = new HashSet<>();
    private int pendingSample = 0;
    private long pendingAtMs = 0;
    private float volume = AudioLevels.DEFAULT_SFX_VOLUME;
    private volatile boolean muted = false;

    public AndroidAudioManager(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attrs)
                .build();
        // Callbacks arrive on the thread that created the pool (main looper).
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status != 0) return;
            loadedSamples.add(sampleId);
            if (sampleId == pendingSample) {
                pendingSample = 0;
                if (SystemClock.uptimeMillis() - pendingAtMs <= LATE_PLAY_WINDOW_MS) {
                    playSample(sampleId);
                }
            }
        });

        loadSounds(context);
    }

    private void loadSounds(Context context) {
        soundIds.put(AudioEvent.UI_CLICK, soundPool.load(context, R.raw.click, 1));
        soundIds.put(AudioEvent.UI_BACK, soundPool.load(context, R.raw.ui_back, 1));
        soundIds.put(AudioEvent.UI_TOGGLE, soundPool.load(context, R.raw.ui_toggle, 1));
        soundIds.put(AudioEvent.CONFIRM, soundPool.load(context, R.raw.confirm, 1));
        soundIds.put(AudioEvent.ERROR, soundPool.load(context, R.raw.error, 1));
        soundIds.put(AudioEvent.WHISTLE, soundPool.load(context, R.raw.whistle, 1));
        soundIds.put(AudioEvent.PLAY_SELECT, soundPool.load(context, R.raw.play, 1));
        soundIds.put(AudioEvent.FIRST_DOWN, soundPool.load(context, R.raw.firstdown, 1));
        soundIds.put(AudioEvent.ADVANCE, soundPool.load(context, R.raw.advance, 1));
        soundIds.put(AudioEvent.WIN, soundPool.load(context, R.raw.win, 1));
        soundIds.put(AudioEvent.LOSS, soundPool.load(context, R.raw.loss, 1));
        // Atmosphere stingers (championship week, upsets, title wins —
        // chosen by SeasonController).
        soundIds.put(AudioEvent.FIGHT_SONG, soundPool.load(context, R.raw.fightsong, 1));
        soundIds.put(AudioEvent.TOUCHDOWN_CHEER, soundPool.load(context, R.raw.touchdown, 1));
        soundIds.put(AudioEvent.CROWD_ROAR, soundPool.load(context, R.raw.crowd_roar, 1));
        soundIds.put(AudioEvent.STADIUM_ORGAN, soundPool.load(context, R.raw.organ, 1));
    }

    @Override
    public void play(AudioEvent event) {
        if (muted || soundPool == null || event == null) return;
        Integer id = soundIds.get(event);
        if (id == null || id == 0) return;
        if (!loadedSamples.contains(id)) {
            // Still decoding: play it from the load callback instead.
            pendingSample = id;
            pendingAtMs = SystemClock.uptimeMillis();
            return;
        }
        playSample(id);
    }

    private void playSample(int id) {
        if (muted || soundPool == null) return;
        float v = AudioLevels.amplitude(volume);
        soundPool.play(id, v, v, 1, 0, 1f);
    }

    @Override
    public void setVolume(float vol) {
        this.volume = AudioLevels.clamp(vol);
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) pendingSample = 0;
    }

    @Override
    public boolean isMuted() {
        return muted;
    }

    @Override
    public void dispose() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundIds.clear();
        loadedSamples.clear();
        pendingSample = 0;
    }
}
