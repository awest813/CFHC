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
        builder.setTitle("Coach Program & NIL");
        builder.setView(activity.getLayoutInflater().inflate(R.layout.coach_program_dialog, null));
        final AlertDialog dialog = builder.create();
        PlatformUiHelper.showImmersive(dialog);

        TextView summary = dialog.findViewById(R.id.textCoachProgramSummary);
        TextView xpView = dialog.findViewById(R.id.textCoachSkillXp);
        Spinner branchSpinner = dialog.findViewById(R.id.spinnerCoachSkillBranch);
        Button upgrade = dialog.findViewById(R.id.buttonCoachSkillUpgrade);
        Button close = dialog.findViewById(R.id.buttonCoachProgramClose);

        Runnable refresh = () -> {
            if (summary != null) {
                summary.setText(buildSummary(userTeam, hc));
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
            int r = CoachSkills.getRank(hc.coachSkillRanksBits, b);
            branchLabels[b] = CoachSkills.branchTitle(b) + " [" + r + "/3]";
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
                    Toast.makeText(activity, "Branch maxed.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hc.coachSkillXp < cost) {
                    Toast.makeText(activity, "Need " + cost + " XP (have " + hc.coachSkillXp + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (hc.tryPurchaseCoachSkillRank(b)) {
                    Toast.makeText(activity, "Upgraded " + CoachSkills.branchTitle(b), Toast.LENGTH_SHORT).show();
                    refresh.run();
                    if (branchSpinner != null) {
                        for (int i = 0; i < CoachSkills.BRANCH_COUNT; i++) {
                            int r = CoachSkills.getRank(hc.coachSkillRanksBits, i);
                            branchLabels[i] = CoachSkills.branchTitle(i) + " [" + r + "/3]";
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

    private static String buildSummary(Team t, Staff hc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Training facilities: L").append(t.getTeamFacilities()).append("\n");
        sb.append("NIL collective: Tier ").append(t.nilCollectiveLevel).append("\n\n");
        for (int b = 0; b < CoachSkills.BRANCH_COUNT; b++) {
            int r = CoachSkills.getRank(hc.coachSkillRanksBits, b);
            sb.append(CoachSkills.branchTitle(b)).append(": ").append(r).append("/3\n");
            sb.append(CoachSkills.branchBlurb(b)).append("\n\n");
        }
        return sb.toString();
    }
}
