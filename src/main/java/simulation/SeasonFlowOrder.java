package simulation;

/**
 * Single source of truth for season week → phase ordering.
 *
 * <p>Canonical order for {@code R = regSeasonWeeks}:
 * <ol>
 *   <li>Week 0 — Preseason</li>
 *   <li>Weeks 1 … R−2 — Regular season</li>
 *   <li>Week R−1 — Conference championships</li>
 *   <li>Weeks R … R+2 — Bowls / playoffs</li>
 *   <li>Week R+3 — National Championship</li>
 *   <li>Weeks R+4 … R+12 — Offseason steps</li>
 *   <li>Week ≥ R+13 — Recruiting (gate; week does not advance until next season)</li>
 * </ol>
 */
public final class SeasonFlowOrder {
    private SeasonFlowOrder() {
    }

    public enum Phase {
        PRESEASON,
        REGULAR_SEASON,
        CONFERENCE_CHAMPIONSHIP,
        POSTSEASON,
        NATIONAL_CHAMPIONSHIP,
        OFFSEASON,
        RECRUITING
    }

    /** Coarse cycle labels in chronological order. */
    public static final String[] CYCLE_ORDER = {
            "Preseason", "Regular Season", "Postseason", "Offseason", "Recruiting"
    };

    public static int clampWeek(int week) {
        return week < 0 ? 0 : week;
    }

    public static Phase phaseAt(int week, int regSeasonWeeks) {
        int w = clampWeek(week);
        int r = Math.max(1, regSeasonWeeks);
        if (w >= r + 13) return Phase.RECRUITING;
        if (w == 0) return Phase.PRESEASON;
        if (w < r - 1) return Phase.REGULAR_SEASON;
        if (w == r - 1) return Phase.CONFERENCE_CHAMPIONSHIP;
        if (w <= r + 2) return Phase.POSTSEASON;
        if (w == r + 3) return Phase.NATIONAL_CHAMPIONSHIP;
        return Phase.OFFSEASON;
    }

    public static Phase phaseAt(League league) {
        if (league == null) return Phase.PRESEASON;
        return phaseAt(league.currentWeek, league.regSeasonWeeks);
    }

    /** Midseason dialog / progression fires after this week is played. */
    public static int midseasonWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) / 2;
    }

    /**
     * First offseason week — postseason (through NCG) is complete when
     * {@code currentWeek} reaches this value.
     */
    public static int firstOffseasonWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 4;
    }

    public static int recruitingWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 13;
    }

    public static boolean isRecruitingGate(int week, int regSeasonWeeks) {
        return clampWeek(week) >= recruitingWeek(regSeasonWeeks);
    }

    /**
     * Expected {@code currentWeek} after one successful {@link SeasonController#advanceWeek()}
     * from {@code weekBefore}. Recruiting is a hard gate (week stays put).
     */
    public static int expectedWeekAfterAdvance(int weekBefore, int regSeasonWeeks) {
        int w = clampWeek(weekBefore);
        if (isRecruitingGate(w, regSeasonWeeks)) {
            return w;
        }
        return w + 1;
    }

    /** Index into {@link #CYCLE_ORDER} for coarse cycle comparisons. */
    public static int cycleIndex(Phase phase) {
        switch (phase) {
            case PRESEASON:
                return 0;
            case REGULAR_SEASON:
            case CONFERENCE_CHAMPIONSHIP:
                return 1;
            case POSTSEASON:
            case NATIONAL_CHAMPIONSHIP:
                return 2;
            case OFFSEASON:
                return 3;
            case RECRUITING:
                return 4;
            default:
                return 0;
        }
    }

    public static String cycleLabel(Phase phase) {
        return CYCLE_ORDER[cycleIndex(phase)];
    }
}
