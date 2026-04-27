package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import simulation.LeagueImportWorkflow;

public final class LeagueImportFlowController {
    public interface Host {
        Context getDialogContext();

        void showDialog(AlertDialog dialog);

        void requestImportDocument(LeagueImportWorkflow.ImportType type);

        void finishImportFlowForNewGame();
    }

    private LeagueImportFlowController() {
    }

    public static void showImportPrompt(final Host host, final boolean newGame) {
        showStep(host, new LeagueImportWorkflow(newGame), null);
    }

    private static void showStep(final Host host, final LeagueImportWorkflow workflow, LeagueImportWorkflow.Step previousStep) {
        if (previousStep != null && previousStep.requestedImportType != null) {
            host.requestImportDocument(previousStep.requestedImportType);
        }
        LeagueImportWorkflow.Step step = workflow.currentStep();
        if (step.finishImportFlowForNewGame) {
            host.finishImportFlowForNewGame();
            return;
        }
        if (step.prompt == LeagueImportWorkflow.Prompt.IMPORT_DATA) {
            showImportPrompt(host, workflow);
        } else if (step.prompt == LeagueImportWorkflow.Prompt.IMPORT_TYPE) {
            showImportTypePrompt(host, workflow);
        } else if (step.prompt == LeagueImportWorkflow.Prompt.IMPORT_MORE) {
            showImportMorePrompt(host, workflow);
        } else if (step.prompt == LeagueImportWorkflow.Prompt.COMPLETE) {
            // Existing-game import flows simply end here; new-game completion is handled above.
            return;
        }
    }

    private static void showImportPrompt(final Host host, final LeagueImportWorkflow workflow) {
        AlertDialog.Builder builder = new AlertDialog.Builder(host.getDialogContext());
        builder.setMessage("Do you want to import Coach/Player data?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.chooseImportData(true));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.chooseImportData(false));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        host.showDialog(dialog);
    }

    private static void showImportTypePrompt(final Host host, final LeagueImportWorkflow workflow) {
        AlertDialog.Builder builder = new AlertDialog.Builder(host.getDialogContext());
        builder.setMessage("What type of custom data would you like to import?")
                .setPositiveButton("Coach File", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.selectImportType(LeagueImportWorkflow.ImportType.COACH));
                    }
                })
                .setNegativeButton("Roster File", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.selectImportType(LeagueImportWorkflow.ImportType.ROSTER));
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.cancelImportType());
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        host.showDialog(dialog);
    }

    private static void showImportMorePrompt(final Host host, final LeagueImportWorkflow workflow) {
        AlertDialog.Builder builder = new AlertDialog.Builder(host.getDialogContext());
        builder.setMessage("Do you want to import more data?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.chooseImportMore(true));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showStep(host, workflow, workflow.chooseImportMore(false));
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        host.showDialog(dialog);
    }
}
