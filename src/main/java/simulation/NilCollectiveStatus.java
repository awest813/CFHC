package simulation;

import java.util.Locale;

/**
 * Read-only snapshot of a program's NIL / booster collective for the UI:
 * current tier, what it is worth right now, and what the next tier costs
 * against the athletic budget. Shared by the desktop and Android shells so
 * both show the same numbers the sim actually uses.
 */
public final class NilCollectiveStatus {

    public final int tier;
    public final int maxTier;
    /** Home-game revenue boost from tier + coach NIL skill, in percent. */
    public final double homeRevenueBonusPct;
    /** Weekly booster stipend added to the athletic budget (tier + coach skill). */
    public final int weeklyStipend;
    /** Extra recruiting dollars each recruiting period from the tier. */
    public final int recruitingBonus;
    /** Next tier number, or 0 when maxed. */
    public final int nextTier;
    /** Athletic budget needed for the next tier, or 0 when maxed. */
    public final int nextTierCost;
    public final int budget;

    private NilCollectiveStatus(int tier, int maxTier, double homeRevenueBonusPct, int weeklyStipend,
                                int recruitingBonus, int nextTier, int nextTierCost, int budget) {
        this.tier = tier;
        this.maxTier = maxTier;
        this.homeRevenueBonusPct = homeRevenueBonusPct;
        this.weeklyStipend = weeklyStipend;
        this.recruitingBonus = recruitingBonus;
        this.nextTier = nextTier;
        this.nextTierCost = nextTierCost;
        this.budget = budget;
    }

    public static NilCollectiveStatus of(Team team) {
        if (team == null) {
            return new NilCollectiveStatus(0, League.NIL_MAX_TIER, 0, 0, 0, 1,
                    League.nilCollectiveUpgradeCost(1), 0);
        }
        int tier = Math.max(0, Math.min(League.NIL_MAX_TIER, team.getNilCollectiveLevel()));
        double revenuePct = (team.getHomeGameRevenueMultiplier() - 1.0) * 100.0;
        int next = tier >= League.NIL_MAX_TIER ? 0 : tier + 1;
        return new NilCollectiveStatus(
                tier,
                League.NIL_MAX_TIER,
                Math.max(0, revenuePct),
                Math.max(0, team.getWeeklyCollectiveStipend()),
                tier * SimulationFacade.RECRUITING_BUDGET_PER_NIL_TIER,
                next,
                League.nilCollectiveUpgradeCost(next),
                team.getTeamBudget());
    }

    public boolean isMaxed() {
        return nextTier == 0;
    }

    /** True when the current athletic budget would fund the next tier this offseason. */
    public boolean canAffordNextTier() {
        return !isMaxed() && budget > nextTierCost;
    }

    /** 0-100 progress of the athletic budget toward the next tier's cost. */
    public int progressPercent() {
        if (isMaxed()) return 100;
        if (nextTierCost <= 0) return 100;
        return (int) Math.max(0, Math.min(100, Math.round(100.0 * budget / nextTierCost)));
    }

    /** "Tier 3 / 12". */
    public String tierLabel() {
        return "Tier " + tier + " / " + maxTier;
    }

    /** "+7.2% home gate  ·  +$285/wk boosters  ·  +$72 recruiting" (or a no-collective note). */
    public String effectsLine() {
        if (tier == 0 && weeklyStipend == 0 && homeRevenueBonusPct < 0.05) {
            return "No collective yet: no booster stipend, home-gate or recruiting bonus.";
        }
        return String.format(Locale.ROOT, "+%.1f%% home gate  ·  +$%,d/wk boosters  ·  +$%,d recruiting",
                homeRevenueBonusPct, weeklyStipend, recruitingBonus);
    }

    /**
     * "Next: Tier 4 at $36,000 — budget $21,500 (60%). Funds automatically in the
     * offseason." / "Maxed out at Tier 12."
     */
    public String nextTierLine() {
        if (isMaxed()) {
            return "Maxed out at Tier " + maxTier + ".";
        }
        String status = canAffordNextTier()
                ? "Budget covers it: the collective expands this offseason."
                : "Funds automatically in the offseason once the budget covers it.";
        return String.format(Locale.ROOT, "Next: Tier %d at $%,d — budget $%,d (%d%%). %s",
                nextTier, nextTierCost, budget, progressPercent(), status);
    }
}
