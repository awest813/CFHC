package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import staff.Staff;
import simulation.CoachSkills;
import simulation.Team;

/**
 * Android UI for coach skill trees and NIL / facility summary.
 */
public final class CoachProgramDialogController {

    private CoachProgramDialogController() {
    }

    public static void show(final MainActivity activity, final Team userTeam) {
        if (userTeam == null || userTeam.getHeadCoach() == null) {
            return;
        }
        final Staff hc = userTeam.getHeadCoach();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(CoachSkills.PROGRAM_DIALOG_TITLE);
        builder.setView(activity.getLayoutInflater().inflate(R.layout.coach_program_dialog, null, false));
        final AlertDialog dialog = builder.create();
        PlatformUiHelper.showImmersive(dialog);

        TextView titleInView = dialog.findViewById(R.id.textCoachProgramTitle);
        if (titleInView != null) {
            titleInView.setText(CoachSkills.PROGRAM_DIALOG_TITLE);
        }
        TextView summary = dialog.findViewById(R.id.textCoachProgramSummary);
        TextView xpView = dialog.findViewById(R.id.textCoachSkillXp);
        Spinner branchSpinner = dialog.findViewById(R.id.spinnerCoachSkillBranch);
        Button upgrade = dialog.findViewById(R.id.buttonCoachSkillUpgrade);
        Button close = dialog.findViewById(R.id.buttonCoachProgramClose);
        TextView hintView = dialog.findViewById(R.id.textCoachProgramHint);

        if (hintView != null) {
            hintView.setText(CoachSkills.PROGRAM_DIALOG_FOOTER_HINT);
        }
        if (upgrade != null) {
            upgrade.setText(CoachSkills.UPGRADE_BRANCH_BUTTON_LABEL);
        }

        Runnable refresh = () -> {
            if (summary != null) {
                summary.setText(CoachSkills.buildProgramSummary(userTeam, hc));
            }
            if (xpView != null) {
                xpView.setText("Skill XP: " + hc.coachSkillXp);
            }
            if (branchSpinner != null) {
                branchSpinner.invalidate();
            }
        };

        String[] branchLabels = new String[CoachSkills.BRANCH_COUNT];
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            branchLabels[b] = CoachSkills.branchPickerLabel(hc.coachSkillRanksBits, b);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, branchLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (branchSpinner != null) {
            branchSpinner.setAdapter(adapter);
        }

        refresh.run();

        if (upgrade != null) {
            upgrade.setOnClickListener(v -> {
                int b = branchSpinner != null ? branchSpinner.getSelectedItemPosition() : 0;
                int cur = CoachSkills.getRank(hc.coachSkillRanksBits, b);
                int cost = CoachSkills.costForNextRank(cur);
                if (cur >= 3) {
                    Toast.makeText(activity, "This branch is maxed.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hc.coachSkillXp < cost) {
                    Toast.makeText(activity,
                            "Need " + cost + " XP (you have " + hc.coachSkillXp + "). XP builds each week you sim.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (hc.tryPurchaseCoachSkillRank(b)) {
                    Toast.makeText(activity, "Upgraded " + CoachSkills.branchTitle(b), Toast.LENGTH_SHORT).show();
                    refresh.run();
                    if (branchSpinner != null) {
                        for (int i = 0; i < CoachSkills.BRANCH_COUNT; i++) {
                            branchLabels[i] = CoachSkills.branchPickerLabel(hc.coachSkillRanksBits, i);
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        if (close != null) {
            close.setOnClickListener(v -> dialog.dismiss());
        }
    }
}
