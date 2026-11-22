package antdroid.cfbcoach;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Applies the in-app theme setting (1 = light, 0 = dark; see {@link GameNavigation#getTheme}).
 *
 * <p>The two palettes are resource qualifiers, not separate layouts: {@code values/} holds the
 * dark broadcast HUD and {@code values-notnight/} the light palette (token colours, Material3
 * Light parents, light system-bar icons). The theme setting is mapped onto AppCompat's night
 * mode, so every layout, drawable, style and {@code ContextCompat.getColor} lookup
 * resolves the right token for the chosen theme, independent of the device's own
 * dark-mode switch.
 */
public final class CfhcTheme {

    /** Theme setting value for the light palette. */
    public static final int LIGHT = 1;
    /** Theme setting value for the dark palette. */
    public static final int DARK = 0;

    static final String PREFS = "cfhc_home";
    static final String KEY_THEME = "theme_choice";

    private CfhcTheme() {
    }

    /** AppCompat night mode for a theme setting value. */
    public static int nightModeFor(int theme) {
        return theme == LIGHT ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
    }

    /** The theme the player last chose on Home (also the default before any screen passes one). */
    public static int savedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME, GameNavigation.DEFAULT_THEME);
    }

    /**
     * Sets the process-wide night mode for {@code theme}. Called from
     * {@link CfhcApplication#onCreate} with the saved choice so activities attach with the
     * right palette and never need recreating; returns true if the mode changed (AppCompat then
     * re-applies it to running activities).
     */
    public static boolean applyNightMode(int theme) {
        int mode = nightModeFor(theme);
        if (AppCompatDelegate.getDefaultNightMode() == mode) {
            return false;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
        return true;
    }

    /**
     * Applies {@code theme} to an activity. Call before {@code super.onCreate}: the activity
     * style is set and the night mode matched so the content view inflates with that
     * palette.
     */
    public static void apply(AppCompatActivity activity, int theme) {
        applyNightMode(theme);
        activity.setTheme(theme == LIGHT ? R.style.AppThemeLight : R.style.AppTheme);
    }
}
