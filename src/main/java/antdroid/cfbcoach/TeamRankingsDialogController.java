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
import simulation.Team;
import ui.TeamRankingsList;

final class TeamRankingsDialogController {
    private TeamRankingsDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Team Statistical Rankings")
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
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Team Statistical Rankings", "Review weekly performance across all major team statistical categories.");

        ArrayList<String> rankings = new ArrayList<>();
        String[] rankingsSelection =
                {"Power Index", "Prestige", "RPI", "Strength of Schedule", "Strength of Wins", "Points Per Game", "Opp Points Per Game",
                        "Yards Per Game", "Opp Yards Per Game", "Pass Yards Per Game", "Rush Yards Per Game",
                        "Opp Pass YPG", "Opp Rush YPG", "TO Differential", "Off Talent", "Def Talent", "Team Chemistry", "Recruiting Class", "Discipline Score", "Team Budget", "Team Facilities", "Head Coach - Overall", "Head Coach Score"};
        Spinner teamRankingsSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamRankingsSpinner);
        ArrayAdapter<String> teamRankingsSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, rankingsSelection);
        teamRankingsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamRankingsSpinner.setAdapter(teamRankingsSpinnerAdapter);

        final ListView teamRankingsList = dialog.findViewById(R.id.listViewTeamRankings);
        final TeamRankingsList teamRankingsAdapter =
                new TeamRankingsList(activity, rankings, userTeam.getName());
        teamRankingsList.setAdapter(teamRankingsAdapter);

        teamRankingsSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> rankings = simLeague.getTeamRankingsStr(position);

                        teamRankingsAdapter.setUserTeamStrRep(userTeam.getName());
                        if (position == 16)
                            teamRankingsAdapter.setUserTeamStrRep(userTeam.getName() + "\n" + userTeam.getTopRecruit());

                        teamRankingsAdapter.clear();
                        teamRankingsAdapter.addAll(rankings);
                        teamRankingsAdapter.notifyDataSetChanged();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
