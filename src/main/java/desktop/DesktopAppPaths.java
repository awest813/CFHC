package desktop;

import java.io.File;
import java.io.IOException;

final class DesktopAppPaths {

    private DesktopAppPaths() {
    }

    static File savesDir() {
        return new File(appDir(), "saves");
    }

    static File prefsFile() {
        return new File(appDir(), "prefs.properties");
    }

    static File appDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            home = System.getProperty("user.dir");
        }

        if (os.contains("mac")) {
            return new File(home, "Library/Application Support/CFHC");
        }
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return new File(appData, "CFHC");
            }
        }
        return new File(home, ".cfhc");
    }

    static File ensureSavesDir() throws IOException {
        File dir = savesDir();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create save directory: " + dir.getAbsolutePath());
        }
        return dir;
    }
}
