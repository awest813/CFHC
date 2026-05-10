package simulation;

/**
 * Packed skill ranks for head coaches (five branches, ranks 0–3).
 * Used for recruiting pitch, player development, game prep, discipline culture,
 * and NIL/marketing synergies with {@link Team#getNilCollectiveLevel()}.
 */
public final class CoachSkills {

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
}
