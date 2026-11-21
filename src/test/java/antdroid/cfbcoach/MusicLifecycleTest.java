package antdroid.cfbcoach;

import android.app.Activity;
import android.os.Bundle;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import simulation.SoundtrackEngine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Background music follows the app, not one screen: another of our activities
 * on top (Recruiting) keeps it playing; leaving the app pauses it.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class MusicLifecycleTest {

    @Test
    public void musicKeepsPlayingUnderAnotherAppScreen_andPausesInBackground() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(
                LeagueFixture.mainIntent(ApplicationProvider.getApplicationContext(), 0))) {
            final SoundtrackEngine[] music = new SoundtrackEngine[1];
            scenario.onActivity(a -> music[0] = a.soundtrack());
            assertNotNull(music[0]);
            assertEquals(SoundtrackEngine.State.PLAYING, music[0].getState());

            // Another of our screens starts on top, then MainActivity stops.
            ActivityController<Activity> other =
                    Robolectric.buildActivity(Activity.class).create(new Bundle()).start().resume();
            scenario.moveToState(Lifecycle.State.CREATED);
            assertEquals("in-app navigation must not pause music",
                    SoundtrackEngine.State.PLAYING, music[0].getState());

            // Leaving the app: the last started screen stops.
            other.pause().stop();
            assertTrue(CfhcApplication.from(ApplicationProvider.getApplicationContext()) != null);
            assertEquals("backgrounding the app pauses music",
                    SoundtrackEngine.State.PAUSED, music[0].getState());

            // Coming back resumes it.
            scenario.moveToState(Lifecycle.State.RESUMED);
            assertEquals(SoundtrackEngine.State.PLAYING, music[0].getState());
            other.destroy();
        }
    }
}
