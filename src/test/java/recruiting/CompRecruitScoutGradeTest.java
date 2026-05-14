package recruiting;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CompRecruitScoutGradeTest {

    @Test
    public void compare_higherScoutGradeSortsFirst() {
        // Scout grade = (4 * recruitOverall + potential) / 5
        // Player A: (4*80 + 90)/5 = (320+90)/5 = 82.0
        // Player B: (4*60 + 70)/5 = (240+70)/5 = 62.0
        RecruitingPlayerRecord high = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,High Grade,1,1,50,50,3,false,false,90,70,80,100,50,50,50,50,70,200,80,F");
        RecruitingPlayerRecord low = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Low Grade,1,1,50,50,3,false,false,70,70,60,50,50,50,50,50,70,200,60,F");

        CompRecruitScoutGrade comp = new CompRecruitScoutGrade();
        List<RecruitingPlayerRecord> list = new ArrayList<>(Arrays.asList(low, high));
        Collections.sort(list, comp);

        assertEquals("Higher scout grade should sort first", "High Grade", list.get(0).name());
        assertEquals("Lower scout grade should sort last", "Low Grade", list.get(1).name());
    }

    @Test
    public void compare_equalGrade_returnsZero() {
        RecruitingPlayerRecord a = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Player A,1,1,50,50,3,false,false,80,70,75,60,50,50,50,50,70,200,75,F");
        RecruitingPlayerRecord b = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Player B,1,1,50,50,3,false,false,80,70,75,60,50,50,50,50,70,200,75,F");

        CompRecruitScoutGrade comp = new CompRecruitScoutGrade();
        assertEquals(0, comp.compare(a, b));
    }

    @Test
    public void compare_sameObject_returnsZero() {
        RecruitingPlayerRecord a = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Player A,1,1,50,50,3,false,false,80,70,75,60,50,50,50,50,70,200,75,F");
        CompRecruitScoutGrade comp = new CompRecruitScoutGrade();
        assertEquals(0, comp.compare(a, a));
    }
}
