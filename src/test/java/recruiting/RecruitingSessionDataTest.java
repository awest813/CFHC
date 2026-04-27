package recruiting;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RecruitingSessionDataTest {

    @Test
    public void parseCoachTalentField_plainInteger() {
        assertEquals(72, RecruitingSessionData.parseCoachTalentField("72"));
    }

    @Test
    public void parseCoachTalentField_stripsNoise() {
        assertEquals(88, RecruitingSessionData.parseCoachTalentField(" 88% "));
    }

    @Test
    public void parseCoachTalentField_clampsToStaffBounds() {
        assertEquals(95, RecruitingSessionData.parseCoachTalentField("120"));
        assertEquals(20, RecruitingSessionData.parseCoachTalentField("5"));
    }

    @Test
    public void parseCoachTalentField_emptyDefaults() {
        assertEquals(70, RecruitingSessionData.parseCoachTalentField(""));
        assertEquals(70, RecruitingSessionData.parseCoachTalentField("   "));
        assertEquals(70, RecruitingSessionData.parseCoachTalentField(null));
        assertEquals(70, RecruitingSessionData.parseCoachTalentField("abc"));
    }

    @Test
    public void parseRecruitBudgetUnits_clamps() {
        assertEquals(1, RecruitingSessionData.parseRecruitBudgetUnits("0", 5));
        assertEquals(20, RecruitingSessionData.parseRecruitBudgetUnits("999", 5));
        assertEquals(8, RecruitingSessionData.parseRecruitBudgetUnits("8", 5));
    }

    @Test
    public void scoutPlayer_respectsCoachTalentDiscount() {
        RecruitingSessionData high = newSession(1000, 84);
        RecruitingSessionData low = newSession(1000, 24);
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Scout Test,1,45,70,75,3,false,false,70,70,70,500,A,B,C,D,72,200,70,F");
        int costHigh = expectedScoutCost(500, 84);
        assertTrue(high.scoutPlayer(recruit));
        assertEquals(1000 - costHigh, high.recruitingBudget);

        RecruitingPlayerRecord recruit2 = RecruitingPlayerRecord.fromRecruitCsv(
                "RB,Scout Test2,1,45,70,75,3,false,false,70,70,70,500,A,B,C,D,72,200,70,F");
        int costLow = expectedScoutCost(500, 24);
        assertTrue(low.scoutPlayer(recruit2));
        assertEquals(1000 - costLow, low.recruitingBudget);
        assertTrue(costHigh < costLow);
    }

    private static int expectedScoutCost(int recruitCost, int coachTalent) {
        int base = Math.max(10, recruitCost / 10);
        int talentDiscount = Math.min(8, coachTalent / 12);
        return Math.max(5, base - talentDiscount);
    }

    /** Session with synthetic budget/talent for unit tests (same ctor path as production). */
    private static RecruitingSessionData newSession(int budget, int coachTalent) {
        RecruitingSessionData s = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5," + coachTalent + "%\n"
                        + "END_TEAM_INFO%\n");
        s.recruitingBudget = budget;
        return s;
    }
}
