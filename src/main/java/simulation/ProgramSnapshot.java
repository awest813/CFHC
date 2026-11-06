package simulation;

/**
 * Program dashboard figures shared by the desktop cards (Program Finances,
 * Program Prestige, Recruiting Pipeline) and Android's Program card, so both
 * shells format money and classify the prestige trend the same way.
 */
public record ProgramSnapshot(int budget, int nilTier, int facilities, int discipline,
                              int prestige, int prestigeDelta, int commits, float classRating) {

    public enum Trend { RISING, STABLE, DECLINING }

    public static ProgramSnapshot of(Team team) {
        int commits = 0;
        if (team.getAllPlayers() != null) {
            for (positions.Player p : team.getAllPlayers()) {
                if (p.year == 1 && !p.wasRedshirt) commits++;
            }
        }
        return new ProgramSnapshot(team.getTeamBudget(), team.getNilCollectiveLevel(), team.getTeamFacilities(),
                team.getTeamDisciplineScore(), team.getTeamPrestige(),
                team.getTeamPrestige() - team.getTeamPrestigeStart(), commits, team.getRecruitingClassRat());
    }

    /** More than +5 this season is rising, below -2 is declining. */
    public Trend trend() {
        if (prestigeDelta > 5) return Trend.RISING;
        if (prestigeDelta < -2) return Trend.DECLINING;
        return Trend.STABLE;
    }

    /** $X.XM, $XK or $X. */
    public static String formatMoney(int amount) {
        if (amount >= 1_000_000) return "$" + String.format(java.util.Locale.ROOT, "%.1fM", amount / 1_000_000.0);
        if (amount >= 1_000) return "$" + (amount / 1_000) + "K";
        return "$" + amount;
    }
}
