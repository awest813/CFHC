package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.util.TypedValue;
import android.widget.TextView;

import simulation.League;
import simulation.Team;

public final class TransferDialogController {
    private TransferDialogController() {}

    public static void showTransfers(MainActivity activity, League simLeague, Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(simLeague.userTransfers)
                .setTitle(simLeague.getYear() + " Transfers")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setNegativeButton("View All Transfers", (dialog, which) -> {
                    dialog.dismiss();
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(activity);
                    builder1.setMessage(simLeague.sumTransfers)
                            .setTitle(simLeague.getYear() + " Transfers")
                            .setPositiveButton("OK", (d, w) -> {});
                    AlertDialog dialog1 = builder1.create();
                    dialog1.show();
                    TextView textView1 = dialog1.findViewById(android.R.id.message);
                    textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
    }

    public static void showRedshirtList(MainActivity activity, League simLeague, Team userTeam) {
        StringBuilder update = new StringBuilder();
        update.append("The following is the list of players that were redshirted this season. Some players automatically received redshirts if they did not play in at least 4 games.\n\n");
        for (int i = 0; i < userTeam.redshirtList.size(); ++i) {
            update.append(userTeam.redshirtList.get(i)).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(update)
                .setTitle(simLeague.getYear() + " Redshirts")
                .setPositiveButton("Close", (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
    }
}
