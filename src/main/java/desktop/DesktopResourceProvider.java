package desktop;

import simulation.PlatformResourceProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Desktop implementation of PlatformResourceProvider.
 * Locates assets and strings from the local filesystem.
 */
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
            String resourcePath = "values/" + fileName;
            if (!loadXmlFromClasspath(resourcePath)) {
                loadXmlFromFilesystem(projectRoot + "/src/main/res/" + resourcePath);
            }
        }

        String[] requiredKeys = {
                KEY_LEAGUE_PLAYER_NAMES,
                KEY_LEAGUE_LAST_NAMES,
                KEY_CONFERENCES,
                KEY_TEAMS,
                KEY_BOWLS
        };

        for (String key : requiredKeys) {
            if (!stringMap.containsKey(key)) {
                throw new IllegalStateException("Missing desktop resource key: " + key);
            }
        }
    }

    private boolean loadXmlFromClasspath(String resourcePath) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return false;
            }
            loadXmlContent(new String(inputStream.readAllBytes()));
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
            loadXmlContent(new String(Files.readAllBytes(Paths.get(path))));
        } catch (IOException e) {
            throw new IllegalStateException("Error loading resource: " + path, e);
        }
    }

    private void loadXmlContent(String content) {
        Pattern pattern = Pattern.compile("<string name=\"([^\"]+)\">([^<]+)</string>");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            stringMap.put(matcher.group(1), matcher.group(2).trim());
        }
    }

    @Override
    public String getString(String key) {
        return stringMap.getOrDefault(key, "[" + key + "]");
    }

    @Override
    public String getString(String key, Object... args) {
        String format = getString(key);
        return String.format(format.replace("%s", "%1$s"), args);
    }

    @Override
    public InputStream openAsset(String path) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("assets/" + path);
        if (inputStream != null) {
            return inputStream;
        }

        String assetPath = projectRoot + "/src/main/assets/" + path;
        return new FileInputStream(assetPath);
    }
}
