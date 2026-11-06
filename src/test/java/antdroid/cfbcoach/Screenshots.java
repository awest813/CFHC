package antdroid.cfbcoach;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Renders a view to a PNG under {@code app/build/screenshots/} (Robolectric
 * native graphics). The images are for reviewing layouts without an emulator;
 * the tests assert on structure, not on pixels.
 */
final class Screenshots {

    private Screenshots() {
    }

    static File capture(View root, String name) throws IOException {
        int w = root.getWidth() > 0 ? root.getWidth() : 1080;
        int h = root.getHeight() > 0 ? root.getHeight() : 2340;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        File dir = new File(System.getProperty("user.dir"), "app/build/screenshots");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdirs " + dir);
        File out = new File(dir, name + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        return out;
    }
}
