package antdroid.cfbcoach;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import simulation.PlatformResourceProvider;

/**
 * Android implementation of PlatformResourceProvider.
 */
public class AndroidResourceProvider implements PlatformResourceProvider {

    private final Context context;

    public AndroidResourceProvider(Context context) {
        this.context = context;
    }

    @Override
    public String getString(String key) {
        int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId);
        }
        return "[" + key + "]";
    }

    @Override
    public String getString(String key, Object... args) {
        int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        if (resId != 0) {
            return context.getString(resId, args);
        }
        return "[" + key + "]";
    }

    @Override
    public InputStream openAsset(String path) throws IOException {
        return context.getAssets().open(path);
    }
}
