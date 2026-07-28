package desktop;

import org.junit.Test;
import simulation.AudioEvent;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileReader;
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

    @Test
    public void classpath_canDecodeBundledClickOgg() throws Exception {
        java.io.File ogg = new java.io.File(System.getProperty("user.dir"),
                "src/main/assets/sounds/click.ogg");
        assertTrue("Expected " + ogg.getAbsolutePath(), ogg.isFile());
        try (InputStream is = new java.io.BufferedInputStream(new java.io.FileInputStream(ogg))) {
            assertNotNull(AudioSystem.getAudioInputStream(is));
        }
    }
}
