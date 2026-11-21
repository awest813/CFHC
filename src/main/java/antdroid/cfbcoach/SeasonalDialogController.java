package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;

import simulation.League;
import simulation.Team;
import ui.TeamRankingsList;

public final class SeasonalDialogController {
    private SeasonalDialogController() {}

    public static void showPreseasonOptions(MainActivity activity, League simLeague, Runnable onSave) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setMessage(R.string.seasonal_preseason_message)
                .setTitle(activity.getString(R.string.seasonal_preseason_title_format, simLeague.getYear()))
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {})
                .setNegativeButton(R.string.seasonal_save_progress, (dialog, which) -> {
                    if (onSave != null) onSave.run();
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
    }

    public static void showMidseasonSummary(MainActivity activity, Team userTeam, League simLeague) {
        // Progression is applied by SeasonController before this dialog opens.
        String string = userTeam != null ? userTeam.midseasonUserProgression() : "";
        if (string == null || string.trim().isEmpty()) {
            string = activity.getString(R.string.seasonal_midseason_empty);
        }
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setMessage(string)
                .setTitle(R.string.seasonal_midseason_title)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
    }

    public static void showSeasonSummary(MainActivity activity, League simLeague, Team userTeam,
                                          Runnable onPrestigeChange) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setMessage(simLeague.seasonSummaryStr()
                        + "\n\n" + activity.getString(R.string.seasonal_summary_note))
                .setTitle(activity.getString(R.string.seasonal_summary_title_format, activity.getDisplayedSeason()))
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {})
                .setNegativeButton(R.string.seasonal_all_prestige_changes, (dialog, which) -> {
                    if (onPrestigeChange != null) onPrestigeChange.run();
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);

        simLeague.addNewsStory(simLeague.currentWeek + 1, "Season Summary>" + simLeague.seasonSummaryStr());
        simLeague.addNewsHeadline("That wraps up the " + activity.getDisplayedSeason() + " season");
    }

    public static void showPrestigeChange(MainActivity activity, League simLeague, Team userTeam) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setCancelable(false);
        builder.setTitle(R.string.seasonal_prestige_title)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {})
                .setView(activity.getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog,
                activity.getString(R.string.seasonal_prestige_shell_title),
                activity.getString(R.string.seasonal_prestige_shell_subtitle));

        androidx.recyclerview.widget.RecyclerView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        ui.RecyclerLists.setAdapter(teamRankingsList, new TeamRankingsList(activity,
                simLeague.getTeamRankingsStr(1), userTeam.getName()));
    }

    public static void showSuspensions(MainActivity activity, Team userTeam) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setMessage(userTeam.suspensionNews)
                .setTitle(R.string.seasonal_discipline_title)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
        userTeam.suspension = false;
    }

    public static void showExitConfirmation(MainActivity activity, Runnable onExit) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
        builder.setMessage(R.string.seasonal_exit_message)
                .setPositiveButton(R.string.seasonal_exit_confirm, (dialog, which) -> {
                    if (onExit != null) onExit.run();
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {})
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
    }
}
