package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.ListView;

import simulation.League;
import simulation.Team;
import ui.TeamRankingsList;

public final class SeasonalDialogController {
    private SeasonalDialogController() {}

    public static void showPreseasonOptions(MainActivity activity, League simLeague, Runnable onSave) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("This will let you redshirt and set budgets in the future")
                .setTitle(simLeague.getYear() + " Pre-Season")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setNegativeButton("SAVE PROGRESS", (dialog, which) -> {
                    if (onSave != null) onSave.run();
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
    }

    public static void showMidseasonSummary(MainActivity activity, Team userTeam, League simLeague) {
        simLeague.midSeasonProgression();
        String string = userTeam.midseasonUserProgression();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(string)
                .setTitle("Mid-Season Progress Report")
                .setPositiveButton("OK", (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
    }

    public static void showSeasonSummary(MainActivity activity, League simLeague, Team userTeam,
                                          Runnable onPrestigeChange) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(simLeague.seasonSummaryStr()
                        + "\n\nNote: You can always review your season summary in the Offseason News.")
                .setTitle(simLeague.getYear() + " Season Summary")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setNegativeButton("All Prestige Changes", (dialog, which) -> {
                    if (onPrestigeChange != null) onPrestigeChange.run();
                });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);

        simLeague.getNewsStories().get(simLeague.currentWeek + 1).add("Season Summary>" + simLeague.seasonSummaryStr());
        simLeague.getNewsHeadlines().add("That wraps up the " + simLeague.getYear() + " Season");
    }

    public static void showPrestigeChange(MainActivity activity, League simLeague, Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle("Prestige Rankings")
                .setPositiveButton("OK", (dialog, which) -> {})
                .setView(activity.getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Prestige Movement",
                "See which programs are rising and falling across the current college football landscape.");

        ListView teamRankingsList = dialog.findViewById(R.id.listViewDialog);
        teamRankingsList.setAdapter(new TeamRankingsList(activity,
                simLeague.getTeamRankingsStr(1), userTeam.getName()));
    }

    public static void showSuspensions(MainActivity activity, Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(userTeam.suspensionNews)
                .setTitle("DISCIPLINARY ACTION")
                .setPositiveButton("OK", (dialog, which) -> {});
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.setDialogMessageTextSize(dialog);
        userTeam.suspension = false;
    }

    public static void showExitConfirmation(MainActivity activity, Runnable onExit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Are you sure you want to return to main menu? Any progress from the beginning of the season will be lost.")
                .setPositiveButton("Yes, Exit", (dialog, which) -> {
                    if (onExit != null) onExit.run();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
    }
}
