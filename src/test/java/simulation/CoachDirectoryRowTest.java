package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoachDirectoryRowTest {

    @Test
    public void employedCoach_splitsAbbreviationFromName() {
        assertEquals("Ada Lovelace", CoachDirectoryRow.bareName("Ada Lovelace (ALA)"));
        assertEquals("ALA", CoachDirectoryRow.statusLabel("Ada Lovelace (ALA)"));
    }

    @Test
    public void unemployedAndRetired_useWords() {
        assertEquals("Ada Lovelace", CoachDirectoryRow.bareName("Ada Lovelace [U]"));
        assertEquals("Unemployed", CoachDirectoryRow.statusLabel("Ada Lovelace [U]"));
        assertEquals("Ada Lovelace", CoachDirectoryRow.bareName("Ada Lovelace [R]"));
        assertEquals("Retired", CoachDirectoryRow.statusLabel("Ada Lovelace [R]"));
    }
}
