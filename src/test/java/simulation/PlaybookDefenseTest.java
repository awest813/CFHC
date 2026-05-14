package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

public class PlaybookDefenseTest {

    @Test
    public void constructor_validatesPlaybookRange() {
        PlaybookDefense pb1 = new PlaybookDefense(1);
        assertNotNull(pb1.getStratName());

        PlaybookDefense pb2 = new PlaybookDefense(2);
        assertNotNull(pb2.getStratName());

        PlaybookDefense pb3 = new PlaybookDefense(3);
        assertNotNull(pb3.getStratName());

        PlaybookDefense pb4 = new PlaybookDefense(4);
        assertNotNull(pb4.getStratName());

        PlaybookDefense pb5 = new PlaybookDefense(5);
        assertNotNull(pb5.getStratName());

        PlaybookDefense pb0 = new PlaybookDefense(0);
        assertNotNull("Playbook 0 should fallback to random valid", pb0.getStratName());

        PlaybookDefense pb6 = new PlaybookDefense(6);
        assertNotNull("Playbook 6 should fallback to random valid", pb6.getStratName());

        PlaybookDefense pbNeg = new PlaybookDefense(-1);
        assertNotNull("Playbook -1 should fallback to random valid", pbNeg.getStratName());
    }

    @Test
    public void playbook_hasExpectedName() {
        assertEquals("4-3 Man", new PlaybookDefense(1).getStratName());
        assertEquals("4-6 Bear", new PlaybookDefense(2).getStratName());
        assertEquals("Cover 0", new PlaybookDefense(3).getStratName());
        assertEquals("Cover 2", new PlaybookDefense(4).getStratName());
        assertEquals("Cover 3", new PlaybookDefense(5).getStratName());
    }

    @Test
    public void playbook1_hasCorrectRunPassPref() {
        PlaybookDefense pb = new PlaybookDefense(1);
        assertEquals(1, pb.getRunPref());
        assertEquals(0, pb.getRunStop());
        assertEquals(0, pb.getRunCoverage());
        assertEquals(1, pb.getRunSpy());
        assertEquals(1, pb.getPassPref());
        assertEquals(0, pb.getPassRush());
        assertEquals(0, pb.getPassCoverage());
        assertEquals(1, pb.getPassSpy());
    }

    @Test
    public void playbook5_hasCorrectRunPassPref() {
        PlaybookDefense pb = new PlaybookDefense(5);
        assertEquals(3, pb.getRunPref());
        assertEquals(-1, pb.getRunStop());
        assertEquals(-2, pb.getRunCoverage());
        assertEquals(1, pb.getRunSpy());
        assertEquals(7, pb.getPassPref());
        assertEquals(-1, pb.getPassRush());
        assertEquals(2, pb.getPassCoverage());
        assertEquals(1, pb.getPassSpy());
    }

    @Test
    public void numPlaybooks_isCorrect() {
        PlaybookDefense pb = new PlaybookDefense(1);
        assertEquals(5, pb.numPlaybooks);
    }

    @Test
    public void getStratDescription_isNonNull() {
        for (int i = 1; i <= 5; i++) {
            assertNotNull(new PlaybookDefense(i).getStratDescription());
        }
    }
}
