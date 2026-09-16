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

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }
}
