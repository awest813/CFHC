package desktop;

import simulation.SimulationFacade;
import simulation.Team;

/**
 * The one "Recruiting Budget" figure shown outside the recruiting board
 * (Program Finances, Program Health, Team Detail), so those views can't
 * disagree with each other or with the board.
 *
 * <p>{@code Team.getTeamRecruitBudget()} is a legacy engine field that is never
 * set (it always read $0), so it is deliberately not used here.
 */
final class DesktopRecruitingBudget {

    /** Implemented by the window that owns the live recruiting board. */
    interface LiveBoard {
        /** Budget left on {@code team}'s open board, or -1 when it has none. */
        int remainingRecruitingBudget(Team team);
    }

    private DesktopRecruitingBudget() {}

    /**
     * @param owner the window hosting the view; when it is a {@link LiveBoard}
     *              with an open board for {@code team}, the remaining budget
     *              (what the Recruiting screen shows) wins over the preview
     * @return the budget, or -1 when {@code team} doesn't recruit with a board
     *         budget (CPU programs)
     */
    static int forTeam(Team team, Object owner) {
        if (team == null || !team.isUserControlled()) {
            return -1;
        }
        if (owner instanceof LiveBoard board) {
            int live = board.remainingRecruitingBudget(team);
            if (live >= 0) {
                return live;
            }
        }
        try {
            return SimulationFacade.previewRecruitingBudget(team);
        } catch (RuntimeException ex) {
            return team.getUserRecruitBudget();
        }
    }

    /** "$1,234", or an em dash when there is no board budget. */
    static String format(int budget) {
        return budget < 0 ? "—" : "$" + String.format(java.util.Locale.ROOT, "%,d", budget);
    }
}
