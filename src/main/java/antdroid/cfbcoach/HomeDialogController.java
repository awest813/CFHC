package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

import simulation.GameFlowManager;
import simulation.LeagueLaunchCoordinator;
import ui.SaveFilesList;

/**
 * Controller for dialogs in the Home activity.
 */
public final class HomeDialogController {

    private HomeDialogController() {
    }

    /**
     * Show prompt for starting a custom game.
     */
    public static void showCustomGamePrompt(final Home activity, final Runnable onOkSelected) {
        AlertDialog.Builder welcome = Dialogs.builder(activity);
        welcome.setMessage("Do you want to start the game with a custom Universe file? \nUniverse file must be correctly formatted to avoid errors!")
                .setTitle("Custom Dynasty")
                .setNeutralButton("Help", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                        intent.setData(Uri.parse("https://www.antdroid.dev/p/game-manual.html"));
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onOkSelected.run();
                    }
                });
        welcome.setCancelable(false);
        AlertDialog dialog = welcome.create();
        PlatformUiHelper.showImmersive(dialog);
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }

    /**
     * Show dialog for loading a save file.
     */
    public static void showLoadLeagueDialog(final Home activity, final String[] fileInfos, final int theme) {
        showLoadLeagueDialog(activity, fileInfos, null, theme);
    }

    /**
     * Slot picker. When {@code autosaveSummary} is non-null, a "Resume Autosave"
     * button continues from the rolling autosave written after each week.
     */
    public static void showLoadLeagueDialog(final Home activity, final String[] fileInfos,
                                            final String autosaveSummary, final int theme) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setTitle("Choose Save to Load");
        if (autosaveSummary != null) {
            builder.setNeutralButton(R.string.home_resume_autosave, (dialog, which) -> {
                activity.finish();
                activity.startActivity(GameNavigation.createMainIntent(
                        activity,
                        LeagueLaunchCoordinator.LaunchRequest.resumeAutosave(),
                        theme
                ));
            });
        }
        SaveFilesList saveFilesAdapter = new SaveFilesList(activity, fileInfos);
        builder.setAdapter(saveFilesAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                String info = fileInfos[item];
                if (simulation.SaveLoadMessages.isLoadable(info)) {
                    activity.finish();
                    activity.startActivity(GameNavigation.createMainIntent(
                            activity,
                            LeagueLaunchCoordinator.LaunchRequest.loadInternal("saveFile" + item + ".cfb"),
                            theme
                    ));
                } else {
                    Toast.makeText(activity, simulation.SaveLoadMessages.toastForSlot(info),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog alert = builder.create();
        PlatformUiHelper.showImmersive(alert);
    }

    /**
     * Show dialog for importing an external save.
     */
    public static void showImportGameDialog(final Home activity, final Runnable onOkSelected) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setTitle("Choose Save to Import");
        builder.setMessage("This feature lets you import external Exported Saves from your device. Please locate and select the desired file after pressing OK.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onOkSelected.run();
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog alert = builder.create();
        PlatformUiHelper.showImmersive(alert);
    }

    /**
     * Show dialog for deleting a save file.
     */
    public static void showDeleteSaveDialog(final Home activity, final String[] fileInfos) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setTitle("Choose Save to Delete");
        SaveFilesList saveFilesAdapter = new SaveFilesList(activity, fileInfos);
        builder.setAdapter(saveFilesAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (!fileInfos[item].equals("EMPTY")) {
                    activity.deleteFile("saveFile" + item + ".cfb");
                    Toast.makeText(activity, "File deleted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "Cannot delete empty file!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog alert = builder.create();
        PlatformUiHelper.showImmersive(alert);
    }

    /**
     * Show prestige mode selection for a new game.
     */
    public static void showPrestigeModeDialog(final Home activity, final String customUri, final GameFlowManager flowManager) {
        AlertDialog.Builder welcome = Dialogs.builder(activity);
        welcome.setMessage("Use default team prestige, randomize prestige, or equalize the field for a fresh start?")
                .setTitle("New Dynasty Setup")
                .setNeutralButton("Default", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        flowManager.startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode.DEFAULT, customUri);
                        activity.finish();
                    }
                })
                .setNegativeButton("Randomize", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        flowManager.startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode.RANDOMIZE, customUri);
                        activity.finish();
                    }
                })
                .setPositiveButton("Equalize", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        flowManager.startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode.EQUALIZE, customUri);
                        activity.finish();
                    }
                });
        welcome.setCancelable(false);
        AlertDialog dialog = welcome.create();
        PlatformUiHelper.showImmersive(dialog);
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    }

    /**
     * Show a generic message dialog.
     */
    public static void showMessageDialog(final Home activity, String title, String message, int textSizeSp) {
        AlertDialog.Builder dialogBuilder = Dialogs.builder(activity);
        dialogBuilder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", null);
        dialogBuilder.setCancelable(false);
        AlertDialog dialog = dialogBuilder.create();
        PlatformUiHelper.showImmersive(dialog);
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }

    /** Credits plus a "Licenses" button for the bundled fonts, libraries and music. */
    public static void showCreditsDialog(final Home activity, String title, String message, int textSizeSp) {
        AlertDialog dialog = Dialogs.builder(activity)
                .setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", null)
                .setNeutralButton(R.string.home_licenses, (d, w) -> showLicensesDialog(activity))
                .setCancelable(false)
                .create();
        PlatformUiHelper.showImmersive(dialog);
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }

    /** Full license texts (OFL 1.1, Apache 2.0) and attributions from res/raw. */
    public static void showLicensesDialog(final Home activity) {
        String text;
        try (java.io.InputStream in = activity.getResources().openRawResource(R.raw.open_source_licenses)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            simulation.IoStreams.copy(in, out);
            text = new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            text = activity.getString(R.string.home_licenses_unavailable);
        }
        showMessageDialog(activity, activity.getString(R.string.home_licenses), text, 11);
    }
}
