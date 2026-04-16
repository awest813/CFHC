package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import simulation.PlaybookDefense;
import simulation.PlaybookOffense;
import simulation.Team;

final class TeamStrategyDialogController {
    private TeamStrategyDialogController() {
    }

    static void show(MainActivity activity, Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Team Strategy")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss handled by the dialog button.
                    }
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_strategy_dialog, null));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        final PlaybookOffense[] tsOff = userTeam.getPlaybookOff();
        final PlaybookDefense[] tsDef = userTeam.getPlaybookDef();

        String[] stratOffSelection = new String[tsOff.length];
        for (int i = 0; i < tsOff.length; ++i) {
            stratOffSelection[i] = tsOff[i].getStratName();
        }

        String[] stratDefSelection = new String[tsDef.length];
        for (int i = 0; i < tsDef.length; ++i) {
            stratDefSelection[i] = tsDef[i].getStratName();
        }

        final TextView offStratDescription = dialog.findViewById(R.id.textOffenseStrategy);
        final TextView defStratDescription = dialog.findViewById(R.id.textDefenseStrategy);

        Spinner stratOffSelectionSpinner = dialog.findViewById(R.id.spinnerOffenseStrategy);
        PlatformUiHelper.avoidSpinnerDropdownFocus(stratOffSelectionSpinner);
        ArrayAdapter<String> stratOffSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, stratOffSelection);
        stratOffSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stratOffSelectionSpinner.setAdapter(stratOffSpinnerAdapter);
        stratOffSelectionSpinner.setSelection(userTeam.playbookOffNum);
        stratOffSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                offStratDescription.setText(tsOff[position].getStratDescription());
                userTeam.playbookOff = tsOff[position];
                userTeam.playbookOffNum = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op.
            }
        });

        Spinner stratDefSelectionSpinner = dialog.findViewById(R.id.spinnerDefenseStrategy);
        PlatformUiHelper.avoidSpinnerDropdownFocus(stratDefSelectionSpinner);
        ArrayAdapter<String> stratDefSpinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, stratDefSelection);
        stratDefSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stratDefSelectionSpinner.setAdapter(stratDefSpinnerAdapter);
        stratDefSelectionSpinner.setSelection(userTeam.playbookDefNum);
        stratDefSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                defStratDescription.setText(tsDef[position].getStratDescription());
                userTeam.playbookDef = tsDef[position];
                userTeam.playbookDefNum = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op.
            }
        });
    }
}
