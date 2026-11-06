package antdroid.cfbcoach;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import simulation.GameCoachPlan;
import simulation.GamedayPlan;
import simulation.League;

/**
 * Interactive gameday coaching on Android. Desktop pauses the sim for a live
 * dialog at each checkpoint; Android simulates the week on the UI thread and
 * cannot block, so the coach sets a {@link GamedayPlan} before kickoff (fourth
 * downs, halftime if trailing / leading, crunch time if trailing / leading)
 * and the engine applies it at each checkpoint of every user game.
 */
final class GamedayPlanDialogController {

    private static final String PREFS = "cfhc_home";
    private static final String KEY_PLAN = "cfhc_gameday_plan";

    private GamedayPlanDialogController() {
    }

    /** Reads the saved plan and installs it on the league (or clears the hook when empty). */
    static void install(Context context, League league) {
        if (league == null) {
            return;
        }
        GamedayPlan plan = load(context);
        league.setGameCoachListener(plan.isEmpty() ? null : plan);
    }

    static GamedayPlan load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return GamedayPlan.decode(prefs.getString(KEY_PLAN, null));
    }

    private static void save(Context context, GamedayPlan plan) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_PLAN, plan.encode()).apply();
    }

    /** Five quick card steps; the plan is saved and installed after the last one. */
    static void show(final MainActivity activity, final League league) {
        final GamedayPlan current = load(activity);
        final GamedayPlan plan = new GamedayPlan();
        final String pill = activity.getString(R.string.gameday_pill);

        // Step 1: fourth downs (all game).
        List<ChoiceCardDialog.Choice> fourth = new ArrayList<>();
        fourth.add(choice(activity, R.string.gameday_fourth_aggressive, R.string.gameday_fourth_aggressive_body, current.fourthDownBias > 0));
        fourth.add(choice(activity, R.string.gameday_staff, R.string.gameday_fourth_staff_body, current.fourthDownBias == 0));
        fourth.add(choice(activity, R.string.gameday_fourth_cautious, R.string.gameday_fourth_cautious_body, current.fourthDownBias < 0));
        ChoiceCardDialog.show(activity, pill, activity.getString(R.string.gameday_fourth_title),
                activity.getString(R.string.gameday_fourth_subtitle), fourth, i -> {
                    plan.fourthDownBias = i == 0 ? 1 : (i == 2 ? -1 : 0);
                    halftimeStep(activity, league, current, plan, pill, true);
                });
    }

    private static final GameCoachPlan.HalftimeFocus[] HALFTIME = {
            GameCoachPlan.HalftimeFocus.AGGRESSIVE,
            GameCoachPlan.HalftimeFocus.CONSERVATIVE,
            GameCoachPlan.HalftimeFocus.NONE
    };

    private static void halftimeStep(MainActivity activity, League league, GamedayPlan current,
                                     GamedayPlan plan, String pill, boolean trailing) {
        GameCoachPlan.HalftimeFocus was = trailing ? current.halftimeIfTrailing : current.halftimeIfLeading;
        List<ChoiceCardDialog.Choice> choices = new ArrayList<>();
        choices.add(choice(activity, R.string.gameday_half_attack, R.string.gameday_half_attack_body, was == HALFTIME[0]));
        choices.add(choice(activity, R.string.gameday_half_control, R.string.gameday_half_control_body, was == HALFTIME[1]));
        choices.add(choice(activity, R.string.gameday_staff, R.string.gameday_half_staff_body,
                was != HALFTIME[0] && was != HALFTIME[1]));
        ChoiceCardDialog.show(activity, pill,
                activity.getString(trailing ? R.string.gameday_half_trailing_title : R.string.gameday_half_leading_title),
                activity.getString(R.string.gameday_half_subtitle), choices, i -> {
                    if (trailing) {
                        plan.halftimeIfTrailing = HALFTIME[i];
                        halftimeStep(activity, league, current, plan, pill, false);
                    } else {
                        plan.halftimeIfLeading = HALFTIME[i];
                        crunchStep(activity, league, current, plan, pill, true);
                    }
                });
    }

    private static void crunchStep(MainActivity activity, League league, GamedayPlan current,
                                   GamedayPlan plan, String pill, boolean trailing) {
        final GameCoachPlan.FourthDownCall[] calls = trailing
                ? new GameCoachPlan.FourthDownCall[]{GameCoachPlan.FourthDownCall.GO_FOR_IT,
                        GameCoachPlan.FourthDownCall.FIELD_GOAL, GameCoachPlan.FourthDownCall.AUTO}
                : new GameCoachPlan.FourthDownCall[]{GameCoachPlan.FourthDownCall.PUNT,
                        GameCoachPlan.FourthDownCall.GO_FOR_IT, GameCoachPlan.FourthDownCall.AUTO};
        GameCoachPlan.FourthDownCall was = trailing ? current.crunchIfTrailing : current.crunchIfLeading;
        List<ChoiceCardDialog.Choice> choices = new ArrayList<>();
        if (trailing) {
            choices.add(choice(activity, R.string.gameday_crunch_go, R.string.gameday_crunch_go_trailing_body, was == calls[0]));
            choices.add(choice(activity, R.string.gameday_crunch_fg, R.string.gameday_crunch_fg_body, was == calls[1]));
        } else {
            choices.add(choice(activity, R.string.gameday_crunch_punt, R.string.gameday_crunch_punt_body, was == calls[0]));
            choices.add(choice(activity, R.string.gameday_crunch_go, R.string.gameday_crunch_go_leading_body, was == calls[1]));
        }
        choices.add(choice(activity, R.string.gameday_staff, R.string.gameday_crunch_staff_body, was == calls[2]));
        ChoiceCardDialog.show(activity, pill,
                activity.getString(trailing ? R.string.gameday_crunch_trailing_title : R.string.gameday_crunch_leading_title),
                activity.getString(R.string.gameday_crunch_subtitle), choices, i -> {
                    if (trailing) {
                        plan.crunchIfTrailing = calls[i];
                        crunchStep(activity, league, current, plan, pill, false);
                    } else {
                        plan.crunchIfLeading = calls[i];
                        save(activity, plan);
                        install(activity, league);
                        PlatformUiHelper.snack(activity, activity.getString(plan.isEmpty()
                                ? R.string.gameday_saved_off : R.string.gameday_saved));
                    }
                });
    }

    private static ChoiceCardDialog.Choice choice(Context context, int title, int body, boolean current) {
        return new ChoiceCardDialog.Choice(context.getString(title),
                current ? context.getString(R.string.gameday_current) : null,
                context.getString(body));
    }
}
