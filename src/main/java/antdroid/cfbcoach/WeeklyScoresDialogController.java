package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import simulation.League;
import ui.NewsStories;

final class WeeklyScoresDialogController {
    private WeeklyScoresDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Weekly Scoreboard")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Weekly Scoreboard", "Flip across regular season and postseason weeks to review the full league scoreboard.");

        ArrayList<String> rankings = new ArrayList<>();
        int dbSize;
        if (simLeague.currentWeek + 2 <= simLeague.regSeasonWeeks + 5 && simLeague.currentWeek + 1 <= simLeague.regSeasonWeeks)
            dbSize = simLeague.currentWeek + 2;
        else dbSize = simLeague.regSeasonWeeks + 5;

        String[] weekSelection = new String[dbSize];
        for (int i = 0; i < weekSelection.length; ++i) {
            if (i == simLeague.regSeasonWeeks) weekSelection[i] = "Conf Champ Week";
            else if (i == simLeague.regSeasonWeeks + 1 && !simLeague.expPlayoffs) weekSelection[i] = "Bowl Week 1";
            else if (i == simLeague.regSeasonWeeks + 2 && !simLeague.expPlayoffs) weekSelection[i] = "Bowl Week 2";
            else if (i == simLeague.regSeasonWeeks + 3 && !simLeague.expPlayoffs) weekSelection[i] = "Bowl Week 3";
            else if (i == simLeague.regSeasonWeeks + 1) weekSelection[i] = "First Round";
            else if (i == simLeague.regSeasonWeeks + 2) weekSelection[i] = "Quarterfinals";
            else if (i == simLeague.regSeasonWeeks + 3) weekSelection[i] = "Semifinals";
            else if (i == simLeague.regSeasonWeeks + 4) weekSelection[i] = "National Champ";
            else weekSelection[i] = "Week " + i;
        }

        Spinner weekSelectionSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(weekSelectionSpinner);
        ArrayAdapter<String> weekSelectionSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, weekSelection);
        weekSelectionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekSelectionSpinner.setAdapter(weekSelectionSpinnerAdapter);

        int psweek = simLeague.currentWeek;
        if (psweek > simLeague.regSeasonWeeks + 4) psweek = simLeague.regSeasonWeeks + 4;
        if (simLeague.currentWeek + 2 <= simLeague.regSeasonWeeks) weekSelectionSpinner.setSelection(dbSize - 2);
        else weekSelectionSpinner.setSelection(psweek);

        final ListView newsStoriesList = dialog.findViewById(R.id.listViewTeamRankings);
        final NewsStories weeklyScoresAdapter = new NewsStories(activity, rankings);
        newsStoriesList.setAdapter(weeklyScoresAdapter);

        weekSelectionSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> scores = simLeague.getWeeklyScores().get(position);
                        boolean isempty = false;
                        if (scores.size() == 0) {
                            isempty = true;
                            scores.add(" > ");
                        }
                        weeklyScoresAdapter.clear();
                        weeklyScoresAdapter.addAll(scores);
                        weeklyScoresAdapter.notifyDataSetChanged();
                        if (isempty) {
                            scores.remove(0);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
