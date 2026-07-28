package desktop;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DesktopUpdateCheckerTest {

    @Test
    public void isSameVersion_matchesCommonTagShapes() {
        assertTrue(DesktopUpdateChecker.isSameVersion("1.4e", "1.4e"));
        assertTrue(DesktopUpdateChecker.isSameVersion("1.4e", "v1.4e"));
        assertTrue(DesktopUpdateChecker.isSameVersion("1.4e", "desktop-1.4e"));
        assertTrue(DesktopUpdateChecker.isSameVersion("1.4e", "CFHC-desktop-1.4e"));
    }

    @Test
    public void isSameVersion_rejectsDifferentOrHotfixSuffix() {
        assertFalse(DesktopUpdateChecker.isSameVersion("1.4e", "1.5.0"));
        assertFalse(DesktopUpdateChecker.isSameVersion("1.4e", "desktop-1.5a"));
        assertFalse(DesktopUpdateChecker.isSameVersion("1.4e", "1.4e-hotfix"));
        assertFalse(DesktopUpdateChecker.isSameVersion("1.4e", "CFHC-desktop-1.4e.1"));
    }

    @Test
    public void isDesktopRelease_prefersDesktopTagsAndAssets() {
        assertTrue(DesktopUpdateChecker.isDesktopRelease("desktop-1.4e", "{}"));
        assertTrue(DesktopUpdateChecker.isDesktopRelease("1.4e", "{}"));
        assertTrue(DesktopUpdateChecker.isDesktopRelease("v1.4.5",
                "\"name\":\"CFHC-desktop-1.4e.jar\""));
        assertFalse(DesktopUpdateChecker.isDesktopRelease("v1.4.5",
                "\"name\":\"app-debug.apk\""));
    }

    @Test
    public void selectDesktopReleaseTag_skipsAndroidLatest() {
        String json = """
                [
                  {"tag_name":"v1.4.5","assets":[{"name":"app-release.apk"}]},
                  {"tag_name":"desktop-1.4e","assets":[{"name":"CFHC-desktop-1.4e.jar"}]}
                ]
                """;
        assertEquals("desktop-1.4e", DesktopUpdateChecker.selectDesktopReleaseTag(json));
    }

    @Test
    public void selectDesktopReleaseTag_returnsNullWhenNone() {
        String json = """
                [
                  {"tag_name":"v1.4.5","assets":[{"name":"app-release.apk"}]},
                  {"tag_name":"v1.4.4","assets":[{"name":"app-debug.apk"}]}
                ]
                """;
        assertNull(DesktopUpdateChecker.selectDesktopReleaseTag(json));
    }

    @Test
    public void parseFirstTagName_readsJson() {
        assertEquals("desktop-1.4e",
                DesktopUpdateChecker.parseFirstTagName("{\"tag_name\":\"desktop-1.4e\"}"));
    }

    @Test
    public void version_matchesGradleProperty() throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("gradle.properties")) {
            p.load(in);
        }
        assertEquals("Keep DesktopVersion.VERSION / FALLBACK in sync with gradle.properties",
                p.getProperty("desktopVersion"), DesktopVersion.VERSION);
        assertTrue(DesktopVersion.DISPLAY.contains(DesktopVersion.VERSION));
        assertTrue(DesktopVersion.RELEASES_URL.contains("github.com"));
        assertTrue(DesktopVersion.RELEASES_API.contains("/releases"));
    }
}
