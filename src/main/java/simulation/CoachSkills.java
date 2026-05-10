package simulation;

import staff.Staff;

/**
 * Packed skill ranks for head coaches (five branches, ranks 0–3).
 * Used for recruiting pitch, player development, game prep, discipline culture,
 * and NIL/marketing synergies with {@link Team#getNilCollectiveLevel()}.
 */
public final class CoachSkills {

    /** Shared coach-program dialog title (desktop + Android). */
    public static final String PROGRAM_DIALOG_TITLE = "Coach Program, NIL & Facilities";
    /** Upgrade button label (desktop + Android). */
    public static final String UPGRADE_BRANCH_BUTTON_LABEL = "Upgrade selected branch";
    /**
     * Footer hint under branch picker (desktop + Android).
     */
    public static final String PROGRAM_DIALOG_FOOTER_HINT =
            "XP is earned weekly while you advance the season. "
                    + "NIL tier grows in the offseason when your budget can fund the collective. "
                    + "Training facilities still power player development in the sim core.";

    public static final int RECRUITING = 0;
    public static final int DEVELOPMENT = 1;
    public static final int GAME_PREP = 2;
    public static final int DISCIPLINE_CULTURE = 3;
    public static final int NIL_MARKETING = 4;

    public static final int BRANCH_COUNT = 5;
    private static final int BITS_PER_BRANCH = 3;
    private static final int RANK_MASK = 7;
    private static final int MAX_RANK = 3;

    private CoachSkills() {
    }

    public static int getRank(int packed, int branch) {
        if (branch < 0 || branch >= BRANCH_COUNT) {
            return 0;
        }
        return Math.min(MAX_RANK, (packed >>> (branch * BITS_PER_BRANCH)) & RANK_MASK);
    }

    public static int withRank(int packed, int branch, int rank) {
        if (branch < 0 || branch >= BRANCH_COUNT) {
            return packed;
        }
        rank = Math.min(MAX_RANK, Math.max(0, rank));
        int shift = branch * BITS_PER_BRANCH;
        int cleared = packed & ~(RANK_MASK << shift);
        return cleared | (rank << shift);
    }

    /** XP cost to move from {@code currentRank} to {@code currentRank + 1}. */
    public static int costForNextRank(int currentRank) {
        if (currentRank >= MAX_RANK) {
            return Integer.MAX_VALUE;
        }
        return 40 + currentRank * 38;
    }

    public static String branchTitle(int branch) {
        return switch (branch) {
            case RECRUITING -> "Recruiting pitch";
            case DEVELOPMENT -> "Player development";
            case GAME_PREP -> "Game prep";
            case DISCIPLINE_CULTURE -> "Discipline & culture";
            case NIL_MARKETING -> "NIL & boosters";
            default -> "Skill";
        };
    }

    public static String branchBlurb(int branch) {
        return switch (branch) {
            case RECRUITING -> "Stronger scouting grades and extra recruiting budget.";
            case DEVELOPMENT -> "Faster attribute growth for your roster.";
            case GAME_PREP -> "Small edge in situational coaching adjustments.";
            case DISCIPLINE_CULTURE -> "Helps stabilize team discipline each offseason.";
            case NIL_MARKETING -> "Extra booster revenues with your collective.";
            default -> "";
        };
    }

    /**
     * Full coach-program summary for desktop/Android dialogs (single source of truth).
     */
    public static String buildProgramSummary(Team t, Staff hc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Training facilities: L").append(t.getTeamFacilities())
                .append("  (drives base player development in the sim)\n");
        sb.append("NIL / booster collective: Tier ").append(t.nilCollectiveLevel)
                .append("  (home revenue, weekly stipend, recruiting budget)\n\n");
        sb.append("Coach skills:\n");
        for (int b = 0; b < BRANCH_COUNT; b++) {
            int r = getRank(hc.coachSkillRanksBits, b);
            int next = costForNextRank(r);
            sb.append("• ").append(branchTitle(b)).append(" — rank ").append(r).append("/3");
            if (r < 3) {
                sb.append("  (next: ").append(next).append(" XP)\n");
            } else {
                sb.append("  (max)\n");
            }
            sb.append("  ").append(branchBlurb(b)).append("\n");
        }
        return sb.toString();
    }

    /** Branch picker row label; matches desktop combo renderer. */
    public static String branchPickerLabel(int packedRanks, int branch) {
        int r = getRank(packedRanks, branch);
        return branchTitle(branch) + "  [Rank " + r + "/3]";
    }
}
