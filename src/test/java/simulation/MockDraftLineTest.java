package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MockDraftLineTest {

    @Test
    public void parse_splitsPickPlayerSchoolAndOverall() {
        String raw = "Round 2, Pick 4 : New York Jets\nQB Ada Lovelace\nJunior>\nPine Valley\nOverall: 88";
        MockDraftLine line = MockDraftLine.parse(raw);
        assertTrue(line.parsed);
        assertEquals("2", line.round);
        assertEquals("4", line.pick);
        assertEquals("R2 · 4", line.pickLabel);
        assertEquals("QB Ada Lovelace", line.player);
        assertEquals("Junior", line.year);
        assertEquals("Pine Valley", line.school);
        assertEquals("New York Jets", line.proTeam);
        assertEquals("88", line.overall);
        assertEquals("Junior  ·  Pine Valley  ·  New York Jets", line.metaLine());
        assertEquals("R2 · 4  QB Ada Lovelace  88  Pine Valley  New York Jets", line.boardLine());
    }

    @Test
    public void parse_messageRowStaysUnparsed() {
        MockDraftLine line = MockDraftLine.parse("No prospects have declared for the draft yet.");
        assertFalse(line.parsed);
        assertEquals("No prospects have declared for the draft yet.", line.raw);
        assertEquals("No prospects have declared for the draft yet.", line.boardLine());
    }
}
