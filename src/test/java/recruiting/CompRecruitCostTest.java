package recruiting;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CompRecruitCostTest {

    @Test
    public void compare_higherCostSortsFirst() {
        RecruitingPlayerRecord cheap = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Cheap QB,1,1,50,50,3,false,false,70,70,60,50,50,50,50,50,70,200,60,F");
        RecruitingPlayerRecord expensive = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Expensive QB,1,1,50,50,3,false,false,70,70,80,100,50,50,50,50,70,200,80,F");

        CompRecruitCost comp = new CompRecruitCost();
        List<RecruitingPlayerRecord> list = new ArrayList<>(Arrays.asList(cheap, expensive));
        Collections.sort(list, comp);

        assertEquals("Most expensive recruit should sort first", 100, list.get(0).cost());
        assertEquals("Cheapest recruit should sort last", 50, list.get(1).cost());
    }

    @Test
    public void compare_equalCost_returnsZero() {
        RecruitingPlayerRecord a = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Player A,1,1,50,50,3,false,false,70,70,75,60,50,50,50,50,70,200,75,F");
        RecruitingPlayerRecord b = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Player B,1,1,50,50,3,false,false,70,70,75,60,50,50,50,50,70,200,75,F");

        CompRecruitCost comp = new CompRecruitCost();
        assertEquals(0, comp.compare(a, b));
    }

    @Test
    public void compare_sameObject_returnsZero() {
        RecruitingPlayerRecord a = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Player A,1,1,50,50,3,false,false,70,70,75,60,50,50,50,50,70,200,75,F");
        CompRecruitCost comp = new CompRecruitCost();
        assertEquals(0, comp.compare(a, a));
    }
}
