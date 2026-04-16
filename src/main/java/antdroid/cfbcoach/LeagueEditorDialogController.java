package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import simulation.Conference;
import simulation.League;
import simulation.Team;

/**
 * Controller for the Game Universe Editor.
 */
public final class LeagueEditorDialogController {

    private LeagueEditorDialogController() {
    }

    public static void show(final MainActivity activity, final League simLeague) {
        final Team[] currentTeam = {activity.userTeam};
        final Conference[] currentConference = {simLeague.conferences.get(simLeague.getConfNumber(activity.userTeam.conference))};

        AlertDialog.Builder GameEditor = new AlertDialog.Builder(activity);
        GameEditor.setTitle("Game Universe Editor v2 (BETA)")
                .setView(activity.getLayoutInflater().inflate(R.layout.game_editor_full, null));
        final AlertDialog dialog = GameEditor.create();
        PlatformUiHelper.showImmersive(dialog);

        final List<String> teamEditorList = new ArrayList<>();
        final List<String> confEditorList = new ArrayList<>();
        final Spinner confListSpinner = dialog.findViewById(R.id.confList);
        final Spinner teamListSpinner = dialog.findViewById(R.id.teamList);
        PlatformUiHelper.avoidSpinnerDropdownFocus(confListSpinner);
        PlatformUiHelper.avoidSpinnerDropdownFocus(teamListSpinner);

        final EditText changeNameEditText = dialog.findViewById(R.id.editTextChangeName);
        final EditText changeAbbrEditText = dialog.findViewById(R.id.editTextChangeAbbr);
        final EditText changeLocationText = dialog.findViewById(R.id.editLocation);
        final EditText changeConfEditText = dialog.findViewById(R.id.editTextChangeConf);
        final EditText changeHCEditText = dialog.findViewById(R.id.editTextChangeHC);
        final EditText changePrestigeEditText = dialog.findViewById(R.id.editPrestige);

        final ArrayAdapter<String> editorAdaptorConf = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, confEditorList);
        editorAdaptorConf.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        confListSpinner.setAdapter(editorAdaptorConf);

        final ArrayAdapter<String> editorAdaptorTeam = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, teamEditorList);
        editorAdaptorTeam.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamListSpinner.setAdapter(editorAdaptorTeam);

        for (int i = 0; i < simLeague.conferences.size(); i++) {
            confEditorList.add(simLeague.conferences.get(i).confName);
        }
        for (int i = 0; i < currentConference[0].confTeams.size(); i++) {
            teamEditorList.add(currentConference[0].confTeams.get(i).name);
        }
        editorAdaptorConf.notifyDataSetChanged();
        editorAdaptorTeam.notifyDataSetChanged();

        confListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (changeNameEditText != null) changeNameEditText.clearComposingText();
                if (changeAbbrEditText != null) changeAbbrEditText.clearComposingText();
                if (changeLocationText != null) changeLocationText.clearComposingText();
                if (changeConfEditText != null) changeConfEditText.clearComposingText();
                if (changeHCEditText != null) changeHCEditText.clearComposingText();
                if (changePrestigeEditText != null) changePrestigeEditText.clearComposingText();

                currentConference[0] = simLeague.conferences.get(position);
                teamEditorList.clear();
                for (int i = 0; i < currentConference[0].confTeams.size(); i++) {
                    teamEditorList.add(currentConference[0].confTeams.get(i).name);
                }
                editorAdaptorTeam.notifyDataSetChanged();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        teamListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Team tm = currentConference[0].confTeams.get(position);
                currentTeam[0] = tm;

                if (changeNameEditText != null) changeNameEditText.setText(currentTeam[0].name);
                if (changeAbbrEditText != null) changeAbbrEditText.setText(currentTeam[0].abbr);
                if (changeLocationText != null) changeLocationText.setText(Integer.toString(currentTeam[0].location));
                if (changeConfEditText != null) changeConfEditText.setText(currentConference[0].confName);
                if (changeHCEditText != null) changeHCEditText.setText(currentTeam[0].HC.name);
                if (changePrestigeEditText != null) changePrestigeEditText.setText(Integer.toString(currentTeam[0].teamPrestige));
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Button cancelBtn = dialog.findViewById(R.id.buttonCancelChangeName);
        if (cancelBtn != null) {
            cancelBtn.setText("BACK");
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    activity.updateCurrConference();
                    activity.resetTeamUI();
                    activity.updateHeaderBar();
                    dialog.dismiss();
                }
            });
        }

        Button okBtn = dialog.findViewById(R.id.buttonOkChangeName);
        if (okBtn != null) {
            okBtn.setText("UPDATE");
            okBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String newName = changeNameEditText.getText().toString().trim();
                    String newAbbr = changeAbbrEditText.getText().toString().trim().toUpperCase();
                    String newLocation = changeLocationText.getText().toString().trim().toUpperCase();
                    String newConf = changeConfEditText.getText().toString().trim();
                    String newHC = changeHCEditText.getText().toString().trim();
                    int newPrestige = Integer.parseInt(changePrestigeEditText.getText().toString().trim());

                    if (simLeague.isNameValid(newName) && simLeague.isAbbrValid(newAbbr) && 
                        simLeague.isNameValid(newConf) && activity.isNameValid(newHC) && 
                        simLeague.isInteger(newLocation)) {
                        
                        simLeague.changeAbbrHistoryRecords(currentTeam[0].abbr, newAbbr);

                        if (!newName.equals(currentTeam[0].name)) {
                            currentTeam[0].name = newName;
                            teamEditorList.clear();
                            for (int i = 0; i < currentConference[0].confTeams.size(); i++) {
                                teamEditorList.add(currentConference[0].confTeams.get(i).name);
                            }
                            editorAdaptorTeam.notifyDataSetChanged();
                        }

                        currentTeam[0].abbr = newAbbr;
                        currentTeam[0].location = Integer.parseInt(newLocation);
                        currentConference[0].confName = newConf;
                        currentTeam[0].HC.name = newHC;
                        currentTeam[0].teamPrestige = newPrestige;
                    }
                }
            });
        }
    }
}
