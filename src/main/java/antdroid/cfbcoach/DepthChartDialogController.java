package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import simulation.Player;
import simulation.Team;
import ui.DepthChart;
import ui.RedshirtAdapter;

final class DepthChartDialogController {
    private DepthChartDialogController() {
    }

    static void showDepthChart(final MainActivity activity, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Set Team Lineup")
                .setView(activity.getLayoutInflater().inflate(R.layout.team_lineup_dialog, null));
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        final String[] positionSelection = {"Quarterbacks", "Running Backs", "Wide Receivers", "Tight Ends", "Off Linemen",
                "Kickers", "Def Linemen", "Linebackers", "Cornerbacks", "Safeties"};
        final int[] positionNumberRequired = {userTeam.startersQB, userTeam.startersRB, userTeam.startersWR, userTeam.startersTE, userTeam.startersOL, userTeam.startersK, userTeam.startersDL, userTeam.startersLB, userTeam.startersCB, userTeam.startersS};
        final Spinner teamLineupPositionSpinner = dialog.findViewById(R.id.spinnerTeamLineupPosition);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamLineupPositionSpinner);
        ArrayAdapter<String> teamLineupPositionSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, positionSelection);
        teamLineupPositionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamLineupPositionSpinner.setAdapter(teamLineupPositionSpinnerAdapter);

        final TextView minPlayersText = dialog.findViewById(R.id.textMinPlayers);
        final TextView textLineupPositionDescription = dialog.findViewById(R.id.textViewLineupPositionDescription);

        final ArrayList<Player> positionPlayers = new ArrayList<>();
        positionPlayers.addAll(userTeam.getTeamQBs());

        final ListView teamPositionList = dialog.findViewById(R.id.listViewTeamLineup);
        final DepthChart teamLineupAdapter = new DepthChart(activity, positionPlayers, 1);
        teamPositionList.setAdapter(teamLineupAdapter);

        teamLineupPositionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                minPlayersText.setText("Starters: " + positionNumberRequired[position]);
                updateLineupList(userTeam, position, teamLineupAdapter, positionNumberRequired, positionPlayers, textLineupPositionDescription);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Button doneButton = dialog.findViewById(R.id.buttonDoneWithLineups);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                activity.updateCurrTeam();
            }
        });

        Button saveButton = dialog.findViewById(R.id.buttonSaveLineups);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int positionSpinner = teamLineupPositionSpinner.getSelectedItemPosition();
                if (teamLineupAdapter.playersSelected.size() == teamLineupAdapter.playersRequired) {
                    userTeam.setStarters(teamLineupAdapter.playersSelected, positionSpinner);
                    updateLineupList(userTeam, positionSpinner, teamLineupAdapter, positionNumberRequired, positionPlayers, textLineupPositionDescription);
                    Toast.makeText(activity, "Saved lineup for " + positionSelection[positionSpinner] + "!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, teamLineupAdapter.playersSelected.size() + " players selected.\nNot the correct number of starters (" + teamLineupAdapter.playersRequired + ")", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    static void showRedshirt(final MainActivity activity, final Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Redshirt Players")
                .setView(activity.getLayoutInflater().inflate(R.layout.team_lineup_dialog, null));
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        final String[] positionSelection = {"Quarterbacks", "Running Backs", "Wide Receivers", "Tight Ends", "Off Linemen",
                "Kickers", "Def Linemen", "Linebackers", "Cornerbacks", "Safeties"};
        final int[] positionNumberRequired = {userTeam.minQBs, userTeam.minRBs, userTeam.minWRs, userTeam.minTEs, userTeam.minOLs, userTeam.minKs, userTeam.minDLs, userTeam.minLBs, userTeam.minCBs, userTeam.minSs};
        final Spinner teamLineupPositionSpinner = dialog.findViewById(R.id.spinnerTeamLineupPosition);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamLineupPositionSpinner);
        ArrayAdapter<String> teamLineupPositionSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, positionSelection);
        teamLineupPositionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamLineupPositionSpinner.setAdapter(teamLineupPositionSpinnerAdapter);

        final TextView minPlayersText = dialog.findViewById(R.id.textMinPlayers);
        final TextView textLineupPositionDescription = dialog.findViewById(R.id.textViewLineupPositionDescription);

        final ArrayList<Player> positionPlayers = new ArrayList<>();
        positionPlayers.addAll(userTeam.getTeamQBs());

        final ListView teamPositionList = dialog.findViewById(R.id.listViewTeamLineup);
        final RedshirtAdapter redshirtSelector = new RedshirtAdapter(activity, positionPlayers, 1);
        teamPositionList.setAdapter(redshirtSelector);

        teamLineupPositionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                minPlayersText.setText("Min Active: " + positionNumberRequired[position] + " Current Active: " + userTeam.getActivePlayers(position));
                redshirtLineup(userTeam, position, redshirtSelector, positionNumberRequired, positionPlayers, textLineupPositionDescription);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Button doneButton = dialog.findViewById(R.id.buttonDoneWithLineups);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button depthchartButton = activity.findViewById(R.id.buttonDepthChart);
                if (!activity.redshirtComplete) depthchartButton.setText("SET REDSHIRTS");
                dialog.dismiss();
                activity.updateCurrTeam();
            }
        });

        Button saveButton = dialog.findViewById(R.id.buttonSaveLineups);
        saveButton.setText("REDSHIRT PLAYERS");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int positionSpinner = teamLineupPositionSpinner.getSelectedItemPosition();
                if (redshirtSelector.playersSelected.size() + userTeam.countRedshirts() - redshirtSelector.playersRemoved.size() <= 10) {
                    userTeam.setRedshirts(redshirtSelector.playersSelected, redshirtSelector.playersRemoved, positionSpinner);
                    redshirtSelector.playersSelected.clear();
                    redshirtSelector.playersRemoved.clear();
                    redshirtLineup(userTeam, positionSpinner, redshirtSelector, positionNumberRequired, positionPlayers, textLineupPositionDescription);
                    minPlayersText.setText("Min Active: " + positionNumberRequired[positionSpinner] + " Current Active: " + userTeam.getActivePlayers(positionSpinner));
                    Toast.makeText(activity, "Set redshirts for " + positionSelection[positionSpinner] + "! You currently have " + userTeam.countRedshirts() + " (Max: 9) redshirted players.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity, "A maximum of 10 players can be redshirted each season. You have exceeded this! You currently have " + userTeam.countRedshirts() + " redshirted players.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static void updateLineupList(Team userTeam, int position, DepthChart teamLineupAdapter, int[] positionNumberRequired,
                                         ArrayList<Player> positionPlayers, TextView textLineupPositionDescription) {
        teamLineupAdapter.playersRequired = positionNumberRequired[position];
        teamLineupAdapter.playersSelected.clear();
        teamLineupAdapter.players.clear();
        positionPlayers.clear();
        switch (position) {
            case 0:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Pass Strength, Pass Accuracy, Evasion, Speed)");
                positionPlayers.addAll(userTeam.getTeamQBs());
                break;
            case 1:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Speed, Evasion, Power, Catch)");
                positionPlayers.addAll(userTeam.teamRBs);
                break;
            case 2:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Speed, Catch, Evasion, Jump)");
                positionPlayers.addAll(userTeam.teamWRs);
                break;
            case 3:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Block, Catch, Evasion, Speed)");
                positionPlayers.addAll(userTeam.getTeamTEs());
                break;
            case 4:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Run Block, Pass Block, Vision, Strength)");
                positionPlayers.addAll(userTeam.getTeamOLs());
                break;
            case 5:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Kick Strength, Kick Accuracy, Pressure, Form)");
                positionPlayers.addAll(userTeam.getTeamKs());
                break;
            case 6:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Run Stop, Tackle, Pass Rush, Strength)");
                positionPlayers.addAll(userTeam.getTeamDLs());
                break;
            case 7:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Tackle, Run Stop, Cover, Speed)");
                positionPlayers.addAll(userTeam.teamLBs);
                break;
            case 8:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Cover, Speed, Tackle, Jump)");
                positionPlayers.addAll(userTeam.teamCBs);
                break;
            case 9:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Tackle, Cover, Speed, Run Stop)");
                positionPlayers.addAll(userTeam.teamSs);
                break;
        }

        for (int i = 0; i < teamLineupAdapter.playersRequired; ++i) {
            teamLineupAdapter.playersSelected.add(positionPlayers.get(i));
        }
        teamLineupAdapter.notifyDataSetChanged();
    }

    private static void redshirtLineup(Team userTeam, int position, RedshirtAdapter redshirtSelector, int[] positionNumberRequired,
                                       ArrayList<Player> positionPlayers, TextView textLineupPositionDescription) {
        redshirtSelector.playersRequired = positionNumberRequired[position];
        redshirtSelector.playersSelected.clear();
        redshirtSelector.players.clear();
        positionPlayers.clear();
        switch (position) {
            case 0:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Pass Strength, Pass Accuracy, Evasion, Speed)");
                positionPlayers.addAll(userTeam.getTeamQBs());
                break;
            case 1:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Power, Speed, Evasion, Catch)");
                positionPlayers.addAll(userTeam.teamRBs);
                break;
            case 2:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Catch, Speed, Evaasion, Jump)");
                positionPlayers.addAll(userTeam.teamWRs);
                break;
            case 3:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Catch, Run Block, Evasion, Speed)");
                positionPlayers.addAll(userTeam.getTeamTEs());
                break;
            case 4:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Strength, Run Block, Pass Block, Awareness)");
                positionPlayers.addAll(userTeam.getTeamOLs());
                break;
            case 5:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Kick Strength, Kick Accuracy, Clumsiness, Pressure)");
                positionPlayers.addAll(userTeam.getTeamKs());
                break;
            case 6:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Strength, Run Def, Pass Def, Tackle)");
                positionPlayers.addAll(userTeam.getTeamDLs());
                break;
            case 7:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Cover, Run Def, Tackle, Run Stop)");
                positionPlayers.addAll(userTeam.teamLBs);
                break;
            case 8:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Cover, Speed, Tackle, Jump)");
                positionPlayers.addAll(userTeam.teamCBs);
                break;
            case 9:
                textLineupPositionDescription.setText("Name [Yr] Overall/Potential\n(Cover, Speed, Tackle, Run Stop)");
                positionPlayers.addAll(userTeam.teamSs);
                break;
        }
        redshirtSelector.notifyDataSetChanged();
    }
}
