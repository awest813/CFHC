package antdroid.cfbcoach;

import android.content.Context;
import android.media.AudioManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAudioManager;

import simulation.SoundtrackEngine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.robolectric.Shadows.shadowOf;

/** Audio-focus behavior: a silent game must never take focus from other apps. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AndroidSoundtrackEngineTest {

    private Context context;
    private ShadowAudioManager shadowAudio;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        shadowAudio = shadowOf((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    }

    @Test
    public void mutedMusic_neverRequestsFocus() {
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.setMuted(true);
        engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
        engine.pause();
        engine.resume();
        assertNull(shadowAudio.getLastAudioFocusRequest());
        assertEquals(SoundtrackEngine.State.PLAYING, engine.getState());

        engine.setMuted(false);
        assertNotNull("unmuting should request focus", shadowAudio.getLastAudioFocusRequest());
        engine.dispose();
    }

    @Test
    public void trackWithoutBundledFile_neverRequestsFocus() {
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.play(SoundtrackEngine.Track.RECRUITING_GROOVE);
        assertNull(shadowAudio.getLastAudioFocusRequest());
        assertEquals(SoundtrackEngine.State.PLAYING, engine.getState());
        engine.dispose();
    }

    @Test
    public void afterPermanentLoss_resumeRequestsFocusAgain() {
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
        ShadowAudioManager.AudioFocusRequest first = shadowAudio.getLastAudioFocusRequest();
        assertNotNull(first);

        first.listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);
        engine.pause();   // activity onPause
        engine.resume();  // activity onResume

        ShadowAudioManager.AudioFocusRequest second = shadowAudio.getLastAudioFocusRequest();
        assertNotSame("resume must re-request focus after a permanent loss", first, second);
        engine.dispose();
    }

    @Test
    public void refusedFocusAtLaunch_isRetriedOnResume() {
        shadowAudio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED);
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
        ShadowAudioManager.AudioFocusRequest first = shadowAudio.getLastAudioFocusRequest();
        assertNotNull(first);

        shadowAudio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        engine.pause();
        engine.resume();
        assertNotSame(first, shadowAudio.getLastAudioFocusRequest());
        engine.dispose();
    }

    @Test
    public void pausing_releasesFocus() {
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
        ShadowAudioManager.AudioFocusRequest req = shadowAudio.getLastAudioFocusRequest();
        assertNotNull(req);
        engine.pause();
        assertNotNull("pausing should hand focus back",
                shadowAudio.getLastAbandonedAudioFocusRequest());
        engine.dispose();
    }

    @Test
    public void headphonesUnplugged_pauses_andTurningMusicBackOnResumes() {
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
        engine.pauseForNoisyDevice();
        assertEquals(SoundtrackEngine.State.PAUSED, engine.getState());
        engine.setMuted(true);   // settings: music off ...
        engine.setMuted(false);  // ... and on again
        assertEquals(SoundtrackEngine.State.PLAYING, engine.getState());
        engine.dispose();
    }

    @Test
    public void ordinaryPause_isNotResumedByTheMusicToggle() {
        AndroidSoundtrackEngine engine = new AndroidSoundtrackEngine(context);
        engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
        engine.pause(); // app in the background
        engine.setMuted(true);
        engine.setMuted(false);
        assertEquals(SoundtrackEngine.State.PAUSED, engine.getState());
        engine.dispose();
    }
}
