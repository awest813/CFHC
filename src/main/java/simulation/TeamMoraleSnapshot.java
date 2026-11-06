package simulation;

/**
 * Point-in-time team morale readout for dashboard presentation and light sim
 * effects. All values are 0-100. Chemistry is the season-long value the game
 * engine already feeds into {@code Game.getTeamChemistryAdv()}; leadership and
 * buy-in are derived from roster character, staff discipline, streaks, and
 * coach culture skill.
 */
public record TeamMoraleSnapshot(int chemistry, int leadership, int buyIn) {
    public TeamMoraleSnapshot {
        chemistry = clamp(chemistry);
        leadership = clamp(leadership);
        buyIn = clamp(buyIn);
    }

    public int overall() {
        return (chemistry + leadership + buyIn) / 3;
    }

    /** Headline word for the overall value: High, Steady, Uneasy or Low. */
    public String label() {
        int o = overall();
        return o >= 75 ? "High" : (o >= 55 ? "Steady" : (o >= 40 ? "Uneasy" : "Low"));
    }

    /** -1 low, 0 uneasy, +1 steady or high: shells pick red / gold / green from it. */
    public int tone() {
        int o = overall();
        return o >= 55 ? 1 : (o >= 40 ? 0 : -1);
    }

    /** One locker-room factor line and whether it helps or hurts. */
    public record Factor(String text, boolean positive) { }

    /**
     * The four factor lines the dashboards show, in order: streak, leadership,
     * buy-in, chemistry. {@code winStreak} is positive for wins, negative for losses.
     */
    public java.util.List<Factor> factors(int winStreak) {
        return java.util.List.of(
                new Factor(winStreak >= 2 ? "\u2713  " + winStreak + " Game Win Streak"
                        : (winStreak <= -2 ? "\u2193  " + (-winStreak) + " Game Slump" : "\u2713  Season On Track"),
                        winStreak >= -1),
                new Factor(leadership >= 70 ? "\u2713  Strong Locker Room" : "\u2193  Leadership Questions",
                        leadership >= 70),
                new Factor(buyIn >= 70 ? "\u2713  Players Bought In" : "\u2193  Buy-In Slipping", buyIn >= 70),
                new Factor(chemistry >= 65 ? "\u2713  Tight Chemistry" : "\u2193  Chemistry Building",
                        chemistry >= 65));
    }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }
}
