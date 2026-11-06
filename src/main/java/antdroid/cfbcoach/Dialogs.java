package antdroid.cfbcoach;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/**
 * The one place dialogs are created (UI redesign Wave 4). Every builder is
 * the AppCompat {@link AlertDialog.Builder}, so the theme's
 * {@code alertDialogTheme} ({@code ThemeOverlay.Cfhc.Dialog}) applies to all
 * of them. {@code scripts/verify_android_res.py} fails the build check if
 * code constructs an {@code AlertDialog.Builder} or
 * {@code MaterialAlertDialogBuilder} anywhere else.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** A themed dialog builder. */
    public static AlertDialog.Builder builder(Context context) {
        return new AlertDialog.Builder(context);
    }

    /** Shows a dialog without bringing the system bars back (immersive). */
    public static void show(AlertDialog dialog) {
        PlatformUiHelper.showImmersive(dialog);
    }
}
