package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Utility class for Android-specific UI operations used across activities and controllers.
 */
public final class PlatformUiHelper {
    private PlatformUiHelper() {
    }

    /**
     * Show an AlertDialog using the immersive helper to hide system bars.
     */
    public static void showImmersive(AlertDialog alert) {
        ImmersiveDialogHelper.show(alert);
    }

    /**
     * Set the title and subtitle for a standard rankings-style dialog layout.
     */
    public static void bindRankingsDialogShell(AlertDialog dialog, String title, String subtitle) {
        TextView shellTitle = dialog.findViewById(R.id.textDialogShellTitle);
        TextView shellSubtitle = dialog.findViewById(R.id.textDialogShellSubtitle);
        if (shellTitle != null) shellTitle.setText(title);
        if (shellSubtitle != null) shellSubtitle.setText(subtitle);
    }

    /**
     * Set title and subtitle for a simple list dialog shell.
     */
    public static void bindSimpleListDialogShell(AlertDialog dialog, String title, String subtitle) {
        TextView shellTitle = dialog.findViewById(R.id.textSimpleDialogShellTitle);
        TextView shellSubtitle = dialog.findViewById(R.id.textSimpleDialogShellSubtitle);
        if (shellTitle != null) shellTitle.setText(title);
        if (shellSubtitle != null) shellSubtitle.setText(subtitle);
    }

    /**
     * Set title and subtitle for an archive dialog shell.
     */
    public static void bindArchiveDialogShell(AlertDialog dialog, String title, String subtitle) {
        TextView shellTitle = dialog.findViewById(R.id.textArchiveShellTitle);
        TextView shellSubtitle = dialog.findViewById(R.id.textArchiveShellSubtitle);
        if (shellTitle != null) shellTitle.setText(title);
        if (shellSubtitle != null) shellSubtitle.setText(subtitle);
    }

    /**
     * Set title and subtitle for a graph dialog shell.
     */
    public static void bindGraphDialogShell(AlertDialog dialog, String title, String subtitle) {
        TextView shellTitle = dialog.findViewById(R.id.textGraphShellTitle);
        TextView shellSubtitle = dialog.findViewById(R.id.textGraphShellSubtitle);
        if (shellTitle != null) shellTitle.setText(title);
        if (shellSubtitle != null) shellSubtitle.setText(subtitle);
    }

    /**
     * Prevents the spinner from taking focus when its dropdown is shown, 
     * which helps maintain immersive mode in some Android versions.
     */
    public static void avoidSpinnerDropdownFocus(Spinner spinner) {
        spinner.setFocusable(false);
        spinner.setFocusableInTouchMode(false);
    }

    /**
     * Show a simple notification dialog with an OK button.
     */
    public static void showNotification(android.content.Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        showImmersive(dialog);
    }

    public static void setDialogMessageTextSize(AlertDialog dialog) {
        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    /**
     * Returns the string at the given index from a String array, or "" if the index is out of bounds.
     */
    public static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    /**
     * Hides system navigation and status bars for immersive mode.
     */
    public static void hideSystemUI(android.app.Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}
