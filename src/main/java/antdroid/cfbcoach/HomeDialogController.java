package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.TypedValue;
import android.widget.TextView;
import android.widget.Toast;

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
        AlertDialog.Builder welcome = new AlertDialog.Builder(activity);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Choose File to Load:");
        SaveFilesList saveFilesAdapter = new SaveFilesList(activity, fileInfos);
        builder.setAdapter(saveFilesAdapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (!fileInfos[item].equals("EMPTY")) {
                    if (!fileInfos[item].contains("Old Save")) {
                        activity.finish();
                        activity.startActivity(GameNavigation.createMainIntent(
                                activity,
                                LeagueLaunchCoordinator.LaunchRequest.loadInternal("saveFile" + item + ".cfb"),
                                theme
                        ));
                    } else {
                        Toast.makeText(activity, "Incompatible Save!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "Cannot load empty file!", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Choose File to Import:");
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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Choose File to Delete:");
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
        AlertDialog.Builder welcome = new AlertDialog.Builder(activity);
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
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("OK", null);
        dialogBuilder.setCancelable(false);
        AlertDialog dialog = dialogBuilder.create();
        PlatformUiHelper.showImmersive(dialog);
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }
}
