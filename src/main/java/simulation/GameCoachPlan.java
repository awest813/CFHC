package simulation;

/**
 * One coach decision for a {@link Game} checkpoint. Every field is optional:
 * negative/zero defaults mean "no change", so a shell can return a partially
 * filled plan (or null for "coach defers") at any checkpoint.
 *
 * <p>Decisions only affect the coached game. Pre-game and halftime scheme
 * changes ride the same restore path as the AI counter-strategy (the
 * season-long playbooks are re-applied after the final whistle).
 */
public class GameCoachPlan {

    /** Fourth-down intent for crunch time. */
    public enum FourthDownCall {
        AUTO, PUNT, FIELD_GOAL, GO_FOR_IT
    }

    /** Halftime locker-room focus. */
    public enum HalftimeFocus {
        NONE, AGGRESSIVE, BALANCED, CONSERVATIVE
    }

    /** Offensive playbook number to start/switch to; -1 leaves the current book. */
    public int offScheme = -1;

    /** Defensive playbook number to start/switch to; -1 leaves the current book. */
    public int defScheme = -1;

    /** Fourth-down aggression bias, -2 (punt-happy) .. +2 (gambler). Default 0. */
    public int fourthDownBias = 0;

    /** Halftime focus. */
    public HalftimeFocus halftimeFocus = HalftimeFocus.NONE;

    /** Timeouts to burn at the crunch-time checkpoint, 0-3. */
    public int timeoutsToBurn = 0;

    /** Fourth-down intent once crunch time begins. */
    public FourthDownCall crunchFourthDownCall = FourthDownCall.AUTO;

    /** True when this plan carries no coach intent at all. */
    public boolean isEmpty() {
        return offScheme < 0 && defScheme < 0 && fourthDownBias == 0
                && halftimeFocus == HalftimeFocus.NONE
                && timeoutsToBurn <= 0
                && crunchFourthDownCall == FourthDownCall.AUTO;
    }
}
