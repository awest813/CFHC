package simulation;

public final class RosterRules {

    private RosterRules() {
    }

    public static final int STARTERS_QB = 1;
    public static final int STARTERS_RB = 2;
    public static final int STARTERS_WR = 3;
    public static final int STARTERS_TE = 1;
    public static final int STARTERS_OL = 5;
    public static final int STARTERS_K = 1;
    public static final int STARTERS_DL = 4;
    public static final int STARTERS_LB = 3;
    public static final int STARTERS_CB = 3;
    public static final int STARTERS_S = 2;

    public static final int SUB_QB = 0;
    public static final int SUB_RB = 1;
    public static final int SUB_WR = 2;
    public static final int SUB_TE = 1;
    public static final int SUB_OL = 2;
    public static final int SUB_K = 0;
    public static final int SUB_DL = 2;
    public static final int SUB_LB = 2;
    public static final int SUB_CB = 1;
    public static final int SUB_S = 1;

    public static final int MIN_QBS = 3;
    public static final int MIN_RBS = 5;
    public static final int MIN_WRS = 8;
    public static final int MIN_TES = 3;
    public static final int MIN_OLS = 11;
    public static final int MIN_KS = 2;
    public static final int MIN_DLS = 9;
    public static final int MIN_LBS = 7;
    public static final int MIN_CBS = 7;
    public static final int MIN_SS = 5;

    /** Every position at its minimum: the depth chart can be filled (walk-ons guarantee this). */
    public static final int MIN_DEPTH_PLAYERS = MIN_QBS + MIN_RBS + MIN_WRS + MIN_TES + MIN_OLS + MIN_KS
            + MIN_DLS + MIN_LBS + MIN_CBS + MIN_SS;

    /** Recruiting target: CPU classes and the user budget's roster-need bonus fill toward this. */
    public static final int MIN_PLAYERS = 65;
    public static final int MAX_PLAYERS = 75;
}
