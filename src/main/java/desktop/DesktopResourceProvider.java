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

    private final String projectRoot;
    private final Map<String, String> stringMap = new HashMap<>();

    public DesktopResourceProvider(String projectRoot) {
        this.projectRoot = projectRoot;
        loadStrings();
    }

    private void loadStrings() {
        // Load from res/values/strings.xml, first_names.xml, last_names.xml
        String resPath = projectRoot + "/src/main/res/values/";
        loadXmlFile(resPath + "strings.xml");
        loadXmlFile(resPath + "first_names.xml");
        loadXmlFile(resPath + "last_names.xml");
        
        // Add hardcoded mappings for keys expected by the simulation
        stringMap.put(KEY_LEAGUE_PLAYER_NAMES, stringMap.getOrDefault("league_player_names", ""));
        stringMap.put(KEY_LEAGUE_LAST_NAMES, stringMap.getOrDefault("league_last_names", ""));
        
        // Default empty strings for others if not found
        String[] keys = {KEY_CONFERENCES, KEY_TEAMS, KEY_BOWLS};
        for (String k : keys) {
            if (!stringMap.containsKey(k)) stringMap.put(k, "");
        }
    }

    private void loadXmlFile(String path) {
        try {
            if (!new File(path).exists()) return;
            String content = new String(Files.readAllBytes(Paths.get(path)));
            Pattern pattern = Pattern.compile("<string name=\"([^\"]+)\">([^<]+)</string>");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                stringMap.put(matcher.group(1), matcher.group(2).trim());
            }
        } catch (IOException e) {
            System.err.println("Error loading resource: " + path);
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
        String assetPath = projectRoot + "/src/main/assets/" + path;
        return new FileInputStream(assetPath);
    }
}
