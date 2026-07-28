package desktop;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DesktopAppPathsTest {

    @Test
    public void chooserStartDir_isWritableSavesFolder() throws Exception {
        File dir = DesktopAppPaths.chooserStartDir();
        assertNotNull(dir);
        assertTrue(dir.isDirectory());
        assertEquals("saves", dir.getName());
        assertTrue(dir.canWrite());
    }

    @Test
    public void ensureSavesDir_idempotent() throws Exception {
        File a = DesktopAppPaths.ensureSavesDir();
        File b = DesktopAppPaths.ensureSavesDir();
        assertEquals(a.getAbsolutePath(), b.getAbsolutePath());
    }
}
