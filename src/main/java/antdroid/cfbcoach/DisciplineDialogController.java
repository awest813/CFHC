package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;

import positions.Player;
import simulation.Team;

final class DisciplineDialogController {
    private DisciplineDialogController() {
    }

    static void showDisciplineAction(final MainActivity activity, final Player player, final String issue, final int gamesA, final int gamesB, final Team userTeam) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setTitle(R.string.discipline_title);
        builder.setMessage(player.position + " " + player.getName() + " (" + player.ratOvr + ") violated a team policy related to " + issue + ".\n\nThe team discipline rating is currently " + userTeam.getTeamDisciplineScore() + "%\n\nHow do you want to proceed?");
        builder.setCancelable(false);
        builder.setPositiveButton(activity.getResources().getQuantityString(R.plurals.discipline_suspend_games, gamesA, gamesA), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userTeam.disciplineAction(player, issue, gamesA, 2);
                dialog.dismiss();
                if (userTeam.suspension) activity.showSuspensions();
                activity.resetUI();
            }
        });
        builder.setNegativeButton(activity.getResources().getQuantityString(R.plurals.discipline_suspend_games, gamesB, gamesB), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userTeam.disciplineAction(player, issue, gamesB, 1);
                dialog.dismiss();
                if (userTeam.suspension) activity.showSuspensions();
                activity.resetUI();
            }
        });
        builder.setNeutralButton(R.string.discipline_ignore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userTeam.disciplineAction(player, issue, gamesA, 3);
                dialog.dismiss();
                if (userTeam.suspension) activity.showSuspensions();
                activity.resetUI();
            }
        });
        AlertDialog dialog = builder.create();
        PlatformUiHelper.showImmersive(dialog);
        userTeam.disciplineAction = false;
    }
}
