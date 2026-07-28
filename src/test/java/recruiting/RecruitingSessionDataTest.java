package recruiting;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void recruitPlayer_rejectsOverBudget() {
        RecruitingSessionData session = newSession(50, 70);
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Big Cost,1,45,70,75,3,false,false,70,70,70,500,A,B,C,D,72,200,70,F");
        try {
            session.recruitPlayer(recruit, false, 0, new Random(1));
            fail("Expected IllegalArgumentException when cost exceeds budget");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("exceeds"));
        }
    }

    @Test
    public void recruitPlayer_spendsBudgetAndAddsCommitmentToRosterGroup() {
        String rawRecruit = "QB,Signed Target,1,45,70,75,3,false,false,70,70,70,120,A,B,C,D,72,200,70,F";
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\n"
                        + "END_TEAM_INFO%\n"
                        + rawRecruit + "%\n"
                        + "END_RECRUITS%\n");
        RecruitingPlayerRecord recruit = session.availAll.get(0);
        session.recruitingBudget = 500;

        session.recruitPlayer(recruit, false, 1.0, new Random(1));

        assertEquals(380, session.recruitingBudget);
        assertTrue(session.playersRecruited.contains(recruit));
        assertTrue(session.teamQBs.contains(recruit));
        assertFalse(session.availAll.contains(recruit));
        assertFalse(session.availQBs.contains(recruit));
    }

    @Test
    public void buildRecruitsSaveData_persistsSignedCommitments() {
        String rawRecruit = "WR,Save Target,1,45,70,75,3,false,false,70,70,70,110,A,B,C,D,72,200,70,F";
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\n"
                        + "END_TEAM_INFO%\n"
                        + rawRecruit + "%\n"
                        + "END_RECRUITS%\n");
        RecruitingPlayerRecord recruit = session.availAll.get(0);
        session.recruitingBudget = 500;

        session.recruitPlayer(recruit, false, 1.0, new Random(1));

        String saveData = session.buildRecruitsSaveData();
        assertTrue(saveData.contains(rawRecruit));
        assertTrue(saveData.endsWith("END_RECRUITS%\n"));
    }

    @Test
    public void fromUserTeamInfo_ignoresRecruitingTerminator() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,75%\n"
                        + "END_TEAM_INFO%\n"
                        + "QB,Casey Arm,1,45,70,75,5,false,false,90,80,80,40,A,B,C,D,72,200,80,F%\n"
                        + "END_RECRUITS%\n");

        assertEquals(1, session.availAll.size());
        assertEquals("Casey Arm", session.availAll.get(0).name());
    }

    @Test
    public void recruitPlayer_rejectsWhenRosterFull() {
        RecruitingSessionData session = newSession(5000, 70);
        // Fill projected roster to MAX_PLAYERS without going through recruiting.
        while (session.projectedRosterSize() < simulation.RosterRules.MAX_PLAYERS) {
            String raw = "RB,Pad" + session.teamPlayers.size()
                    + ",1,45,70,75,3,false,false,70,70,70,10,A,B,C,D,72,200,70,F";
            session.teamPlayers.add(RecruitingPlayerRecord.fromRecruitCsv(raw));
        }
        assertFalse(session.canRecruitMore());
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Overflow,1,45,70,75,3,false,false,70,70,70,50,A,B,C,D,72,200,70,F");
        try {
            session.recruitPlayer(recruit, false, 1.0, new Random(1));
            fail("Expected IllegalStateException when roster is full");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("full"));
        }
    }

    @Test
    public void emptyBoardStatus_isExplicit() {
        RecruitingSessionData session = newSession(100, 70);
        assertEquals(0, session.availAll.size());
        assertEquals("Board: no prospects available",
                RecruitingPresentation.buildBoardStatus(session));
        assertTrue(RecruitingPresentation.buildEmptyBoardMessage().toLowerCase().contains("no recruits"));
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
