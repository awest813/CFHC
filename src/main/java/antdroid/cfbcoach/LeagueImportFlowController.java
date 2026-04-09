package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public final class LeagueImportFlowController {
    public enum ImportType {
        COACH,
        ROSTER
    }

    public interface Host {
        Context getDialogContext();

        void showDialog(AlertDialog dialog);

        void requestImportDocument(ImportType type);

        void finishImportFlowForNewGame();
    }

    private LeagueImportFlowController() {
    }

    public static void showImportPrompt(final Host host, final boolean newGame) {
        AlertDialog.Builder builder = new AlertDialog.Builder(host.getDialogContext());
        builder.setMessage("Do you want to import Coach/Player data?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showImportTypePrompt(host, newGame);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (newGame) {
                            host.finishImportFlowForNewGame();
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        host.showDialog(dialog);
    }

    private static void showImportTypePrompt(final Host host, final boolean newGame) {
        AlertDialog.Builder builder = new AlertDialog.Builder(host.getDialogContext());
        builder.setMessage("What type of custom data would you like to import?")
                .setPositiveButton("Coach File", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.requestImportDocument(ImportType.COACH);
                        showImportMorePrompt(host, newGame);
                    }
                })
                .setNegativeButton("Roster File", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.requestImportDocument(ImportType.ROSTER);
                        showImportMorePrompt(host, newGame);
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showImportMorePrompt(host, newGame);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        host.showDialog(dialog);
    }

    private static void showImportMorePrompt(final Host host, final boolean newGame) {
        AlertDialog.Builder builder = new AlertDialog.Builder(host.getDialogContext());
        builder.setMessage("Do you want to import more data?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showImportTypePrompt(host, newGame);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (newGame) {
                            host.finishImportFlowForNewGame();
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        host.showDialog(dialog);
    }
}
