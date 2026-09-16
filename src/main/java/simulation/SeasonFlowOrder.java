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

    /** Legacy save-format sentinel: currentWeek 99 means "recruiting checkpoint saved". */
    public static final int RECRUITING_SENTINEL_WEEK = 99;

    // Canonical in-season boundary weeks (all relative to R = regSeasonWeeks).

    public static int conferenceChampionshipWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) - 1;
    }

    public static int bowlWeek1(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks);
    }

    public static int bowlWeek2(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 1;
    }

    public static int bowlWeek3(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 2;
    }

    public static int nationalChampionshipWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 3;
    }

    // Canonical offseason step weeks (see SeasonController class doc for the sequence).

    public static int seasonSummaryWeek(int regSeasonWeeks) {
        return firstOffseasonWeek(regSeasonWeeks);
    }

    public static int contractsWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 5;
    }

    public static int jobOffersWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 6;
    }

    public static int coachCarouselWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 7;
    }

    public static int coordinatorHiringWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 8;
    }

    public static int graduationWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 9;
    }

    public static int transferPortalWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 10;
    }

    public static int transferListWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 11;
    }

    public static int realignmentWeek(int regSeasonWeeks) {
        return Math.max(1, regSeasonWeeks) + 12;
    }

    /** Ordered offseason step names, matching weeks R+4 through R+13. */
    public static String[] offseasonSteps() {
        return new String[]{
                "Season Summary",
                "Contracts",
                "Job Offers",
                "Coach Carousel",
                "Coordinator Hiring",
                "Graduation & Development",
                "Transfer Portal",
                "Transfer List",
                "Realignment",
                "Recruiting"
        };
    }

    /** Index (0-based) into {@link #offseasonSteps} for the given week, or -1 outside the offseason. */
    public static int offseasonStepIndex(int week, int regSeasonWeeks) {
        int w = clampWeek(week);
        int r = Math.max(1, regSeasonWeeks);
        if (w < r + 4 || w > r + 13) {
            return -1;
        }
        return w - (r + 4);
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
