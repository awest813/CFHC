package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

/** Both shells grant or clear redshirts only in the preseason. */
public class RedshirtWindowTest {

    @Test
    public void onlyThePreseasonIsOpen() {
        assertTrue(SeasonFlowOrder.isRedshirtWindow(0));
        assertFalse(SeasonFlowOrder.isRedshirtWindow(1));
        assertFalse(SeasonFlowOrder.isRedshirtWindow(League.STANDARD_REG_SEASON_WEEKS));
        assertFalse(SeasonFlowOrder.isRedshirtWindow(
                SeasonFlowOrder.recruitingWeek(League.STANDARD_REG_SEASON_WEEKS)));
    }
}
