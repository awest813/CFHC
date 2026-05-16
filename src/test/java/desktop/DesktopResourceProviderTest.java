package desktop;

import org.junit.Before;
import org.junit.Test;

import simulation.PlatformResourceProvider;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DesktopResourceProviderTest {

    private DesktopResourceProvider provider;

    @Before
    public void setUp() {
        provider = new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void allRequiredKeysProduceNonPlaceholder() {
        String[] requiredKeys = {
                PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES,
                PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES,
                PlatformResourceProvider.KEY_CONFERENCES,
                PlatformResourceProvider.KEY_TEAMS,
                PlatformResourceProvider.KEY_BOWLS,
        };
        for (String key : requiredKeys) {
            String value = provider.getString(key);
            assertNotNull("Key should not produce null: " + key, value);
            assertFalse("Key should not produce empty: " + key, value.isEmpty());
            assertFalse("Key should produce resolved value, not placeholder: " + key,
                    value.startsWith("[") && value.endsWith("]"));
        }
    }

    @Test
    public void playerNamesAndLastNamesKeyBothNonEmpty() {
        String names = provider.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES);
        String lastNames = provider.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES);
        assertNotNull(names);
        assertNotNull(lastNames);
        assertFalse(names.isEmpty());
        assertFalse(lastNames.isEmpty());
    }

    @Test
    public void missingKeyReturnsPlaceholder() {
        String missing = provider.getString("non_existent_key_12345");
        assertEquals("[non_existent_key_12345]", missing);
    }

    @Test
    public void appNameStringIsPresent() {
        String appName = provider.getString("app_name");
        assertNotNull(appName);
        assertFalse(appName.startsWith("["));
    }
}
