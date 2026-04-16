package simulation;

import java.io.InputStream;

/**
 * Interface for providing platform-specific resources (strings, assets).
 * This allows the simulation layer to access text and data without depending on Android R.
 */
public interface PlatformResourceProvider {
    
    /**
     * Get a localized string from the platform's resource bundle.
     */
    String getString(String key);

    /**
     * Get a string with format arguments.
     */
    String getString(String key, Object... args);

    /**
     * Load an asset or file as an InputStream.
     */
    InputStream openAsset(String path) throws java.io.IOException;
    
    /**
     * Common string keys used across the simulation.
     */
    String KEY_APP_NAME = "app_name";
    String KEY_CHANGELOG = "changelog";
    String KEY_CREDITS = "credits";
    
    String KEY_LEAGUE_PLAYER_NAMES = "league_player_names";
    String KEY_LEAGUE_LAST_NAMES = "league_last_names";
    String KEY_CONFERENCES = "conferences";
    String KEY_TEAMS = "teams";
    String KEY_BOWLS = "bowls";
}
