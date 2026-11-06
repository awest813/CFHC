package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import simulation.League;
import simulation.Team;
import ui.MockDraft;
import ui.StatsRowAdapter;

/**
 * MainActivity-side recruiting dialogs (pre-board handoff and class rankings).
 * Distinct from {@code antdroid.cfbcoach.recruiting.RecruitingDialogController},
 * which serves the in-board RecruitingActivity flows.
 */
public final class RecruitingOptionsDialogController {
    private RecruitingOptionsDialogController() {}

    public static void showBeginRecruiting(MainActivity activity, Team userTeam, League simLeague,
                                            String playerInfo, Runnable onRecruiting,
                                            Runnable onSave) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
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
                activity.getString(R.string.dlg_recruit_launch_subtitle));

        String[] spinnerSelection = {
                activity.getString(R.string.dlg_draft_players_leaving),
                activity.getString(R.string.dlg_draft_board)
        };
        Spinner spinner = dialog.findViewById(R.id.spinnerTeamRankings);
        PlatformUiHelper.avoidSpinnerDropdownFocus(spinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, spinnerSelection);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        final androidx.recyclerview.widget.RecyclerView playerList = dialog.findViewById(R.id.listViewTeamRankings);
        final StatsRowAdapter playerStatsAdapter =
                new StatsRowAdapter(activity, userTeam.getGradPlayersList());
        String[] draftBoard = simLeague.getMockDraftPlayersList();
        if (draftBoard == null || draftBoard.length == 0) {
            draftBoard = new String[]{activity.getString(R.string.dlg_draft_empty)};
        }
        final MockDraft mockDraftAdapter =
                new MockDraft(activity, draftBoard, userTeam.getName());
        ui.RecyclerLists.setAdapter(playerList, playerStatsAdapter);

        spinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               android.view.View view, int position, long id) {
                        if (position == 0) {
                            ui.RecyclerLists.setAdapter(playerList, playerStatsAdapter);
                        } else {
                            ui.RecyclerLists.setAdapter(playerList, mockDraftAdapter);
                        }
                    }
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    public static void showRecruitingClassRankings(MainActivity activity, League simLeague,
                                                    Team userTeam, Runnable onContinue) {
        AlertDialog.Builder builder = Dialogs.builder(activity);
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
                activity.getString(R.string.dlg_recruit_class_subtitle));

        androidx.recyclerview.widget.RecyclerView rankingsList = dialog.findViewById(R.id.listViewDialog);
        ui.RecyclerLists.setAdapter(rankingsList, new ui.TeamRankingsList(activity,
                simLeague.getTeamRankingsStr(17), userTeam.getName()));
    }

    /** Mock draft board from the drawer, any time of year (desktop parity: View → Mock Draft). */
    public static void showMockDraft(MainActivity activity, League simLeague, Team userTeam) {
        AlertDialog dialog = Dialogs.builder(activity)
                .setPositiveButton("OK", null)
                .setView(activity.getLayoutInflater().inflate(R.layout.simple_list_dialog, null, false))
                .create();
        activity.showImmersive(dialog);
        PlatformUiHelper.bindSimpleListDialogShell(dialog, activity.getString(R.string.dlg_draft_board),
                activity.getString(R.string.dlg_mock_draft_subtitle));
        String[] board;
        try {
            board = simLeague.getMockDraftPlayersList();
        } catch (RuntimeException ex) {
            simulation.PlatformLog.e("MockDraft", "Error building mock draft", ex);
            board = null;
        }
        if (board == null || board.length == 0) {
            board = new String[]{activity.getString(R.string.dlg_draft_empty)};
        }
        androidx.recyclerview.widget.RecyclerView list = dialog.findViewById(R.id.listViewDialog);
        ui.RecyclerLists.setAdapter(list, new MockDraft(activity, board, userTeam != null ? userTeam.getName() : ""));
    }
}

