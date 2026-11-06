package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

public class TrendChartModelTest {

    @Test
    public void positionsSpanTheRange_andClamp() {
        TrendChartModel m = new TrendChartModel(new double[]{0, 50, 120}, new String[]{"a", "b", "c"}, 0, 100, null);
        assertEquals(0.0, m.xFraction(0), 1e-9);
        assertEquals(0.5, m.xFraction(1), 1e-9);
        assertEquals(1.0, m.xFraction(2), 1e-9);
        assertEquals(0.0, m.yFraction(0), 1e-9);
        assertEquals(0.5, m.yFraction(1), 1e-9);
        assertEquals("above max is clamped", 1.0, m.yFraction(2), 1e-9);
    }

    @Test
    public void singlePointAndFlatRange_stayDrawable() {
        TrendChartModel m = new TrendChartModel(new double[]{40}, new String[]{"2026"}, 40, 40, null);
        assertEquals(0.5, m.xFraction(0), 1e-9);
        assertTrue(m.yFraction(0) >= 0 && m.yFraction(0) <= 1);
        assertArrayEquals(new int[]{0}, m.xLabelIndexes(5));
    }

    @Test
    public void xLabels_areThinnedAndKeepTheEnds() {
        String[] years = new String[20];
        for (int i = 0; i < years.length; i++) years[i] = Integer.toString(2026 + i);
        TrendChartModel m = new TrendChartModel(new double[20], years, 0, 10, null);
        int[] idx = m.xLabelIndexes(5);
        assertEquals(5, idx.length);
        assertEquals(0, idx[0]);
        assertEquals(19, idx[4]);
        for (int k = 1; k < idx.length; k++) assertTrue(idx[k] > idx[k - 1]);
        assertEquals(3, m.xLabelIndexes(3).length);
        assertEquals("never more labels than seasons", 20, m.xLabelIndexes(50).length);
    }

    @Test
    public void rankLabels_invertTheStoredScore() {
        int teams = 130;
        // Charts store "teams - rank" so a better rank plots higher.
        TrendChartModel m = new TrendChartModel(new double[]{teams - 1, teams - 25}, new String[]{"x", "y"},
                0, teams, v -> Integer.toString(teams - (int) Math.round(v)));
        assertEquals("130", m.gridLabel(0, 4));
        assertEquals("0", m.gridLabel(4, 4));
        assertTrue(m.yFraction(0) > m.yFraction(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mismatchedInput_isRejected() {
        new TrendChartModel(new double[]{1, 2}, new String[]{"a"}, 0, 1, null);
    }
}
