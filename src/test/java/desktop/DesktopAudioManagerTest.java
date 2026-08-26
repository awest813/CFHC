package desktop;

import org.junit.Test;
import simulation.AudioEvent;
import simulation.IoStreams;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ServiceLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DesktopAudioManagerTest {

    @Test
    public void fileNameFor_mapsUiEvents() {
        assertEquals("click.ogg", DesktopAudioManager.fileNameFor(AudioEvent.UI_CLICK));
        assertEquals("play.ogg", DesktopAudioManager.fileNameFor(AudioEvent.PLAY_SELECT));
        assertEquals("firstdown.ogg", DesktopAudioManager.fileNameFor(AudioEvent.FIRST_DOWN));
        assertEquals("win.ogg", DesktopAudioManager.fileNameFor(AudioEvent.WIN));
    }

    @Test
    public void fileBaseFor_mapsUiEventsWithoutExtension() {
        assertEquals("click", DesktopAudioManager.fileBaseFor(AudioEvent.UI_CLICK));
        assertEquals("crowd_roar", DesktopAudioManager.fileBaseFor(AudioEvent.CROWD_ROAR));
    }

    @Test
    public void classpath_hasVorbisAudioFileReaderSpi() {
        boolean found = false;
        for (AudioFileReader reader : ServiceLoader.load(AudioFileReader.class)) {
            if (reader.getClass().getName().contains("Vorbis")) {
                found = true;
                break;
            }
        }
        assertTrue("Expected Vorbis AudioFileReader on classpath (vorbisspi+jorbis+tritonus)", found);
    }

    /**
     * Every wired SFX event must have a generated WAV on the classpath that
     * decodes to PCM with real audio — guards against the corrupt/near-silent
     * inherited OGG set regressing into the UI (the "scratching" bug).
     */
    @Test
    public void bundledSfxWavs_decodeToPcmWithAudio() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        int validated = 0;
        for (AudioEvent event : AudioEvent.values()) {
            String res = "assets/sounds/" + DesktopAudioManager.fileBaseFor(event) + ".wav";
            try (InputStream in = cl.getResourceAsStream(res)) {
                if (in == null) {
                    continue; // only the 9 wired events ship generated WAVs
                }
                byte[] bytes = IoStreams.readAllBytes(in);
                try (AudioInputStream ais = AudioDecoding.toPcm(
                        AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes)))) {
                    assertEquals("PCM encoding for " + res,
                            AudioFormat.Encoding.PCM_SIGNED, ais.getFormat().getEncoding());
                    byte[] pcm = IoStreams.readAllBytes(ais);
                    assertTrue(res + " decodes to no audio", pcm.length > 882); // >10ms mono
                }
            }
            validated++;
        }
        assertEquals("expected exactly 9 wired WAV events", 9, validated);
    }

    /**
     * The march OGGs must survive the two-step SPI conversion to PCM —
     * passing a VORBISENC stream to Clip.open() is what previously produced
     * static and forced the synth fallback. Reads a small slice only (the
     * full decode is ~38MB).
     */
    @Test
    public void marchOgg_convertsToPcm() throws Exception {
        String res = "assets/sounds/soundtrack/fight_song.ogg";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(res)) {
            assertNotNull("missing " + res, in);
            byte[] bytes = IoStreams.readAllBytes(in);
            try (AudioInputStream ais = AudioDecoding.toPcm(
                    AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes)))) {
                assertEquals(AudioFormat.Encoding.PCM_SIGNED, ais.getFormat().getEncoding());
                byte[] slice = new byte[65536];
                int total = 0;
                int n;
                while (total < slice.length
                        && (n = ais.read(slice, total, slice.length - total)) != -1) {
                    total += n;
                }
                assertTrue("march decode produced no PCM", total > 0);
            }
        }
    }

    @Test
    public void preload_marksAvailableWhenAssetsPresent() {
        DesktopAudioManager manager = new DesktopAudioManager();
        try {
            assertTrue(manager.isAvailable());
            // Play may fail on a headless mixer; that must not permanently disable decode-capable audio.
            manager.play(AudioEvent.UI_CLICK);
            assertTrue(manager.isAvailable());
        } finally {
            manager.dispose();
        }
    }
}
