package desktop;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manual update check against GitHub Releases (no auto-download / self-replace).
 * Prefer desktop-tagged releases / {@code CFHC-desktop-*} assets over Android APK tags.
 * Fail-soft when offline or the API is unavailable.
 */
public final class DesktopUpdateChecker {

    public enum Status {
        UP_TO_DATE,
        UPDATE_AVAILABLE,
        UNKNOWN,
        OFFLINE
    }

    public record Result(Status status, String remoteTag, String message) {}

    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ASSET_NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final int CONNECT_TIMEOUT_MS = 4_000;
    private static final int READ_TIMEOUT_MS = 6_000;

    private DesktopUpdateChecker() {}

    public static Result check() {
        return check(DesktopVersion.VERSION, DesktopVersion.RELEASES_API);
    }

    /** Package-visible for unit tests. */
    static Result check(String localVersion, String apiUrl) {
        try {
            String body = httpGet(apiUrl);
            if (body == null || body.isBlank()) {
                return new Result(Status.UNKNOWN, null,
                        "Could not read release information. Try again later, or open the releases page.");
            }
            String tag = selectDesktopReleaseTag(body);
            if (tag == null) {
                return new Result(Status.UNKNOWN, null,
                        "No desktop release was found among recent GitHub releases.\n"
                                + "You are running " + DesktopVersion.DISPLAY + ".\n"
                                + "Open the releases page to check manually.");
            }
            if (isSameVersion(localVersion, tag)) {
                return new Result(Status.UP_TO_DATE, tag,
                        "You are on " + DesktopVersion.DISPLAY + " (latest desktop release: " + tag + ").\n"
                                + "Updates are manual downloads — this app does not auto-install.");
            }
            return new Result(Status.UPDATE_AVAILABLE, tag,
                    "A newer desktop release may be available: " + tag
                            + "\nYou are running " + DesktopVersion.DISPLAY + "."
                            + "\n\nOpen the releases page to download."
                            + "\n(This app does not auto-install updates.)");
        } catch (java.net.UnknownHostException | java.net.SocketTimeoutException
                 | java.net.ConnectException e) {
            return new Result(Status.OFFLINE, null,
                    "Could not reach GitHub (offline or blocked). Open the releases page when connected.");
        } catch (Exception e) {
            return new Result(Status.UNKNOWN, null,
                    "Update check failed: " + e.getMessage()
                            + "\nOpen the releases page to check manually.");
        }
    }

    /**
     * Picks the newest release that looks like a desktop build (tag or asset name).
     * Skips Android-style {@code v1.4.5} tags unless they also ship a desktop jar asset.
     */
    static String selectDesktopReleaseTag(String releasesJson) {
        List<ReleaseSlice> releases = splitReleases(releasesJson);
        for (ReleaseSlice r : releases) {
            if (isDesktopRelease(r.tag, r.body)) {
                return r.tag;
            }
        }
        return null;
    }

    static boolean isDesktopRelease(String tag, String releaseJsonSlice) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String lowerTag = tag.toLowerCase(Locale.ROOT);
        if (lowerTag.contains("desktop")) {
            return true;
        }
        if (releaseJsonSlice != null) {
            Matcher assets = ASSET_NAME_PATTERN.matcher(releaseJsonSlice);
            while (assets.find()) {
                String name = assets.group(1).toLowerCase(Locale.ROOT);
                if (name.contains("cfhc-desktop") || name.endsWith(".appimage")
                        || name.contains("desktop-portable")) {
                    return true;
                }
            }
        }
        String n = normalize(tag);
        // Desktop line historically uses tokens like 1.4e (digit.digit + letter).
        if (n.matches("\\d+\\.\\d+[a-z]\\w*")) {
            return true;
        }
        // Pure Android version tags (v1.4.5) without desktop assets are not desktop releases.
        return false;
    }

    /** True when local and remote resolve to the same desktop version token. */
    static boolean isSameVersion(String localVersion, String remoteTag) {
        if (localVersion == null || localVersion.isBlank() || remoteTag == null || remoteTag.isBlank()) {
            return true;
        }
        return normalize(localVersion).equals(normalize(remoteTag));
    }

    /** @deprecated use {@link #isSameVersion}; kept for older call sites/tests. */
    @Deprecated
    static boolean isSameOrOlder(String localVersion, String remoteTag) {
        return isSameVersion(localVersion, remoteTag);
    }

    static String normalize(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("v") && s.length() > 1 && Character.isDigit(s.charAt(1))) {
            s = s.substring(1);
        }
        s = s.replace("cfhc-desktop-", "")
                .replace("cfhc-", "")
                .replace("desktop-", "")
                .replace("desktop ", "");
        return s.trim();
    }

    static String parseFirstTagName(String body) {
        Matcher m = TAG_PATTERN.matcher(body);
        return m.find() ? m.group(1).trim() : null;
    }

    private static List<ReleaseSlice> splitReleases(String body) {
        List<ReleaseSlice> out = new ArrayList<>();
        // Rough split on objects that contain tag_name — good enough for GitHub list JSON.
        Matcher m = TAG_PATTERN.matcher(body);
        List<Integer> starts = new ArrayList<>();
        List<String> tags = new ArrayList<>();
        while (m.find()) {
            starts.add(m.start());
            tags.add(m.group(1).trim());
        }
        for (int i = 0; i < tags.size(); i++) {
            int from = starts.get(i);
            int to = (i + 1 < starts.size()) ? starts.get(i + 1) : body.length();
            out.add(new ReleaseSlice(tags.get(i), body.substring(from, to)));
        }
        return out;
    }

    private record ReleaseSlice(String tag, String body) {}

    private static String httpGet(String apiUrl) throws Exception {
        URL url = URI.create(apiUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "CFHC-Desktop/" + DesktopVersion.VERSION);
        int code = conn.getResponseCode();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("HTTP " + code);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    public static boolean openReleasesPage() {
        try {
            if (!Desktop.isDesktopSupported()) {
                return false;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(URI.create(DesktopVersion.RELEASES_URL));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
