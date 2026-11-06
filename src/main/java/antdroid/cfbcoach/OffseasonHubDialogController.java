package antdroid.cfbcoach;

import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import simulation.League;
import simulation.SeasonFlowOrder;

/**
 * Offseason Hub (desktop parity with {@code desktop.OffseasonHubDialog}): one
 * checklist for the ten offseason steps instead of an unlabeled chain of
 * Play Week taps. "Advance One Step" runs the same week advance as Play Week,
 * so decision steps still open their own dialogs on top of the hub.
 */
final class OffseasonHubDialogController {

    private OffseasonHubDialogController() {
    }

    static boolean isOffseason(League league) {
        return league != null
                && SeasonFlowOrder.offseasonStepIndex(league.currentWeek, league.regSeasonWeeks) >= 0;
    }

    static void show(final MainActivity activity, final League league, final Runnable advanceOneStep) {
        final AlertDialog dialog = Dialogs.builder(activity)
                .setTitle(activity.getString(R.string.offseason_hub_title, league.getYear()))
                .setMessage(checklist(activity, league))
                .setPositiveButton(R.string.offseason_hub_advance, null) // wired below to keep the hub open
                .setNegativeButton(R.string.offseason_hub_close, null)
                .create();
        dialog.setOnShowListener(d -> {
            final Button advance = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            advance.setEnabled(isOffseason(league));
            advance.setOnClickListener(v -> {
                advanceOneStep.run();
                if (activity.isFinishing()) {
                    // Recruiting hands off to another screen.
                    dialog.dismiss();
                    return;
                }
                dialog.setMessage(checklist(activity, league));
                advance.setEnabled(isOffseason(league));
            });
        });
        PlatformUiHelper.showImmersive(dialog);
    }

    private static String checklist(MainActivity activity, League league) {
        int index = SeasonFlowOrder.offseasonStepIndex(league.currentWeek, league.regSeasonWeeks);
        String[] names = SeasonFlowOrder.offseasonSteps();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i < index) {
                sb.append("✓  ").append(names[i]);
            } else if (i == index) {
                sb.append("▶  ").append(names[i]).append("  — ")
                        .append(activity.getString(R.string.offseason_hub_next_up));
            } else {
                sb.append("     ").append(names[i]);
            }
            sb.append('\n');
        }
        if (index < 0) {
            sb.append('\n').append(activity.getString(R.string.offseason_hub_done));
        }
        return sb.toString().trim();
    }
}
