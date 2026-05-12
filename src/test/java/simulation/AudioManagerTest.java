package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AudioManagerTest {

    @Test
    public void audioEvent_enumHasExpectedValues() {
        AudioEvent[] values = AudioEvent.values();
        assertEquals(9, values.length);
        assertEquals(AudioEvent.UI_CLICK, AudioEvent.valueOf("UI_CLICK"));
        assertEquals(AudioEvent.CONFIRM, AudioEvent.valueOf("CONFIRM"));
        assertEquals(AudioEvent.ERROR, AudioEvent.valueOf("ERROR"));
        assertEquals(AudioEvent.WHISTLE, AudioEvent.valueOf("WHISTLE"));
        assertEquals(AudioEvent.PLAY_SELECT, AudioEvent.valueOf("PLAY_SELECT"));
        assertEquals(AudioEvent.FIRST_DOWN, AudioEvent.valueOf("FIRST_DOWN"));
        assertEquals(AudioEvent.ADVANCE, AudioEvent.valueOf("ADVANCE"));
        assertEquals(AudioEvent.WIN, AudioEvent.valueOf("WIN"));
        assertEquals(AudioEvent.LOSS, AudioEvent.valueOf("LOSS"));
    }

    @Test
    public void noOp_playDoesNotThrow() {
        AudioManager.NO_OP.play(AudioEvent.UI_CLICK);
        AudioManager.NO_OP.play(AudioEvent.ADVANCE);
        AudioManager.NO_OP.play(AudioEvent.WIN);
        AudioManager.NO_OP.play(AudioEvent.LOSS);
    }

    @Test
    public void noOp_volumeDefaultsToOne() {
        assertEquals(1f, AudioManager.NO_OP.getVolume(), 0f);
    }

    @Test
    public void noOp_isNotMutedByDefault() {
        assertFalse(AudioManager.NO_OP.isMuted());
    }

    @Test
    public void noOp_setVolumeDoesNotThrow() {
        AudioManager.NO_OP.setVolume(0.5f);
        AudioManager.NO_OP.setVolume(0f);
        AudioManager.NO_OP.setVolume(1f);
    }

    @Test
    public void noOp_setMutedDoesNotThrow() {
        AudioManager.NO_OP.setMuted(true);
        AudioManager.NO_OP.setMuted(false);
    }

    @Test
    public void noOp_disposeDoesNotThrow() {
        AudioManager.NO_OP.dispose();
    }

    @Test
    public void noOp_isSingleton() {
        assertNotNull(AudioManager.NO_OP);
    }
}
