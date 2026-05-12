package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SaveLoadServiceTest {
    private File tempDir;
    private SaveLoadService service;

    @Before
    public void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "cfhc_test_" + System.nanoTime());
        tempDir.mkdirs();
        service = new SaveLoadService(tempDir);
    }

    @Test
    public void constructor_acceptsValidDirectory() {
        assertNotNull(service);
    }

    @Test
    public void isSlotEmpty_returnsTrueForNewService() {
        assertTrue(service.isSlotEmpty(0));
        assertTrue(service.isSlotEmpty(5));
        assertTrue(service.isSlotEmpty(9));
    }

    @Test
    public void getSlotFile_returnsNonNullFile() {
        File f = service.getSlotFile(3);
        assertNotNull(f);
        assertTrue(f.getName().contains("saveFile"));
    }

    @Test
    public void getSaveFileSummaries_doesNotThrow() {
        String[] summaries = service.getSaveFileSummaries();
        assertNotNull(summaries);
        assertEquals(20, summaries.length);
    }

    @Test
    public void saveToSlot_and_isSlotEmpty_areConsistent() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);

        assertTrue(service.isSlotEmpty(0));
        boolean saved = service.saveToSlot(league, 0);
        assertTrue("Save should succeed", saved);
        assertFalse("Slot should not be empty after save", service.isSlotEmpty(0));
    }
}
