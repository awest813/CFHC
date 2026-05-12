package antdroid.cfbcoach;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import java.util.HashMap;
import java.util.Map;

import simulation.AudioEvent;
import simulation.AudioManager;

public class AndroidAudioManager implements AudioManager {

    private SoundPool soundPool;
    private final Map<AudioEvent, Integer> soundIds = new HashMap<>();
    private float volume = 0.7f;
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

        loadSounds(context);
    }

    private void loadSounds(Context context) {
        soundIds.put(AudioEvent.UI_CLICK, soundPool.load(context, R.raw.click, 1));
        soundIds.put(AudioEvent.CONFIRM, soundPool.load(context, R.raw.confirm, 1));
        soundIds.put(AudioEvent.ERROR, soundPool.load(context, R.raw.error, 1));
        soundIds.put(AudioEvent.WHISTLE, soundPool.load(context, R.raw.whistle, 1));
        soundIds.put(AudioEvent.PLAY_SELECT, soundPool.load(context, R.raw.play, 1));
        soundIds.put(AudioEvent.FIRST_DOWN, soundPool.load(context, R.raw.firstdown, 1));
        soundIds.put(AudioEvent.ADVANCE, soundPool.load(context, R.raw.advance, 1));
        soundIds.put(AudioEvent.WIN, soundPool.load(context, R.raw.win, 1));
        soundIds.put(AudioEvent.LOSS, soundPool.load(context, R.raw.loss, 1));
    }

    @Override
    public void play(AudioEvent event) {
        if (muted || soundPool == null) return;
        Integer id = soundIds.get(event);
        if (id != null && id != 0) {
            soundPool.play(id, volume, volume, 1, 0, 1f);
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
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        soundIds.clear();
    }
}
