package recruiting;

import org.junit.Test;

import static org.junit.Assert.*;

public class RecruitingCostTest {

    @Test
    public void applyBudgetBonuses_neverProducesNegativeBudget() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\nEND_TEAM_INFO%\n");
        session.recruitingBudget = 100;
        int before = session.recruitingBudget;
        // minPlayers less than current roster size yields negative recBonus,
        // but Math.max(0, ...) clamps so budget is never reduced.
        session.applyBudgetBonuses(0);
        assertTrue("Budget should never go below initial after applyBudgetBonuses",
                session.recruitingBudget >= before);
    }

    @Test
    public void applyBudgetBonuses_addsPositiveContribution() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\nEND_TEAM_INFO%\n");
        session.recruitingBudget = 100;
        int before = session.recruitingBudget;
        // minPlayers large enough to produce positive bonus
        session.applyBudgetBonuses(50);
        assertTrue("Budget should increase when large minPlayers is given",
                session.recruitingBudget > before);
    }

    @Test
    public void rebuildTopProspects_limitsTo50() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\nEND_TEAM_INFO%\n"
                        + generateRecruits(60));
        assertEquals(60, session.availAll.size());
        assertEquals("rebuildTopProspects should select exactly 50 non-K prospects",
                50, session.avail50.size());
    }

    @Test
    public void rebuildTopProspects_excludesKickers() {
        StringBuilder data = new StringBuilder(
                "Big East,Test U,TST,5,70%\nEND_TEAM_INFO%\n");
        data.append(generateRecruits(25));
        // Add some kickers — should be skipped
        for (int i = 0; i < 10; i++) {
            data.append("K,Kicker").append(i)
                    .append(",1,45,50,50,3,false,false,70,80,70,100,A,B,C,D,72,200,70,F%\n");
        }
        data.append(generateRecruits(30, 100));
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(data.toString());

        assertEquals(50, session.avail50.size());
        boolean hasKicker = session.avail50.stream()
                .anyMatch(r -> r.position().equals("K"));
        assertFalse("avail50 should not contain any kickers", hasKicker);
    }

    @Test
    public void sortBoardsByGrade_sortsTopProspects() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\nEND_TEAM_INFO%\n"
                        + generateRecruits(60));

        assertFalse("avail50 should not be empty", session.avail50.isEmpty());
        for (int i = 0; i < session.avail50.size() - 1; i++) {
            RecruitingPlayerRecord a = session.avail50.get(i);
            RecruitingPlayerRecord b = session.avail50.get(i + 1);
            float gradeA = (4f * a.recruitOverall() + a.potential()) / 5f;
            float gradeB = (4f * b.recruitOverall() + b.potential()) / 5f;
            assertTrue("avail50 should be sorted by scout grade descending; "
                            + "grade[" + i + "]=" + gradeA + " < grade[" + (i + 1) + "]=" + gradeB,
                    gradeA >= gradeB);
        }
    }

    @Test
    public void sortBoardsByGrade_sortsAvailAll() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,70%\nEND_TEAM_INFO%\n"
                        + generateRecruits(30));

        assertFalse(session.availAll.isEmpty());
        for (int i = 0; i < session.availAll.size() - 1; i++) {
            RecruitingPlayerRecord a = session.availAll.get(i);
            RecruitingPlayerRecord b = session.availAll.get(i + 1);
            float gradeA = (4f * a.recruitOverall() + a.potential()) / 5f;
            float gradeB = (4f * b.recruitOverall() + b.potential()) / 5f;
            assertTrue("availAll should be sorted by grade descending", gradeA >= gradeB);
        }
    }

    private static String generateRecruits(int count) {
        return generateRecruits(count, 0);
    }

    private static String generateRecruits(int count, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < startIndex + count; i++) {
            int ovr = 70 + (i % 30);
            int pot = 70 + ((i * 7) % 30);
            String pos = (i % 3 == 0) ? "QB" : (i % 3 == 1) ? "RB" : "WR";
            String[] grades = {"A", "B", "C", "D"};
            sb.append(pos).append(",Recruit").append(i)
                    .append(",1,45,").append(50 + i % 50).append(",").append(50 + i % 50)
                    .append(",").append(3 + i % 3).append(",false,false,")
                    .append(pot).append(",80,").append(ovr)
                    .append(",100,").append(grades[i % 4]).append(",").append(grades[(i + 1) % 4])
                    .append(",").append(grades[(i + 2) % 4]).append(",").append(grades[(i + 3) % 4])
                    .append(",72,200,").append(70 + i % 30).append(",F%\n");
        }
        return sb.toString();
    }
}
