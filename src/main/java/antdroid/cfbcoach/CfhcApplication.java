package antdroid.cfbcoach;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

/**
 * Application entry for global Material theming. Applies Material dynamic color on
 * API 31+ when available; older devices keep the static palette from {@code styles.xml}.
 */
public class CfhcApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
