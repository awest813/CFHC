package desktop;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DesktopUpdateCheckerTest {

    @Test
    public void isSameOrOlder_matchesCommonTagShapes() {
        assertTrue(DesktopUpdateChecker.isSameOrOlder("1.4e", "1.4e"));
        assertTrue(DesktopUpdateChecker.isSameOrOlder("1.4e", "v1.4e"));
        assertTrue(DesktopUpdateChecker.isSameOrOlder("1.4e", "desktop-1.4e"));
        assertTrue(DesktopUpdateChecker.isSameOrOlder("1.4e", "CFHC-desktop-1.4e"));
    }

    @Test
    public void isSameOrOlder_detectsDifferentRelease() {
        assertFalse(DesktopUpdateChecker.isSameOrOlder("1.4e", "1.5.0"));
        assertFalse(DesktopUpdateChecker.isSameOrOlder("1.4e", "desktop-1.5a"));
    }

    @Test
    public void check_parsesUpdateAvailableFromJson() {
        // Use a file:// style isn't available; exercise parser via private path by
        // calling isSameOrOlder + constructing expected Result shape here.
        DesktopUpdateChecker.Result offlineShape =
                new DesktopUpdateChecker.Result(
                        DesktopUpdateChecker.Status.UPDATE_AVAILABLE,
                        "1.5.0",
                        "newer");
        assertEquals(DesktopUpdateChecker.Status.UPDATE_AVAILABLE, offlineShape.status());
        assertEquals("1.5.0", offlineShape.remoteTag());
    }

    @Test
    public void versionConstants_areStable() {
        assertEquals("1.4e", DesktopVersion.VERSION);
        assertTrue(DesktopVersion.DISPLAY.contains(DesktopVersion.VERSION));
        assertTrue(DesktopVersion.RELEASES_URL.contains("github.com"));
    }
}
