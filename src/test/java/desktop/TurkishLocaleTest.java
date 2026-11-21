package desktop;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import simulation.AudioEvent;

import static org.junit.Assert.assertTrue;

/**
 * Turkish lower-cases "I" to dotless "ı" (and upper-cases "i" to "İ"). Code that
 * turned identifiers into file names or compared lower-cased English words
 * with the default locale broke there: sounds named after events ("WIN" ->
 * "wın.wav") went missing and "WINDOWS" stopped matching "win".
 */
public class TurkishLocaleTest {

    private Locale saved;

    @Before
    public void turkish() {
        saved = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR"));

    }

    @After
    public void restore() {
        Locale.setDefault(saved);
    }

    @Test
    public void everySoundStillLoads() {
        DesktopAudioManager manager = new DesktopAudioManager();
        try {
            for (AudioEvent e : AudioEvent.values()) {
                assertTrue("no sound for " + e + " under tr-TR", manager.hasSound(e));
            }
        } finally {
            manager.dispose();
        }
    }
}
