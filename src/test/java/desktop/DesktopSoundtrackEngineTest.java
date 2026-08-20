package desktop;

import org.junit.Test;
import simulation.SoundtrackEngine;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Soundtrack engine wiring: the four public-domain march OGGs must be on
 * the classpath (prepareDesktopResources copies them), and the engine must
 * degrade gracefully when audio lines are unavailable (headless CI).
 */
public class DesktopSoundtrackEngineTest {

    private static final String[] OGG_RESOURCES = {
            "assets/sounds/soundtrack/fight_song.ogg",
            "assets/sounds/soundtrack/dashboard_organ.ogg",
            "assets/sounds/soundtrack/offseason_calm.ogg",
            "assets/sounds/soundtrack/recruiting_groove.ogg",
    };

    @Test
    public void marchOggResources_areOnClasspathWithOggSMagic() throws Exception {
        for (String res : OGG_RESOURCES) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(res)) {
                assertNotNull("missing soundtrack resource: " + res, in);
                byte[] head = new byte[4];
                assertEquals(4, in.read(head));
                assertEquals("OggS magic for " + res, "OggS", new String(head, "UTF-8"));
            }
        }
    }

    @Test
    public void engine_neverThrowsHeadless_andReportsTrack() {
        SoundtrackEngine engine = new DesktopSoundtrackEngine();
        try {
            engine.play(SoundtrackEngine.Track.FIGHT_SONG);
            // On machines with audio the OGG loop starts; on headless CI the
            // engine silently no-ops (falls back to synth, which also no-ops).
            // Either way the API contract holds:
            assertEquals(SoundtrackEngine.Track.FIGHT_SONG, engine.getCurrentTrack());
            engine.pause();
            engine.resume();
            engine.setVolume(0.5f);
            engine.setMuted(true);
            assertTrue(engine.isMuted());
            engine.setMuted(false);
            engine.stop();
            assertEquals(SoundtrackEngine.State.STOPPED, engine.getState());
        } finally {
            engine.dispose();
        }
    }

    @Test
    public void trackDisplayNames_areMarchTitles() {
        assertTrue(SoundtrackEngine.Track.FIGHT_SONG.getDisplayName()
                .contains("Stars and Stripes"));
        assertTrue(SoundtrackEngine.Track.DASHBOARD_ORGAN.getDisplayName()
                .contains("Washington Post"));
        assertTrue(SoundtrackEngine.Track.OFFSEASON_CALM.getDisplayName()
                .contains("National Emblem"));
        assertTrue(SoundtrackEngine.Track.RECRUITING_GROOVE.getDisplayName()
                .contains("Semper Fidelis"));
    }
}
