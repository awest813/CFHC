package desktop;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manual update check against GitHub Releases (no auto-download / self-replace).
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
    private static final int CONNECT_TIMEOUT_MS = 4_000;
    private static final int READ_TIMEOUT_MS = 6_000;

    private DesktopUpdateChecker() {}

    public static Result check() {
        return check(DesktopVersion.VERSION, DesktopVersion.LATEST_RELEASE_API);
    }

    /** Package-visible for unit tests. */
    static Result check(String localVersion, String apiUrl) {
        try {
            String body = httpGet(apiUrl);
            if (body == null || body.isBlank()) {
                return new Result(Status.UNKNOWN, null,
                        "Could not read release information. Try again later, or open the releases page.");
            }
            Matcher m = TAG_PATTERN.matcher(body);
            if (!m.find()) {
                return new Result(Status.UNKNOWN, null,
                        "Could not parse the latest release tag. Open the releases page to check manually.");
            }
            String tag = m.group(1).trim();
            if (isSameOrOlder(localVersion, tag)) {
                return new Result(Status.UP_TO_DATE, tag,
                        "You are on " + DesktopVersion.DISPLAY + " (latest release: " + tag + ").");
            }
            return new Result(Status.UPDATE_AVAILABLE, tag,
                    "A newer release may be available: " + tag
                            + "\nYou are running " + DesktopVersion.DISPLAY + "."
                            + "\n\nOpen the releases page to download.");
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
     * True when the remote tag looks like the same desktop line (or older wording).
     * Conservative: only treat as "update available" when the remote token clearly differs
     * and is not a substring match of the local version label.
     */
    static boolean isSameOrOlder(String localVersion, String remoteTag) {
        if (localVersion == null || localVersion.isBlank() || remoteTag == null || remoteTag.isBlank()) {
            return true;
        }
        String local = normalize(localVersion);
        String remote = normalize(remoteTag);
        if (local.equals(remote)) {
            return true;
        }
        // Tags like "desktop-1.4e", "v1.4e", "CFHC-desktop-1.4e"
        if (remote.contains(local) || local.contains(remote)) {
            return true;
        }
        return false;
    }

    private static String normalize(String raw) {
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
