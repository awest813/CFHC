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

final class NewsDialogController {
    private NewsDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("News Stories")
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
        PlatformUiHelper.bindRankingsDialogShell(dialog, "League News Feed", "Review preseason updates, weekly headlines, coaching movement, and offseason storylines from one clean archive.");

        ArrayList<String> rankings = new ArrayList<>();
        String[] weekSelection = new String[simLeague.currentWeek + 1];
        for (int i = 0; i < weekSelection.length; ++i) {
            if (i == 0) weekSelection[i] = "Pre-Season News";
            else if (i == simLeague.regSeasonWeeks) weekSelection[i] = "Conf Champ Week";
            else if (i == simLeague.regSeasonWeeks + 1) weekSelection[i] = "Bowl Week 1";
            else if (i == simLeague.regSeasonWeeks + 2) weekSelection[i] = "Bowl Week 2";
            else if (i == simLeague.regSeasonWeeks + 3) weekSelection[i] = "Bowl Week 3";
            else if (i == simLeague.regSeasonWeeks + 4) weekSelection[i] = "National Champ";
            else if (i == simLeague.regSeasonWeeks + 5) weekSelection[i] = "Season Summary";
            else if (i == simLeague.regSeasonWeeks + 6) weekSelection[i] = "Coaching Contracts";
            else if (i == simLeague.regSeasonWeeks + 7) weekSelection[i] = "Off-Season News";
            else if (i == simLeague.regSeasonWeeks + 8) weekSelection[i] = "Head Coach Hirings";
            else if (i == simLeague.regSeasonWeeks + 9) weekSelection[i] = "Coordinator Hirings";
            else if (i == simLeague.regSeasonWeeks + 10) weekSelection[i] = "Transfer News";
            else if (i == simLeague.regSeasonWeeks + 11) weekSelection[i] = "Off-Season News";
            else if (i == simLeague.regSeasonWeeks + 12) weekSelection[i] = "Recruiting News";
            else weekSelection[i] = "Week " + i;
        }

        Spinner weekSelectionSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(weekSelectionSpinner);
        ArrayAdapter<String> weekSelectionSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, weekSelection);
        weekSelectionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        weekSelectionSpinner.setAdapter(weekSelectionSpinnerAdapter);
        weekSelectionSpinner.setSelection(simLeague.currentWeek);

        final ListView newsStoriesList = dialog.findViewById(R.id.listViewTeamRankings);
        final NewsStories newsStoriesAdapter = new NewsStories(activity, rankings);
        newsStoriesList.setAdapter(newsStoriesAdapter);

        weekSelectionSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> rankings = simLeague.getNewsStories().get(position);
                        boolean isempty = false;
                        if (simLeague.currentWeek == simLeague.regSeasonWeeks + 11 && rankings.size() == 0) {
                            rankings.add("National Letter of Intention Day!>Today marks the first day of open recruitment. Teams are now allowed to sign incoming freshmen to their schools.");
                        }
                        if (rankings.size() == 0) {
                            isempty = true;
                            rankings.add("No news stories.>I guess this week was a bit boring, huh?");
                        }
                        newsStoriesAdapter.clear();
                        newsStoriesAdapter.addAll(rankings);
                        newsStoriesAdapter.notifyDataSetChanged();
                        if (isempty) {
                            rankings.remove(0);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
