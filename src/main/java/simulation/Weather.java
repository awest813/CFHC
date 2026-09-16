package simulation;

/**
 * Per-game weather. Generated once at kickoff from the home region and the
 * calendar week; feeds small, bounded modifiers into pass completion, rushing,
 * field-goal accuracy, fumble rates, and run/pass tendency.
 *
 * <p>Region codes follow the shared recruiting/portal region scale; the lower
 * codes are treated as cold-weather regions (rain turns to snow there late in
 * the season). Adjust {@link #COLD_REGION_CUTOFF} if the region mapping grows
 * a name-based source.
 */
public enum Weather {
    CLEAR("Clear"),
    CLOUDY("Overcast"),
    WIND("Windy"),
    RAIN("Rain"),
    SNOW("Snow");

    /** Regions with index below this are treated as cold-weather. */
    public static final int COLD_REGION_CUTOFF = 3;

    private final String label;

    Weather(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isWet() {
        return this == RAIN || this == SNOW;
    }

    /** Deterministic per-game weather from the home region and calendar week. */
    public static Weather forGame(int homeRegion, int week, int regSeasonWeeks) {
        boolean cold = homeRegion < COLD_REGION_CUTOFF;
        boolean late = week >= Math.max(8, regSeasonWeeks - 4);
        double precipChance = late ? 0.35 : 0.15;
        double windChance = 0.10;

        double roll = SimRandom.nextDouble();
        if (roll < precipChance) {
            boolean snow = cold && SimRandom.nextDouble() < (late ? 0.6 : 0.2);
            return snow ? SNOW : RAIN;
        }
        if (roll < precipChance + windChance) {
            return WIND;
        }
        if (roll < precipChance + windChance + 0.25) {
            return CLOUDY;
        }
        return CLEAR;
    }

    /** Completion-formula modifier for passing plays. */
    public int completionAdj() {
        switch (this) {
            case RAIN: return -3;
            case SNOW: return -5;
            default: return 0;
        }
    }

    /** Rushing-formula modifier. */
    public int rushAdj() {
        return this == SNOW ? -2 : 0;
    }

    /** Field-goal accuracy modifier. */
    public int fgAdj() {
        switch (this) {
            case WIND: return -4;
            case SNOW: return -4;
            case RAIN: return -2;
            default: return 0;
        }
    }

    /** Relative multiplier on fumble odds. */
    public double fumbleMultiplier() {
        return isWet() ? 1.25 : 1.0;
    }

    /** Tendency shift toward the run in bad weather (applied to preferRush). */
    public double runLean() {
        return isWet() ? 0.15 : 0.0;
    }
}
