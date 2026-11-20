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
    public void mp3ThemeResource_isOnClasspathWithMpegMagic() throws Exception {
        String res = "assets/sounds/soundtrack/marching_band.mp3";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(res)) {
            assertNotNull("missing soundtrack resource: " + res, in);
            byte[] head = new byte[2];
            assertEquals(2, in.read(head));
            // Raw MPEG frame sync: 0xFF followed by 3 set high bits (no ID3 tag).
            assertEquals(0xFF, head[0] & 0xFF);
            assertEquals(0xE0, head[1] & 0xE0);
        }
    }

    @Test
    public void engine_neverThrowsHeadless_andReportsTrack() {
        SoundtrackEngine engine = new DesktopSoundtrackEngine();
        try {
            engine.play(SoundtrackEngine.Track.FIGHT_SONG);
            // On machines with audio the OGG loop starts (decoded async on
            // the loader thread); on headless CI the engine stays silent.
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
    public void trackDisplayNames_areLicensedTitles() {
        assertTrue(SoundtrackEngine.Track.DASHBOARD_ORGAN.getDisplayName()
                .contains("Marching Band"));
        assertTrue(SoundtrackEngine.Track.FIGHT_SONG.getDisplayName()
                .contains("Stars and Stripes"));
        assertTrue(SoundtrackEngine.Track.OFFSEASON_CALM.getDisplayName()
                .contains("National Emblem"));
        assertTrue(SoundtrackEngine.Track.RECRUITING_GROOVE.getDisplayName()
                .contains("Semper Fidelis"));
    }

    @Test
    public void rapidSwitching_leavesAtMostOneLiveSession() throws Exception {
        DesktopSoundtrackEngine engine = new DesktopSoundtrackEngine();
        try {
            SoundtrackEngine.Track[] tracks = SoundtrackEngine.Track.values();
            for (int i = 0; i < 40; i++) {
                engine.play(tracks[i % tracks.length]);
                if (i % 7 == 0) engine.pause();
                if (i % 7 == 1) engine.resume();
            }
            engine.play(SoundtrackEngine.Track.OFFSEASON_CALM);
            assertEquals(SoundtrackEngine.Track.OFFSEASON_CALM, engine.getCurrentTrack());
            assertEquals(SoundtrackEngine.State.PLAYING, engine.getState());
            // Cancelled sessions exit promptly; only the last one may remain.
            long deadline = System.currentTimeMillis() + 5000;
            while (liveSessions() > 1 && System.currentTimeMillis() < deadline) {
                Thread.sleep(25);
            }
            assertTrue("live soundtrack threads: " + liveSessions(), liveSessions() <= 1);
            engine.stop();
            deadline = System.currentTimeMillis() + 5000;
            while (liveSessions() > 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(25);
            }
            assertEquals(0, liveSessions());
        } finally {
            engine.dispose();
        }
    }

    @Test
    public void pauseSurvivesTrackSwitchContract() {
        DesktopSoundtrackEngine engine = new DesktopSoundtrackEngine();
        try {
            engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN);
            engine.pause();
            assertEquals(SoundtrackEngine.State.PAUSED, engine.getState());
            engine.resume();
            assertEquals(SoundtrackEngine.State.PLAYING, engine.getState());
            engine.play(SoundtrackEngine.Track.DASHBOARD_ORGAN); // same track: no restart
            assertEquals(SoundtrackEngine.State.PLAYING, engine.getState());
        } finally {
            engine.dispose();
        }
    }

    @Test
    public void legacyVolumePref_migratesToSameLoudness() throws Exception {
        java.util.prefs.Preferences node = java.util.prefs.Preferences.userRoot()
                .node("cfhc/test/audio-migration-" + System.nanoTime());
        try {
            node.putFloat("old", 0.49f);
            float level = DesktopAudioPrefs.readLevel(node, "new", "old", 0.8f);
            assertEquals(0.7f, level, 1e-4f);
            assertEquals(0.7f, node.getFloat("new", -1f), 1e-4f);
            assertEquals(0.8f, DesktopAudioPrefs.readLevel(node, "absent", "absent_old", 0.8f), 0f);
        } finally {
            node.removeNode();
        }
    }

    private static int liveSessions() {
        int n = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("cfhc-soundtrack-")) n++;
        }
        return n;
    }

    @Test
    public void everyTrack_streamsAs16BitLittleEndianPcm() throws Exception {
        for (SoundtrackEngine.Track t : SoundtrackEngine.Track.values()) {
            String res = DesktopSoundtrackEngine.soundtrackResource(t);
            byte[] bytes;
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(res)) {
                assertNotNull(res, in);
                bytes = simulation.IoStreams.readAllBytes(in);
            }
            try (javax.sound.sampled.AudioInputStream pcm = DesktopSoundtrackEngine.openPcm(bytes)) {
                javax.sound.sampled.AudioFormat f = pcm.getFormat();
                assertEquals(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, f.getEncoding());
                assertEquals(16, f.getSampleSizeInBits());
                assertFalse(f.isBigEndian());
                // Stream ~1 s and make sure it is real audio, not silence.
                byte[] buf = new byte[(int) f.getFrameRate() * f.getFrameSize()];
                int total = 0, r, calls = 0;
                while (total < buf.length && calls++ < 1_000_000
                        && (r = pcm.read(buf, total, buf.length - total)) != -1) total += r;
                assertTrue(t + " produced no PCM", total > buf.length / 2);
                long peak = 0;
                for (int i = 0; i + 1 < total; i += 2) {
                    peak = Math.max(peak, Math.abs((short) ((buf[i] & 0xff) | (buf[i + 1] << 8))));
                }
                assertTrue(t + " is silent", peak > 500);
            }
        }
    }
}
