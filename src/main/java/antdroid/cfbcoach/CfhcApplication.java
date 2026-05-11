package antdroid.cfbcoach;

import android.app.Application;

/**
 * Application entry. Uses the fixed navy / gold dynasty palette from {@code styles.xml}
 * so the UI stays cohesive (management-sim readability). Dynamic Material You theming
 * is intentionally disabled to preserve that consistent sports-dashboard look end-to-end.
 */
public class CfhcApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
