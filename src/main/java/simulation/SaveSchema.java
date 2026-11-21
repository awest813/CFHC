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

    /**
     * The generation before team/conference/league state lines. It loads through the
     * same reader: the new lines are simply absent and their fields take defaults.
     */
    public static final String V1_4E = "v1.4e";

    /**
     * The generation with team/conference/league state lines but without the v1.5b
     * lists ({@code LL:}, {@code TL:}, {@code PX:}, {@code XP:}, {@code XS:}, {@code XC:}).
     * It loads through the same reader with these defaults: no news before the load,
     * awards decided again from the restored players if the save falls between the
     * conference championship and graduation (see {@code League.ensureSeasonAwardsForRead}),
     * empty offseason and transfer lists, coach pools merged into the free agents,
     * fresh FCS opponents, recomputed talent and ranks, and the RNG restarted from the seed.
     */
    public static final String V1_5A = "v1.5a";

    /**
     * The generation with the full between-weeks state but without the played games'
     * box scores (the v1.5c {@code LL:boxScores} list). It loads through the same
     * reader; its past games show only their final scores.
     */
    public static final String V1_5B = "v1.5b";

    /**
     * The generation with box scores but without the {@code END_SAVE} trailer (and
     * with names written unescaped). It loads through the same reader; a truncated
     * file of it cannot be told from a complete one.
     */
    public static final String V1_5C = "v1.5c";

    /**
     * The generation with the {@code END_SAVE} trailer but without the national
     * recruiting pool ({@code LL:recruitPool*}, v1.5e). It loads through the same
     * reader; a save of it at the recruiting gate after CPU classes were signed gets
     * a pool built on load for the user's board, with no CPU needs left to fill.
     */
    public static final String V1_5D = "v1.5d";

    static {
        LinkedHashSet<String> versions = new LinkedHashSet<>();
        versions.add(League.CURRENT_SAVE_VERSION);
        versions.add(V1_5D);
        versions.add(V1_5C);
        versions.add(V1_5B);
        versions.add(V1_5A);
        versions.add(V1_4E);
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
     * Migration hook. v1.4e, v1.5a, v1.5b, v1.5c and v1.5d to the current version are identity at the
     * record level: the fields later versions added are optional lines, and
     * {@code League} defaults them (or rebuilds them from the schedules) when a record
     * does not carry them ({@link LeagueRecord.LeagueState#hasFullState()} tells which).
     */
    public static LeagueRecord migrate(String fromVersion, LeagueRecord record) throws IOException {
        String normalized = requireSupported(fromVersion);
        if (League.CURRENT_SAVE_VERSION.equals(normalized) || V1_5D.equals(normalized)
                || V1_5C.equals(normalized) || V1_5B.equals(normalized)
                || V1_5A.equals(normalized) || V1_4E.equals(normalized)) {
            return record;
        }
        throw new IOException(unsupportedMessage(normalized));
    }

    /**
     * Whether saves of {@code version} end with the {@code END_SAVE} trailer (v1.5d
     * on), so one without it is incomplete. Null (no {@code V:} line) predates it.
     */
    public static boolean hasEndTrailer(String version) {
        String v = normalize(version);
        return version != null && (League.CURRENT_SAVE_VERSION.equals(v) || V1_5D.equals(v));
    }

    public static String normalize(String version) {
        if (version == null) {
            return "";
        }
        return version.trim().toLowerCase(Locale.US);
    }
}
