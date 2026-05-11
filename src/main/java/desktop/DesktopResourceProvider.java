package desktop;

import simulation.IoStreams;
import simulation.PlatformResourceProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Desktop implementation of PlatformResourceProvider.
 * Locates assets and strings from the local filesystem.
 */
public class DesktopResourceProvider implements PlatformResourceProvider {

    private final String projectRoot;
    private final Map<String, String> stringMap = new HashMap<>();

    public DesktopResourceProvider(String projectRoot) {
        this.projectRoot = projectRoot;
        loadStrings();
    }

    private void loadStrings() {
        for (String fileName : DesktopResourceContract.VALUE_XML_FILES) {
            String resourcePath = "values/" + fileName;
            if (!loadXmlFromClasspath(resourcePath)) {
                loadXmlFromFilesystem(projectRoot + "/src/main/res/" + resourcePath);
            }
        }

        for (String key : DesktopResourceContract.REQUIRED_STRING_KEYS) {
            if (!stringMap.containsKey(key)) {
                throw new IllegalStateException("Missing desktop resource key: " + key);
            }
        }
    }

    private boolean loadXmlFromClasspath(String resourcePath) {
        try (InputStream inputStream =
                     Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return false;
            }
            loadXmlContent(new String(IoStreams.readAllBytes(inputStream), StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Error loading classpath resource: " + resourcePath, e);
        }
    }

    private void loadXmlFromFilesystem(String path) {
        try {
            if (!new File(path).exists()) {
                return;
            }
            try (FileInputStream fis = new FileInputStream(path)) {
                loadXmlContent(new String(IoStreams.readAllBytes(fis), StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error loading resource: " + path, e);
        }
    }

    private void loadXmlContent(String content) {
        Pattern pattern = Pattern.compile("<string name=\"([^\"]+)\">([^<]+)</string>");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            stringMap.put(matcher.group(1), unescapeXmlValue(matcher.group(2)));
        }
    }

    private static String unescapeXmlValue(String raw) {
        return raw.trim()
                .replace("\\'", "'")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    @Override
    public String getString(String key) {
        return stringMap.getOrDefault(key, "[" + key + "]");
    }

    @Override
    public String getString(String key, Object... args) {
        String format = getString(key);
        if (args == null || args.length == 0) {
            return format;
        }
        try {
            return String.format(Locale.ROOT, format, args);
        } catch (java.util.IllegalFormatException e) {
            return format;
        }
    }

    @Override
    public InputStream openAsset(String path) throws IOException {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/" + path);
        if (inputStream != null) {
            return inputStream;
        }

        String assetPath = projectRoot + "/src/main/assets/" + path;
        return new FileInputStream(assetPath);
    }
}
