package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AwardTextTest {

    @Test
    public void count_pluralizesOnlyWhenNotOne() {
        assertEquals("1 fumble", AwardText.count(1, "fumble"));
        assertEquals("0 fumbles", AwardText.count(0, "fumble"));
        assertEquals("3 passes", AwardText.count(3, "pass", "passes"));
        assertEquals("1 pass", AwardText.count(1, "pass", "passes"));
    }

    @Test
    public void fewCount_onlySaysJustForLowCounts() {
        assertEquals("just 2 fumbles", AwardText.fewCount(2, 3, "fumble"));
        assertEquals("8 fumbles", AwardText.fewCount(8, 3, "fumble"));
    }

    @Test
    public void article_matchesSpokenNumber() {
        assertEquals("an", AwardText.article(8));
        assertEquals("an", AwardText.article(11));
        assertEquals("an", AwardText.article(18));
        assertEquals("an", AwardText.article(80));
        assertEquals("a", AwardText.article(1));
        assertEquals("a", AwardText.article(10));
        assertEquals("a", AwardText.article(12));
    }

    @Test
    public void times_readsNaturally() {
        assertEquals("once", AwardText.times(1));
        assertEquals("twice", AwardText.times(2));
        assertEquals("0 times", AwardText.times(0));
        assertEquals("7 times", AwardText.times(7));
    }
}
