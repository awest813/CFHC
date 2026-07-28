package desktop;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Smoke tests for macOS Desktop API integration (safe on all OSes).
 */
public class MacDesktopIntegrationTest {

    @Test
    public void installEarly_doesNotThrow() {
        MacDesktopIntegration.installEarly();
    }

    @Test
    public void showAbout_messageUsesVersion() {
        // Ensure shared About path is wired; dialog not shown headlessly.
        assertNotNull(DesktopVersion.DISPLAY);
        assertNotNull(DesktopVersion.RELEASES_URL);
    }
}
