package simulation;

/**
 * Opt-in coaching hook for interactive gameday. When a league installs a
 * listener ({@code League#setGameCoachListener}), the user team's games pause
 * at three checkpoints and the listener returns a {@link GameCoachPlan};
 * returning {@code null} defers to the standard CPU logic.
 *
 * <p>The hook is synchronous by design — shells show a modal decision UI from
 * inside the callback (desktop already uses this pattern for career dialogs),
 * and headless tests answer with scripted plans. Games without a listener, and
 * games between two CPU teams, run exactly as before.
 */
public interface GameCoachListener {

    /** Sim checkpoints that request a coach decision. */
    enum Checkpoint {
        /** Before the first snap: scheme, aggression. */
        PREGAME,
        /** Halftime locker room: focus, scheme switch. */
        HALFTIME,
        /** Start of the fourth quarter: timeouts, crunch fourth-down intent. */
        CRUNCH_TIME
    }

    /**
     * @param game       the coached game (read state freely to decide)
     * @param checkpoint which decision point fired
     * @return the coach's plan, or {@code null} to defer to CPU logic
     */
    GameCoachPlan decide(Game game, Checkpoint checkpoint);
}
