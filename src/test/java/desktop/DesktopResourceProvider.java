package desktop;

import simulation.PlatformResourceProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DesktopResourceProvider implements PlatformResourceProvider {
    private static final String[] VALUE_FILES = {
            "strings.xml",
            "first_names.xml",
            "last_names.xml",
            "conferences.xml",
            "teams.xml",
            "bowls.xml"
    };

    private final String projectRoot;
    private final Map<String, String> stringMap = new HashMap<>();

    public DesktopResourceProvider(String projectRoot) {
        this.projectRoot = projectRoot;
        loadStrings();
    }

    private void loadStrings() {
        for (String fileName : VALUE_FILES) {
            loadXmlFromFilesystem(projectRoot + "/src/main/res/values/" + fileName);
        }
    }

    private void loadXmlFromFilesystem(String path) {
        try {
            loadXmlContent(new String(Files.readAllBytes(Paths.get(path))));
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
            return String.format(format, args);
        } catch (java.util.IllegalFormatException e) {
            return format;
        }
    }

    @Override
    public InputStream openAsset(String path) throws IOException {
        return new FileInputStream(projectRoot + "/src/main/assets/" + path);
    }
}
