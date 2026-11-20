package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AudioLevelsTest {

    @Test
    public void curveIsQuadraticAndClamped() {
        assertEquals(0f, AudioLevels.amplitude(0f), 0f);
        assertEquals(1f, AudioLevels.amplitude(1f), 0f);
        assertEquals(0.25f, AudioLevels.amplitude(0.5f), 1e-6f);
        assertEquals(1f, AudioLevels.amplitude(3f), 0f);
        assertEquals(0f, AudioLevels.amplitude(-1f), 0f);
        assertEquals(0f, AudioLevels.amplitude(Float.NaN), 0f);
    }

    @Test
    public void decibelsMatchAmplitude() {
        assertEquals(AudioLevels.SILENT_DB, AudioLevels.decibels(0f), 0f);
        assertEquals(0f, AudioLevels.decibels(1f), 1e-6f);
        // 0.5 slider → 0.25 amplitude → −12.04 dB
        assertEquals(-12.04f, AudioLevels.decibels(0.5f), 0.01f);
    }

    @Test
    public void trackTrimsNeverBoost() {
        for (SoundtrackEngine.Track t : SoundtrackEngine.Track.values()) {
            assertTrue(t + " boosts (tracks peak near 0 dBFS)", t.getGainTrimDb() <= 0f);
            assertTrue(t.getGainTrim() > 0f && t.getGainTrim() <= 1f);
        }
    }
}
