package simulation;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/** The Locker Room readout both dashboards show. */
public class TeamMoraleSnapshotTest {

    @Test
    public void labelAndTone_followTheOverallBands() {
        assertEquals("High", new TeamMoraleSnapshot(80, 80, 80).label());
        assertEquals("Steady", new TeamMoraleSnapshot(60, 60, 60).label());
        assertEquals("Uneasy", new TeamMoraleSnapshot(45, 45, 45).label());
        assertEquals("Low", new TeamMoraleSnapshot(10, 10, 10).label());
        assertEquals(1, new TeamMoraleSnapshot(60, 60, 60).tone());
        assertEquals(0, new TeamMoraleSnapshot(45, 45, 45).tone());
        assertEquals(-1, new TeamMoraleSnapshot(10, 10, 10).tone());
    }

    @Test
    public void factors_reportStreakAndEachThreshold() {
        List<TeamMoraleSnapshot.Factor> f = new TeamMoraleSnapshot(65, 70, 69).factors(3);
        assertEquals(4, f.size());
        assertTrue(f.get(0).text().contains("3 Game Win Streak"));
        assertTrue(f.get(0).positive());
        assertTrue("leadership 70 is strong", f.get(1).positive());
        assertFalse("buy-in 69 is slipping", f.get(2).positive());
        assertTrue("chemistry 65 is tight", f.get(3).positive());

        TeamMoraleSnapshot.Factor slump = new TeamMoraleSnapshot(50, 50, 50).factors(-4).get(0);
        assertTrue(slump.text().contains("4 Game Slump"));
        assertFalse(slump.positive());
        assertTrue(new TeamMoraleSnapshot(50, 50, 50).factors(-1).get(0).text().contains("On Track"));
    }
}
