package recruiting;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecruitingPlayerRecordTest {

    // Recruit CSV layout (21 fields):
    // 0:Pos 1:Name 2:Yr 3:RegionCode 4:Character 5:Intelligence 6:Stars
    // 7:IsTransfer 8:WasRedshirt 9:Potential 10:Durability 11:RecruitOvr
    // 12:Cost 13..16:Ratings 17:Height 18:Weight 19:CurrentOvr 20:ScoutFlag
    private static final String UNSCOUTED_RECRUIT_CSV =
            "QB,John Doe,1,45,70,75,5,false,false,90,80,80,500,A,B,C,D,72,200,80,F";

    private static final String SCOUTED_RECRUIT_CSV =
            "QB,John Doe,1,45,70,75,5,false,false,90,80,80,500,A,B,C,D,72,200,80,T";

    // Roster CSV layout (21 fields):
    // 0:Pos 1:Name 2:Yr 3:Potential 4:Intelligence 5:Durability
    // 6..9:Ratings 10:(unused) 11:WasRedshirt 12:IsTransfer 13:(unused)
    // 14:RegionCode 15:Character 16:Stars 17:Height 18:Weight 19:CurrentOvr 20:Improvement
    private static final String ROSTER_CSV =
            "LB,Senior Leader,4,88,75,80,A,B,C,D,-,false,false,-,23,70,3,73,220,85,+2";

    @Test
    public void recruitCsvRoundTrip_parsesCoreFields() {
        RecruitingPlayerRecord record = RecruitingPlayerRecord.fromRecruitCsv(UNSCOUTED_RECRUIT_CSV);

        assertEquals("QB", record.position());
        assertEquals("John Doe", record.name());
        assertEquals("1", record.year());
        assertEquals(45, record.regionCode());
        assertEquals(4, record.regionBucket());
        assertEquals(5, record.stars());
        assertEquals(80, record.recruitOverall());
        assertEquals(90, record.potential());
        assertEquals(500, record.cost());
        assertFalse(record.isTransfer());
        assertEquals(UNSCOUTED_RECRUIT_CSV, record.raw());
    }

    @Test
    public void rosterCsv_parsesSeniorCorrectly() {
        RecruitingPlayerRecord record = RecruitingPlayerRecord.fromRosterCsv(ROSTER_CSV);

        assertEquals("LB", record.position());
        assertEquals("Senior Leader", record.name());
        assertEquals("4", record.year());
        assertEquals(85, record.currentOverall());
        assertEquals(88, record.potential());
        assertEquals(2, record.regionBucket());
        assertFalse(record.isGraduating());
    }

    @Test
    public void isGraduating_trueForFifthYearFlag() {
        String graduatingCsv = ROSTER_CSV.replaceFirst(",4,", ",5,");
        RecruitingPlayerRecord record = RecruitingPlayerRecord.fromRosterCsv(graduatingCsv);
        assertTrue(record.isGraduating());
    }

    @Test
    public void listKey_dropsScoutFlag() {
        RecruitingPlayerRecord unscouted = RecruitingPlayerRecord.fromRecruitCsv(UNSCOUTED_RECRUIT_CSV);
        RecruitingPlayerRecord scouted = RecruitingPlayerRecord.fromRecruitCsv(SCOUTED_RECRUIT_CSV);

        // listKey() strips the trailing ",F" or ",T" so both records produce the same key.
        assertEquals(unscouted.listKey(), scouted.listKey());
    }
}
