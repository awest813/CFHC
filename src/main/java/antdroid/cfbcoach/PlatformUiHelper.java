package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
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
     * Set the title and subtitle for the shared rankings/list shell (team_rankings_dialog).
     */
    public static void bindRankingsDialogShell(AlertDialog dialog, String title, String subtitle) {
        bindShell(dialog, R.id.textDialogShellTitle, R.id.textDialogShellSubtitle, title, subtitle);
    }

    /**
     * Set title and subtitle for the simple list shell (simple_list_dialog).
     */
    public static void bindSimpleListDialogShell(AlertDialog dialog, String title, String subtitle) {
        bindShell(dialog, R.id.textSimpleDialogShellTitle, R.id.textSimpleDialogShellSubtitle, title, subtitle);
    }

    /**
     * Set title and subtitle for the archive / postseason shell (bowl_ccg_dialog).
     */
    public static void bindArchiveDialogShell(AlertDialog dialog, String title, String subtitle) {
        bindShell(dialog, R.id.textArchiveShellTitle, R.id.textArchiveShellSubtitle, title, subtitle);
    }

    /**
     * Set title and subtitle for the graph shell (graphview).
     */
    public static void bindGraphDialogShell(AlertDialog dialog, String title, String subtitle) {
        bindShell(dialog, R.id.textGraphShellTitle, R.id.textGraphShellSubtitle, title, subtitle);
    }

    /**
     * Shared binder behind the bind*DialogShell helpers: every custom dialog shell exposes a
     * Cf.Display title and a Cf.Caption subtitle; an empty subtitle collapses its row so the
     * header card keeps a tight rhythm. Null views (layout without a shell) are ignored.
     */
    private static void bindShell(AlertDialog dialog, int titleId, int subtitleId, String title, String subtitle) {
        TextView shellTitle = dialog.findViewById(titleId);
        TextView shellSubtitle = dialog.findViewById(subtitleId);
        if (shellTitle != null) shellTitle.setText(title);
        if (shellSubtitle != null) {
            shellSubtitle.setText(subtitle);
            shellSubtitle.setVisibility(subtitle == null || subtitle.trim().isEmpty() ? View.GONE : View.VISIBLE);
        }
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
     * In-app confirmation (save done, export done, blocked action) as a HUD-styled
     * Snackbar above the bottom action bar. Falls back to a Toast when the
     * activity has no content view yet. Use a Toast instead while a dialog stays
     * open on top, since a Snackbar draws in the activity window beneath it.
     */
    public static void snack(android.app.Activity activity, CharSequence message) {
        if (activity == null || message == null) return;
        android.view.View root = activity.findViewById(android.R.id.content);
        if (root == null) {
            android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        com.google.android.material.snackbar.Snackbar bar =
                com.google.android.material.snackbar.Snackbar.make(root, message,
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT);
        bar.setBackgroundTint(androidx.core.content.ContextCompat.getColor(activity, R.color.cf_card_elevated));
        bar.setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.cf_text_primary));
        bar.setActionTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.cf_emerald));
        android.view.View anchor = activity.findViewById(R.id.simGameButton);
        if (anchor != null && anchor.isShown()) bar.setAnchorView(anchor);
        bar.show();
    }

    /**
     * Show a simple notification dialog with an OK button.
     */
    public static void showNotification(android.content.Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        showImmersive(dialog);
    }

    /**
     * Align a plain-message AlertDialog body with the dialog shells: body size from the
     * cf_text_body token (14sp) plus the same 2dp line spacing as TextAppearance.Cfhc.DialogBody.
     */
    public static void setDialogMessageTextSize(AlertDialog dialog) {
        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            android.content.res.Resources res = textView.getResources();
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.cf_text_body));
            textView.setLineSpacing(res.getDisplayMetrics().density * 2f, 1f);
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
