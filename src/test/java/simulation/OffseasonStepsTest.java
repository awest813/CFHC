package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Phase 5 part 3: the offseason hub's step model — ten ordered steps mapped
 * one-to-one onto weeks R+4..R+13.
 */
public class OffseasonStepsTest {

    @Test
    public void steps_areTenNamedEntries() {
        String[] steps = SeasonFlowOrder.offseasonSteps();
        assertEquals(10, steps.length);
        for (String step : steps) {
            assertFalse("Step names must be non-empty", step == null || step.isEmpty());
        }
        assertEquals("Season Summary", steps[0]);
        assertEquals("Recruiting", steps[9]);
    }

    @Test
    public void stepIndex_mapsOffseasonWeeksOnly() {
        int r = 13;
        assertEquals(-1, SeasonFlowOrder.offseasonStepIndex(0, r));          // preseason
        assertEquals(-1, SeasonFlowOrder.offseasonStepIndex(r + 3, r));      // national championship
        assertEquals(0, SeasonFlowOrder.offseasonStepIndex(r + 4, r));       // season summary
        assertEquals(5, SeasonFlowOrder.offseasonStepIndex(r + 9, r));       // graduation
        assertEquals(9, SeasonFlowOrder.offseasonStepIndex(r + 13, r));      // recruiting
        assertEquals(-1, SeasonFlowOrder.offseasonStepIndex(r + 14, r));     // past the gate
    }

    @Test
    public void stepIndex_holdsForOtherSeasonLengths() {
        int r = 15;
        assertEquals(-1, SeasonFlowOrder.offseasonStepIndex(r + 3, r));
        assertEquals(0, SeasonFlowOrder.offseasonStepIndex(r + 4, r));
        assertEquals(9, SeasonFlowOrder.offseasonStepIndex(r + 13, r));
    }
}
