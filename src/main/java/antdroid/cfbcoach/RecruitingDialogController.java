package antdroid.cfbcoach;

import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import simulation.League;
import simulation.Team;
import ui.MockDraft;
import ui.StatsRowAdapter;

public final class RecruitingDialogController {
    private RecruitingDialogController() {}

    public static void showBeginRecruiting(MainActivity activity, Team userTeam, League simLeague,
                                            String playerInfo, Runnable onRecruiting,
                                            Runnable onSave) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(userTeam.getAbbr() + " Players Leaving")
                .setPositiveButton("Recruiting", (dialog, which) -> {
                    if (onRecruiting != null) onRecruiting.run();
                })
                .setNegativeButton("Back", (dialog, which) -> {})
                .setNeutralButton("Save", (dialog, which) -> {
                    if (onSave != null) onSave.run();
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.team_rankings_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindRankingsDialogShell(dialog, "Recruiting Launch",
                "Check who is leaving your roster or headed to the draft before you enter recruiting season.");

        String[] spinnerSelection = {"Players Leaving", "Pro Mock Draft"};
        Spinner spinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, spinnerSelection);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        final ListView playerList = dialog.findViewById(R.id.listViewTeamRankings);
        final StatsRowAdapter playerStatsAdapter =
                new StatsRowAdapter(activity, userTeam.getGradPlayersList());
        final MockDraft mockDraftAdapter =
                new MockDraft(activity, simLeague.getMockDraftPlayersList(), userTeam.getName());
        playerList.setAdapter(playerStatsAdapter);

        spinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               android.view.View view, int position, long id) {
                        if (position == 0) {
                            playerList.setAdapter(playerStatsAdapter);
                        } else {
                            playerList.setAdapter(mockDraftAdapter);
                        }
                    }
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    public static void showRecruitingClassRankings(MainActivity activity, League simLeague,
                                                    Team userTeam, Runnable onContinue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle("Recruiting Class Rankings")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (onContinue != null) onContinue.run();
                })
                .setView(activity.getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false));
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, "Recruiting Class Rankings",
                "Measure your incoming class against the rest of the country before you move on to season goals.");

        ListView rankingsList = dialog.findViewById(R.id.listViewDialog);
        rankingsList.setAdapter(new ui.TeamRankingsList(activity,
                simLeague.getTeamRankingsStr(17), userTeam.getName()));
    }
}
