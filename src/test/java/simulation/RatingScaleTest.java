package simulation;

import org.junit.Test;

import static org.junit.Assert.*;

public class RatingScaleTest {

    @Test
    public void gradesAndStarsFollowTheBands() {
        assertEquals("A+", RatingScale.grade(90));
        assertEquals("A-", RatingScale.grade(80));
        assertEquals("B", RatingScale.grade(72));
        assertEquals("C", RatingScale.grade(10));
        assertEquals("N/A", RatingScale.grade(0));
        assertEquals(5, RatingScale.stars(92));
        assertEquals(4, RatingScale.stars(82));
        assertEquals(3, RatingScale.stars(70));
        assertEquals(1, RatingScale.stars(1));
        assertEquals(0, RatingScale.stars(0));
    }

    @Test
    public void parseStars_readsRecruitStrings() {
        assertEquals(4f, RatingScale.parseStars("4"), 0f);
        assertEquals(3.5f, RatingScale.parseStars(" 3.5 Stars"), 0f);
        assertEquals(5f, RatingScale.parseStars("9"), 0f);
        assertEquals(0f, RatingScale.parseStars("n/a"), 0f);
        assertEquals(0f, RatingScale.parseStars(null), 0f);
    }
}
