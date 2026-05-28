package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PlaybookOffenseTest {

    @Test
    public void constructor_validatesPlaybookRange() {
        for (int i = 1; i <= 6; i++) {
            assertNotNull(new PlaybookOffense(i).getStratName());
        }

        assertNotNull("Playbook 0 should fallback to random valid", new PlaybookOffense(0).getStratName());
        assertNotNull("Playbook 7 should fallback to random valid", new PlaybookOffense(7).getStratName());
        assertNotNull("Playbook -1 should fallback to random valid", new PlaybookOffense(-1).getStratName());
    }

    @Test
    public void playbook_hasExpectedName() {
        assertEquals("Multiple Pro", new PlaybookOffense(1).getStratName());
        assertEquals("Power Spread", new PlaybookOffense(2).getStratName());
        assertEquals("Quick Game", new PlaybookOffense(3).getStratName());
        assertEquals("Air Raid", new PlaybookOffense(4).getStratName());
        assertEquals("Zone Read", new PlaybookOffense(5).getStratName());
        assertEquals("Spread RPO", new PlaybookOffense(6).getStratName());
    }

    @Test
    public void playbook1_hasCorrectRunPassPref() {
        PlaybookOffense pb = new PlaybookOffense(1);
        assertEquals(1, pb.getRunPref());
        assertEquals(0, pb.getRunProtection());
        assertEquals(0, pb.getRunPotential());
        assertEquals(1, pb.getRunUsage());
        assertEquals(1, pb.getPassPref());
        assertEquals(0, pb.getPassProtection());
        assertEquals(0, pb.getPassPotential());
        assertEquals(1, pb.getPassUsage());
    }

    @Test
    public void playbook6_hasCorrectRunPassPref() {
        PlaybookOffense pb = new PlaybookOffense(6);
        assertEquals(2, pb.getRunPref());
        assertEquals(-1, pb.getRunProtection());
        assertEquals(1, pb.getRunPotential());
        assertEquals(1, pb.getRunUsage());
        assertEquals(3, pb.getPassPref());
        assertEquals(-1, pb.getPassProtection());
        assertEquals(-1, pb.getPassPotential());
        assertEquals(1, pb.getPassUsage());
    }

    @Test
    public void numPlaybooks_isCorrect() {
        assertEquals(6, new PlaybookOffense(1).numPlaybooks);
    }

    @Test
    public void getStratDescription_isNonNull() {
        for (int i = 1; i <= 6; i++) {
            assertNotNull(new PlaybookOffense(i).getStratDescription());
        }
    }
}
