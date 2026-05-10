package desktop;

import simulation.PlatformResourceProvider;

/**
 * Files and string keys the Swing shell expects when loading {@link DesktopResourceProvider}.
 * <p>
 * Gradle {@code prepareDesktopResources} must copy every {@link #VALUE_XML_FILES} entry into the
 * desktop runtime classpath; keep {@code build.gradle} {@code desktopValueXmlFiles} in sync.
 */
public final class DesktopResourceContract {

    private DesktopResourceContract() {
    }

    /**
     * {@code res/values/*.xml} entries merged into the desktop string map (order irrelevant).
     */
    public static final String[] VALUE_XML_FILES = {
            "strings.xml",
            "first_names.xml",
            "last_names.xml",
            "conferences.xml",
            "teams.xml",
            "bowls.xml",
    };

    /**
     * Keys that must be present after parsing those XML files (startup fails otherwise).
     */
    public static final String[] REQUIRED_STRING_KEYS = {
            PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES,
            PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES,
            PlatformResourceProvider.KEY_CONFERENCES,
            PlatformResourceProvider.KEY_TEAMS,
            PlatformResourceProvider.KEY_BOWLS,
    };
}
