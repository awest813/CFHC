package simulation;

/**
 * Coaching decisions made before kickoff, answered automatically at each
 * {@link GameCoachListener.Checkpoint}. Shells that cannot block mid-sim for a
 * live dialog (Android runs the week on the UI thread) install this as the
 * league's coach listener: the player sets intents up front ("if we trail at
 * the half, attack"), and the game applies them at the checkpoint.
 *
 * <p>Ties at halftime or entering the fourth quarter use the staff's own logic
 * (no override), so only a real lead or deficit triggers a plan branch.
 */
public final class GamedayPlan implements GameCoachListener {

    /** Fourth-down aggression for the whole game, -2 (cautious) .. +2 (aggressive). */
    public int fourthDownBias;
    public GameCoachPlan.HalftimeFocus halftimeIfTrailing = GameCoachPlan.HalftimeFocus.NONE;
    public GameCoachPlan.HalftimeFocus halftimeIfLeading = GameCoachPlan.HalftimeFocus.NONE;
    public GameCoachPlan.FourthDownCall crunchIfTrailing = GameCoachPlan.FourthDownCall.AUTO;
    public GameCoachPlan.FourthDownCall crunchIfLeading = GameCoachPlan.FourthDownCall.AUTO;

    /** True when every choice defers to the staff, i.e. installing the plan changes nothing. */
    public boolean isEmpty() {
        return fourthDownBias == 0
                && halftimeIfTrailing == GameCoachPlan.HalftimeFocus.NONE
                && halftimeIfLeading == GameCoachPlan.HalftimeFocus.NONE
                && crunchIfTrailing == GameCoachPlan.FourthDownCall.AUTO
                && crunchIfLeading == GameCoachPlan.FourthDownCall.AUTO;
    }

    @Override
    public GameCoachPlan decide(Game game, Checkpoint checkpoint) {
        GameCoachPlan plan = new GameCoachPlan();
        int margin = userMargin(game);
        switch (checkpoint) {
            case PREGAME -> plan.fourthDownBias = Math.max(-2, Math.min(2, fourthDownBias));
            case HALFTIME -> {
                if (margin < 0) plan.halftimeFocus = halftimeIfTrailing;
                else if (margin > 0) plan.halftimeFocus = halftimeIfLeading;
            }
            case CRUNCH_TIME -> {
                if (margin < 0) plan.crunchFourthDownCall = crunchIfTrailing;
                else if (margin > 0) plan.crunchFourthDownCall = crunchIfLeading;
            }
        }
        // null = defer to the CPU logic, exactly as with no listener.
        return plan.isEmpty() ? null : plan;
    }

    /** User team's score minus the opponent's; 0 when neither side is the user's. */
    static int userMargin(Game game) {
        if (game.homeTeam != null && game.homeTeam.isUserControlled()) {
            return game.homeScore - game.awayScore;
        }
        if (game.awayTeam != null && game.awayTeam.isUserControlled()) {
            return game.awayScore - game.homeScore;
        }
        return 0;
    }

    /** Compact form for a preferences store: {@code bias,htTrail,htLead,crTrail,crLead}. */
    public String encode() {
        return fourthDownBias + "," + halftimeIfTrailing.name() + "," + halftimeIfLeading.name()
                + "," + crunchIfTrailing.name() + "," + crunchIfLeading.name();
    }

    /** Parses {@link #encode()} output; anything unreadable yields an empty plan. */
    public static GamedayPlan decode(String text) {
        GamedayPlan plan = new GamedayPlan();
        if (text == null) {
            return plan;
        }
        String[] f = text.split(",", -1);
        if (f.length != 5) {
            return plan;
        }
        try {
            plan.fourthDownBias = Math.max(-2, Math.min(2, Integer.parseInt(f[0].trim())));
            plan.halftimeIfTrailing = GameCoachPlan.HalftimeFocus.valueOf(f[1].trim());
            plan.halftimeIfLeading = GameCoachPlan.HalftimeFocus.valueOf(f[2].trim());
            plan.crunchIfTrailing = GameCoachPlan.FourthDownCall.valueOf(f[3].trim());
            plan.crunchIfLeading = GameCoachPlan.FourthDownCall.valueOf(f[4].trim());
        } catch (IllegalArgumentException ex) {
            return new GamedayPlan();
        }
        return plan;
    }
}
