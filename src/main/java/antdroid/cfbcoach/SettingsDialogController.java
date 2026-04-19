package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import simulation.League;

/**
 * Controller for the Game Settings dialog.
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
        String title = isNewGameSetup ? (simLeague.getYear() + " Head Coach Career Setup") : "Game Settings";
        builder.setTitle(title)
                .setView(activity.getLayoutInflater().inflate(R.layout.settings_menu, null));
        final AlertDialog dialog = builder.create(); dialog.setCancelable(false);
        PlatformUiHelper.showImmersive(dialog);

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
                    simLeague.showPotential = checkboxShowPotential.isChecked();
                    simLeague.fullGameLog = checkboxGameLog.isChecked();
                    simLeague.careerMode = checkboxCareerMode.isChecked();
                    simLeague.neverRetire = checkboxNeverRetire.isChecked();
                    simLeague.enableUnivProRel = checkboxProRelegation.isChecked();
                    simLeague.confRealignment = checkboxRealignment.isChecked();
                    simLeague.advancedRealignment = checkboxAdvRealignment.isChecked();
                    simLeague.expPlayoffs = checkboxPlayoffs.isChecked();
                    simLeague.enableTV = checkboxTV.isChecked();
                    
                    if (isNewGameSetup) {
                        if (simLeague.enableUnivProRel) {
                            simLeague.confRealignment = false;
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
