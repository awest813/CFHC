package desktop;

/**
 * Single source of truth for the desktop release label used in UI, jars, and update checks.
 */
public final class DesktopVersion {

    /** Short version token embedded in jar names and packaging metadata (e.g. {@code 1.4e}). */
    public static final String VERSION = "1.4e";

    /** User-facing banner string. */
    public static final String DISPLAY = "Desktop " + VERSION;

    /** GitHub releases landing page (manual download / update check). */
    public static final String RELEASES_URL = "https://github.com/awest813/CFHC/releases";

    /** GitHub API endpoint for the latest published release. */
    public static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/awest813/CFHC/releases/latest";

    private DesktopVersion() {}
}
