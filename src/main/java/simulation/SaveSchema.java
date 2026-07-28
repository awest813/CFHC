package simulation;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Save-file schema versioning for the {@code SaveManager} {@code L:}/{@code V:} format.
 *
 * <p>Current files start with {@code V:<version>} followed by the {@code L:} league header.
 * Pre-version {@code L:}-only files are treated as {@link #CURRENT} for backward compatibility.
 * Unknown versions fail loudly so format drift cannot silently corrupt dynasties.
 */
public final class SaveSchema {

    /** Line prefix written at the top of new-format saves. */
    public static final String VERSION_PREFIX = "V:";

    /**
     * Versions that can be loaded without a transform. When a real format break ships,
     * bump {@link League#CURRENT_SAVE_VERSION}, add a migrator, and keep prior IDs here
     * until they are retired.
     */
    private static final Set<String> SUPPORTED;

    static {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        versions.add(League.CURRENT_SAVE_VERSION);
        SUPPORTED = Collections.unmodifiableSet(versions);
    }

    private SaveSchema() {
    }

    public static String current() {
        return League.CURRENT_SAVE_VERSION;
    }

    public static Set<String> supportedVersions() {
        return SUPPORTED;
    }

    public static boolean isSupported(String version) {
        return version != null && SUPPORTED.contains(normalize(version));
    }

    /**
     * Parses a {@code V:} line body (without prefix) or returns null if blank.
     */
    public static String parseVersionToken(String raw) throws IOException {
        if (raw == null) {
            return null;
        }
        String token = normalize(raw);
        if (token.isEmpty()) {
            throw new IOException("Save schema version line is empty (expected V:<version>)");
        }
        return token;
    }

    /**
     * Validates a version from a {@code V:} header. Unknown versions throw with a
     * user-facing message.
     */
    public static String requireSupported(String version) throws IOException {
        String normalized = normalize(version);
        if (!isSupported(normalized)) {
            throw new IOException(unsupportedMessage(normalized));
        }
        return normalized;
    }

    /**
     * Effective version when a new-format file has no {@code V:} line (Wave A / early
     * {@code SaveManager} saves). Treated as current for load compatibility.
     */
    public static String unversionedNewFormatDefault() {
        return League.CURRENT_SAVE_VERSION;
    }

    public static String unsupportedMessage(String version) {
        return "Unsupported save schema version '" + version + "'. "
                + "This build supports: " + String.join(", ", SUPPORTED) + ". "
                + "Current version: " + League.CURRENT_SAVE_VERSION + ".";
    }

    /**
     * Migration hook. Today every supported version is identity; add version-to-version
     * transforms here when the on-disk shape changes.
     */
    public static LeagueRecord migrate(String fromVersion, LeagueRecord record) throws IOException {
        String normalized = requireSupported(fromVersion);
        // Identity migrate for the single supported generation.
        if (League.CURRENT_SAVE_VERSION.equals(normalized)) {
            return record;
        }
        throw new IOException(unsupportedMessage(normalized));
    }

    public static String normalize(String version) {
        if (version == null) {
            return "";
        }
        return version.trim().toLowerCase(Locale.US);
    }
}
