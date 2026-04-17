package recruiting;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecruitingPlayerRecordTest {

    @Test
    public void testFromRecruitCsv() {
        // Sample recruit CSV: Name,Pos,Yr,Stars,Ovr,Pot,Attr,Home,Cost,Scouted
        String csv = "John Doe,QB,0,5,80,90,80,10,500,F";
        RecruitingPlayerRecord record = RecruitingPlayerRecord.fromRecruitCsv(csv);

        assertEquals("John Doe", record.name());
        assertEquals("QB", record.position());
        assertEquals(0, record.year());
        assertEquals(5, record.stars());
        assertEquals(80, record.recruitOverall());
        assertEquals(90, record.potential());
        assertEquals(10, record.regionBucket());
        assertEquals(500, record.cost());
        assertFalse(record.isScouted());
        assertFalse(record.isTransfer());
    }

    @Test
    public void testFromRosterCsv() {
        // Sample roster CSV: Name,Pos,Yr,Ovr,OvrStart,Pot,Attr,Att,Dur,Imp,Prog,Home,Char,Ht,Wt,Susp,Weeks,Times,Talent,Stats...
        // Index mapping used in fromRosterCsv:
        // 0:name, 1:pos, 2:yr, 3:ovr, 5:pot, 11:home, 20:graduating
        String csv = "Senior Leader,LB,3,85,80,88,70,75,80,10,5,2,70,75,220,F,0,0,8.5,0,T"; 
        RecruitingPlayerRecord record = RecruitingPlayerRecord.fromRosterCsv(csv);

        assertEquals("Senior Leader", record.name());
        assertEquals("LB", record.position());
        assertEquals(3, record.year());
        assertEquals(85, record.currentOverall());
        assertEquals(88, record.potential());
        assertEquals(2, record.regionBucket());
        assertTrue(record.isGraduating());
    }

    @Test
    public void testScoutingToggle() {
        String csv = "John Doe,QB,0,5,80,90,80,10,500,F";
        RecruitingPlayerRecord record = RecruitingPlayerRecord.fromRecruitCsv(csv);
        assertFalse(record.isScouted());

        // Scouted version should have 'T' at the end
        String scoutedCsv = csv.substring(0, csv.length() - 1) + "T";
        RecruitingPlayerRecord scoutedRecord = RecruitingPlayerRecord.fromRecruitCsv(scoutedCsv);
        assertTrue(scoutedRecord.isScouted());
    }
}
