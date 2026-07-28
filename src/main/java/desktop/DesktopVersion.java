package desktop;

import java.io.InputStream;
import java.util.Properties;

/**
 * Desktop release label used in UI, jars, and update checks.
 * Keep {@link #VERSION} in sync with the {@code desktopVersion} Gradle property.
 */
public final class DesktopVersion {

    /** Short version token embedded in jar names and packaging metadata (e.g. {@code 1.4e}). */
    public static final String VERSION = loadVersion();

    /** User-facing banner string. */
    public static final String DISPLAY = "Desktop " + VERSION;

    /** GitHub releases landing page (manual download / update check). */
    public static final String RELEASES_URL = "https://github.com/awest813/CFHC/releases";

    /** GitHub API: recent releases (not {@code /latest}, which may be Android-only). */
    public static final String RELEASES_API =
            "https://api.github.com/repos/awest813/CFHC/releases?per_page=15";

    private static final String FALLBACK_VERSION = "1.4e";

    private DesktopVersion() {}

    private static String loadVersion() {
        try (InputStream in = DesktopVersion.class.getResourceAsStream("/desktop-version.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String v = p.getProperty("version");
                if (v != null && !v.isBlank()) {
                    return v.trim();
                }
            }
        } catch (Exception ignored) {
            // Fall through to compile-time fallback.
        }
        return FALLBACK_VERSION;
    }
}
