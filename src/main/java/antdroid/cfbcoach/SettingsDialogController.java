package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import simulation.League;
import simulation.LeagueSettingsOptions;
import simulation.PracticeFocus;

/**
 * Controller for the League Settings dialog (parity with desktop {@code SettingsDialog}).
 */
public final class SettingsDialogController {

    private SettingsDialogController() {
    }

    public static void show(final MainActivity activity, final League simLeague) {
        show(activity, simLeague, false);
    }

    public static void showCareerSetup(final MainActivity activity, final League simLeague) {
        show(activity, simLeague, true);
    }

    private static void show(final MainActivity activity, final League simLeague, final boolean isNewGameSetup) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String title = isNewGameSetup ? (simLeague.getYear() + " Head Coach Career Setup")
                : activity.getString(R.string.league_settings_title);
        builder.setTitle(title)
                .setView(activity.getLayoutInflater().inflate(R.layout.settings_menu, null, false));
        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

        final TextView footerHint = dialog.findViewById(R.id.textSettingsFooterHint);
        if (footerHint != null) {
            footerHint.setVisibility(isNewGameSetup ? View.GONE : View.VISIBLE);
        }

        final CheckBox checkboxShowPotential = dialog.findViewById(R.id.checkboxShowPotential);
        checkboxShowPotential.setChecked(simLeague.showPotential);

        final CheckBox checkboxGameLog = dialog.findViewById(R.id.checkboxShowFullGameLog);
        checkboxGameLog.setChecked(simLeague.fullGameLog);

        final CheckBox checkboxCareerMode = dialog.findViewById(R.id.checkboxCareerMode);
        checkboxCareerMode.setChecked(simLeague.isCareerMode());

        final CheckBox checkboxNeverRetire = dialog.findViewById(R.id.checkboxNeverRetire);
        checkboxNeverRetire.setChecked(simLeague.neverRetire);

        final CheckBox checkboxTV = dialog.findViewById(R.id.checkboxTV);
        checkboxTV.setChecked(simLeague.enableTV);

        final CheckBox checkboxPlayoffs = dialog.findViewById(R.id.checkboxPlayoffs);
        final TextView textPlayoffs = dialog.findViewById(R.id.textPlayoffs);
        if(isNewGameSetup || simLeague.currentWeek < simLeague.regSeasonWeeks) {
            checkboxPlayoffs.setChecked(simLeague.expPlayoffs);
        } else {
            if (textPlayoffs != null) textPlayoffs.setVisibility(View.INVISIBLE);
            checkboxPlayoffs.setVisibility(View.INVISIBLE);
        }

        final CheckBox checkboxRealignment = dialog.findViewById(R.id.checkboxConfRealignment);
        final TextView textRealignment = dialog.findViewById(R.id.textConfRealignment);
        if(!isNewGameSetup && simLeague.enableUnivProRel) {
            if (textRealignment != null) textRealignment.setVisibility(View.INVISIBLE);
            checkboxRealignment.setVisibility(View.INVISIBLE);
        }
        checkboxRealignment.setChecked(simLeague.confRealignment);

        final CheckBox checkboxAdvRealignment = dialog.findViewById(R.id.checkboxAdvConfRealignment);
        final TextView textAdvRealignment = dialog.findViewById(R.id.textAdvConfRealignment);
        if(!isNewGameSetup && simLeague.enableUnivProRel) {
            if (textAdvRealignment != null) textAdvRealignment.setVisibility(View.INVISIBLE);
            checkboxAdvRealignment.setVisibility(View.INVISIBLE);
        }
        checkboxAdvRealignment.setChecked(simLeague.advancedRealignment);

        final CheckBox checkboxProRelegation = dialog.findViewById(R.id.checkboxProRelegation);
        checkboxProRelegation.setChecked(simLeague.enableUnivProRel);
        
        final TextView textProRel = dialog.findViewById(R.id.textEnableProRel);

        if (isNewGameSetup) {
            final TextView textSettingsLead = dialog.findViewById(R.id.textSettingsLead);
            final TextView textSettingsSublead = dialog.findViewById(R.id.textSettingsSublead);
            if (textSettingsLead != null) textSettingsLead.setText(simLeague.getYear() + " Head Coach Career");
            if (textSettingsSublead != null) textSettingsSublead.setText("Set the tone for your first season, choose whether your coach is on the hot seat, and decide if this universe opens with the modern 12-team playoff.");
        } else {
            checkboxProRelegation.setVisibility(View.INVISIBLE);
            if (textProRel != null) textProRel.setVisibility(View.INVISIBLE);
        }

        final View sectionPracticeFocus = dialog.findViewById(R.id.sectionPracticeFocus);
        final Spinner spinnerPracticeFocus = dialog.findViewById(R.id.spinnerPracticeFocus);
        final TextView textPracticeFocusDesc = dialog.findViewById(R.id.textPracticeFocusDesc);
        if (!isNewGameSetup && simLeague.userTeam != null && sectionPracticeFocus != null
                && spinnerPracticeFocus != null && textPracticeFocusDesc != null) {
            sectionPracticeFocus.setVisibility(View.VISIBLE);
            PracticeFocus[] values = PracticeFocus.values();
            String[] labels = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                labels[i] = values[i].displayName();
            }
            ArrayAdapter<String> pfAdapter = new ArrayAdapter<>(activity,
                    android.R.layout.simple_spinner_item, labels);
            pfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPracticeFocus.setAdapter(pfAdapter);
            PracticeFocus currentPf = simLeague.userTeam.practiceFocus != null
                    ? simLeague.userTeam.practiceFocus
                    : PracticeFocus.BALANCED;
            spinnerPracticeFocus.setSelection(currentPf.ordinal());
            textPracticeFocusDesc.setText(currentPf.shortDescription());
            spinnerPracticeFocus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    textPracticeFocusDesc.setText(PracticeFocus.values()[position].shortDescription());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else if (sectionPracticeFocus != null) {
            sectionPracticeFocus.setVisibility(View.GONE);
        }

        checkboxAdvRealignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkboxAdvRealignment.isChecked()) {
                    checkboxProRelegation.setChecked(false);
                    checkboxRealignment.setChecked(true);
                }
                if(simLeague.regSeasonWeeks > 13) checkboxAdvRealignment.setChecked(true);
            }
        });

        checkboxRealignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkboxRealignment.isChecked()) {
                    checkboxProRelegation.setChecked(false);
                }
            }
        });

        checkboxProRelegation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkboxProRelegation.isChecked()) {
                    checkboxRealignment.setChecked(false);
                    checkboxAdvRealignment.setChecked(false);
                }
            }
        });

        Button cancelButton = dialog.findViewById(R.id.buttonCancelSettings);
        Button okButton = dialog.findViewById(R.id.buttonOkSettings);
        Button changeTeamsButton = dialog.findViewById(R.id.buttonChangeTeams);
        Button gameEditorButton = dialog.findViewById(R.id.buttonGameEditor);
        Button fixBowlButton = dialog.findViewById(R.id.buttonFixBowls);
        Button fixProRel = dialog.findViewById(R.id.buttonProRel);

        if (isNewGameSetup) {
            if (cancelButton != null) cancelButton.setVisibility(View.INVISIBLE);
            if (changeTeamsButton != null) changeTeamsButton.setVisibility(View.INVISIBLE);
            if (gameEditorButton != null) gameEditorButton.setVisibility(View.INVISIBLE);
            if (fixBowlButton != null) fixBowlButton.setVisibility(View.INVISIBLE);
            if (fixProRel != null) fixProRel.setVisibility(View.INVISIBLE);
        } else {
            if (activity.userTeam.getHeadCoach().age >= 55 && !simLeague.neverRetire) {
                if (changeTeamsButton != null) changeTeamsButton.setText("RETIRE");
            }
            if (simLeague.currentWeek < simLeague.regSeasonWeeks + 6) {
                if (changeTeamsButton != null) changeTeamsButton.setVisibility(View.INVISIBLE);
            }
            if (simLeague.currentWeek > 0 && fixProRel != null) fixProRel.setVisibility(View.INVISIBLE);
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        if (gameEditorButton != null) {
            gameEditorButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    LeagueEditorDialogController.show(activity, simLeague);
                }
            });
        }

        if (fixBowlButton != null) {
            fixBowlButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showFixBowlConfirmation(activity);
                }
            });
        }

        if (fixProRel != null) {
            fixProRel.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showProRelConfirmation(activity, simLeague);
                }
            });
        }

        if (okButton != null) {
            okButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    boolean proRelWasEnabled = simLeague.enableUnivProRel;
                    boolean proRelRequested = checkboxProRelegation.isChecked();

                    LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(simLeague);
                    options.showPotential = checkboxShowPotential.isChecked();
                    options.fullGameLog = checkboxGameLog.isChecked();
                    options.careerMode = checkboxCareerMode.isChecked();
                    options.neverRetire = checkboxNeverRetire.isChecked();
                    options.universalProRel = proRelRequested;
                    options.conferenceRealignment = checkboxRealignment.isChecked();
                    options.advancedRealignment = checkboxAdvRealignment.isChecked();
                    options.expandedPlayoffs = checkboxPlayoffs.isChecked();
                    options.enableTv = checkboxTV.isChecked();

                    boolean allowPlayoffChange = isNewGameSetup || simLeague.currentWeek < simLeague.regSeasonWeeks;
                    options.applyTo(simLeague, allowPlayoffChange, isNewGameSetup, false);

                    if (!isNewGameSetup && simLeague.userTeam != null && spinnerPracticeFocus != null) {
                        int pos = spinnerPracticeFocus.getSelectedItemPosition();
                        PracticeFocus[] vals = PracticeFocus.values();
                        if (pos >= 0 && pos < vals.length) {
                            simLeague.userTeam.practiceFocus = vals[pos];
                        }
                    }

                    if (isNewGameSetup) {
                        if (proRelRequested && !proRelWasEnabled) {
                            activity.universalProRelAction();
                        }
                        activity.selectTeam();
                    }
                    dialog.dismiss();
                }
            });
        }
    }

    private static void showFixBowlConfirmation(final MainActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to restore bowl names to game default names?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.fixBowlNames();
                        Toast.makeText(activity, "Bowl Names Replaced!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private static void showProRelConfirmation(final MainActivity activity, final League simLeague) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Confirmation")
                .setMessage("Are you sure you want to convert to Promotion-Relegation Mode?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        simLeague.enableUnivProRel = true;
                        simLeague.convertUnivProRel();
                        simLeague.confRealignment = false;
                        simLeague.advancedRealignment = false;
                        Toast.makeText(activity, "Conversion Complete!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        activity.updateSpinners();
                        activity.resetTeamUI();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }
}
