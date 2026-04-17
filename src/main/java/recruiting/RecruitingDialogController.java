package recruiting;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import antdroid.cfbcoach.GameNavigation;
import antdroid.cfbcoach.LeagueLaunchCoordinator;
import antdroid.cfbcoach.PlatformUiHelper;
import antdroid.cfbcoach.R;
import simulation.RosterRules;

/**
 * Controller for dialogs in the RecruitingActivity.
 */
public final class RecruitingDialogController {

    private RecruitingDialogController() {
    }

    /**
     * Show display options dialog (Expand All, Sort, etc.)
     */
    public static void showDisplayOptions(final RecruitingActivity activity, final boolean autoFilter, final List<RecruitingPlayerRecord> players, final ExpandableListView recruitList, final RecruitingActivity.ExpandableListAdapterRecruiting expListAdapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("DISPLAY OPTIONS");
        
        String filter = autoFilter ? "Disable Auto-Remove Unaffordable Players" : "Enable Auto-Remove Unaffordable Players";
        final String[] sels = {"Expand All", "Collapse All", "Sort by Grade", "Sort by Cost", filter};
        
        builder.setItems(sels, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    for (int i = 0; i < players.size(); ++i) {
                        recruitList.expandGroup(i, false);
                    }
                } else if (item == 1) {
                    for (int i = 0; i < players.size(); ++i) {
                        recruitList.collapseGroup(i);
                    }
                } else if (item == 2) {
                    activity.sortByGrade();
                    expListAdapter.notifyDataSetChanged();
                } else if (item == 3) {
                    activity.sortByCost();
                    expListAdapter.notifyDataSetChanged();
                } else if (item == 4) {
                    activity.toggleAutoFilter();
                }
                dialog.dismiss();
            }
        });
        
        AlertDialog alert = builder.create();
        PlatformUiHelper.showImmersive(alert);
    }

    /**
     * Show roster overview dialog
     */
    public static void showRosterDialog(final RecruitingActivity activity, String teamName, String rosterStr, int totalPlayers) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(rosterStr)
                .setTitle(teamName + " Roster | Team Size: " + totalPlayers)
                .setPositiveButton("OK", null);
        
        AlertDialog dialog = builder.create();
        PlatformUiHelper.showImmersive(dialog);
        
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) {
            msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    /**
     * Show recruiting confirmation dialog
     */
    public static void showRecruitConfirmDialog(final RecruitingActivity activity, final RecruitingSessionData sessionData, final RecruitingPlayerRecord recruit, final int groupPosition, final ExpandableListView recruitList, final RecruitingActivity.ExpandableListAdapterRecruiting expListAdapter, boolean showPopUp) {
        int moneyNeeded = recruit.cost();
        
        if (sessionData.recruitingBudget < moneyNeeded) {
            collapseForRefresh(recruitList, groupPosition, playersCount(sessionData));
            Toast.makeText(activity, "Not enough money!", Toast.LENGTH_SHORT).show();
            recruitList.expandGroup(groupPosition);
            expListAdapter.notifyDataSetChanged();
            return;
        }

        if (!showPopUp) {
            performRecruit(activity, sessionData, recruit, groupPosition, recruitList, expListAdapter);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Confirm Recruiting");
        builder.setMessage(RecruitingPresentation.buildRecruitConfirmMessage(sessionData, RosterRules.MAX_PLAYERS, recruit.raw()));
        
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                performRecruit(activity, sessionData, recruit, groupPosition, recruitList, expListAdapter);
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("Yes, Don't Show", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                activity.setShowPopUp(false);
                performRecruit(activity, sessionData, recruit, groupPosition, recruitList, expListAdapter);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                collapseForRefresh(recruitList, groupPosition, playersCount(sessionData));
                recruitList.expandGroup(groupPosition);
                expListAdapter.notifyDataSetChanged();
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        PlatformUiHelper.showImmersive(dialog);
        
        TextView msgTxt = dialog.findViewById(android.R.id.message);
        if (msgTxt != null) {
            msgTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        }
    }

    /**
     * Show exit confirmation dialog
     */
    public static void showExitConfirmDialog(final RecruitingActivity activity, List<String> positions, final RecruitingController controller) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(RecruitingPresentation.buildExitConfirmMessage(positions))
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        controller.finishRecruiting();
                    }
                })
                .setNegativeButton("No", null);
        
        AlertDialog dialog = builder.create();
        PlatformUiHelper.showImmersive(dialog);
    }

    private static void performRecruit(RecruitingActivity activity, RecruitingSessionData sessionData, RecruitingPlayerRecord recruit, int groupPosition, ExpandableListView recruitList, RecruitingActivity.ExpandableListAdapterRecruiting expListAdapter) {
        collapseForRefresh(recruitList, groupPosition, playersCount(sessionData));
        activity.recruitPlayer(recruit);
        expListAdapter.notifyDataSetChanged();
    }


    private static void collapseForRefresh(ExpandableListView recruitList, int groupPosition, int totalCount) {
        recruitList.collapseGroup(groupPosition);
        for (int i = groupPosition + 1; i < totalCount; ++i) {
            recruitList.collapseGroup(i);
        }
    }

    private static int playersCount(RecruitingSessionData sessionData) {
        // This is a bit hacky because we don't have direct access to 'players' list here without passing it.
        // But we can approximate or pass it if needed.
        return 1000; // Large enough to cover all groups for now, or we can pass the actual list size.
    }
}
