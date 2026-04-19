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
import ui.PlayerRankingsList;

final class PlayerRankingsDialogController {
    private PlayerRankingsDialogController() {
    }

    static void show(final MainActivity activity, final League simLeague, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Player Statistical Rankings")
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
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Player Statistical Rankings", "Review national leaderboards for passing, rushing, receiving, kicking, and defensive statistics.");

        ArrayList<String> rankings = new ArrayList<>();
        String[] rankingsSelection =
                {"Passer Rating", "Passing Yards", "Passing TDs", "Interceptions Thrown", "Pass Comp PCT", "Rushing Yards", "Rushing TDs", "Receptions", "Receiving Yards", "Receiving TDs",
                        "Tackles", "Sacks", "Fumbles Recovered", "Interceptions", "Field Goals Made", "Field Goal Pct", "Kickoff Return Yards", "Kickoff Return TDs", "Punt Return Yards", "Punt Return TDs",
                        "Head Coach - Overall", "Head Coach - Season Score"
                };
        Spinner playerRankingsSpinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(playerRankingsSpinner);
        ArrayAdapter<String> playerRankingsSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, rankingsSelection);
        playerRankingsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerRankingsSpinner.setAdapter(playerRankingsSpinnerAdapter);

        final ListView playerRankingsList = dialog.findViewById(R.id.listViewTeamRankings);
        final PlayerRankingsList playerRankingsAdapter =
                new PlayerRankingsList(activity, rankings, userTeam.getAbbr(), activity);
        playerRankingsList.setAdapter(playerRankingsAdapter);

        playerRankingsSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        ArrayList<String> rankings = simLeague.getPlayerRankStr(position);
                        playerRankingsAdapter.setUserTeamStrRep(userTeam.getAbbr());
                        playerRankingsAdapter.clear();
                        playerRankingsAdapter.addAll(rankings);
                        playerRankingsAdapter.notifyDataSetChanged();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
    }
}
