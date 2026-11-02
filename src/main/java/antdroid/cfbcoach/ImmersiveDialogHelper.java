package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;

final class ImmersiveDialogHelper {
    private ImmersiveDialogHelper() {
    }

    static void show(AlertDialog alert) {
        Window window = alert.getWindow();
        if (window != null) {
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }

        alert.show();

        window = alert.getWindow();
        if (window == null) {
            return;
        }

        PlatformUiHelper.hideSystemBars(window);
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
    }
}
