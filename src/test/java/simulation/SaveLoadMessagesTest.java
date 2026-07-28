package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SaveLoadMessagesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void classifiesEmptyIncompatibleAndUnreadableSlots() {
        assertTrue(SaveLoadMessages.isEmptySlot(SaveLoadMessages.EMPTY));
        assertTrue(SaveLoadMessages.isEmptySlot(null));
        assertFalse(SaveLoadMessages.isLoadable(SaveLoadMessages.EMPTY));

        assertTrue(SaveLoadMessages.isIncompatible(SaveLoadMessages.LEGACY_INCOMPATIBLE));
        assertTrue(SaveLoadMessages.isIncompatible("Title\nMode\nUnsupported Version: v9.9"));
        assertTrue(SaveLoadMessages.isIncompatible("Old Save leftover"));
        assertFalse(SaveLoadMessages.isLoadable(SaveLoadMessages.LEGACY_INCOMPATIBLE));

        assertTrue(SaveLoadMessages.isUnreadable(SaveLoadMessages.UNREADABLE));
        assertFalse(SaveLoadMessages.isLoadable(SaveLoadMessages.UNREADABLE));

        assertTrue(SaveLoadMessages.isLoadable("Alabama\nHead Coach Career  |  12-Team Playoff\nVersion: v1.4e"));
    }

    @Test
    public void toastCopyMatchesSlotKind() {
        assertEquals(SaveLoadMessages.TOAST_EMPTY, SaveLoadMessages.toastForSlot(SaveLoadMessages.EMPTY));
        assertEquals(SaveLoadMessages.TOAST_UNREADABLE,
                SaveLoadMessages.toastForSlot(SaveLoadMessages.UNREADABLE));
        assertEquals(SaveLoadMessages.TOAST_INCOMPATIBLE,
                SaveLoadMessages.toastForSlot(SaveLoadMessages.LEGACY_INCOMPATIBLE));
    }

    @Test
    public void loadFailureMessage_preservesUnsupportedAndMissingHints() {
        assertTrue(SaveLoadMessages.loadFailureMessage(
                new IOException("Unsupported save schema version 'v9'")).contains("Unsupported"));
        assertTrue(SaveLoadMessages.loadFailureMessage(
                new IOException("Save file is missing the L: league header")).contains("missing"));
        assertTrue(SaveLoadMessages.loadFailureMessage(
                new java.io.FileNotFoundException("no such file")).toLowerCase().contains("not found"));
        assertTrue(SaveLoadMessages.loadFailureMessage(null).toLowerCase().contains("unable to load"));
    }

    @Test
    public void unreadableSlot_isNotReportedAsEmpty() throws Exception {
        File slot = LeagueSaveStorage.getSlotFile(tmp.getRoot(), 0);
        try (FileWriter w = new FileWriter(slot, StandardCharsets.UTF_8)) {
            // Valid-looking binary garbage that still opens as a file but fails summary parse
            // is hard to force through FileReader; instead write a one-byte file that summarize
            // treats as a title without version — that remains loadable. Use a directory named
            // like a slot file to force IOException in summarize.
        }
        File bad = LeagueSaveStorage.getSlotFile(tmp.getRoot(), 1);
        assertTrue(bad.mkdir());
        String[] infos = LeagueSaveStorage.getSaveFileInfos(tmp.getRoot(), League.CURRENT_SAVE_VERSION);
        assertEquals(SaveLoadMessages.UNREADABLE, infos[1]);
        assertFalse(SaveLoadMessages.isEmptySlot(infos[1]));
        assertFalse(SaveLoadMessages.isLoadable(infos[1]));
    }
}
