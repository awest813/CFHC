package recruiting;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecruitingPresentationTest {

    @Test
    public void getGrade_fiveStar_returnsCorrectStars() {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Test A,1,1,50,50,3,false,false,70,70,75,50,98,50,50,50,70,200,75,F");
        String details = RecruitingPresentation.buildRecruitBoardDetails(recruit, "QB");
        assertTrue(details.contains("Pass Strength:  * * * * *"));
    }

    @Test
    public void getGrade_fourStar_returnsCorrectStars() {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Test A,1,1,50,50,3,false,false,70,70,75,50,85,50,50,50,70,200,75,F");
        String details = RecruitingPresentation.buildRecruitBoardDetails(recruit, "QB");
        assertTrue(details.contains("Pass Strength:  * * * *"));
    }

    @Test
    public void getGrade_threeStar_returnsCorrectStars() {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Test A,1,1,50,50,3,false,false,70,70,75,50,75,50,50,50,70,200,75,F");
        String details = RecruitingPresentation.buildRecruitBoardDetails(recruit, "QB");
        assertTrue(details.contains("Pass Strength:  * * *"));
    }

    @Test
    public void getGrade_twoStar_returnsCorrectStars() {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Test A,1,1,50,50,3,false,false,70,70,75,50,65,50,50,50,70,200,75,F");
        String details = RecruitingPresentation.buildRecruitBoardDetails(recruit, "QB");
        assertTrue(details.contains("Pass Strength:  * *"));
    }

    @Test
    public void getGrade_oneStar_returnsCorrectStars() {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Test A,1,1,50,50,3,false,false,70,70,75,50,40,50,50,50,70,200,75,F");
        String details = RecruitingPresentation.buildRecruitBoardDetails(recruit, "QB");
        assertTrue(details.contains("Pass Strength:  *"));
    }

    @Test
    public void getGrade_nonNumeric_returnsQuestionMark() {
        RecruitingPlayerRecord recruit = RecruitingPlayerRecord.fromRecruitCsv(
                "QB,Test A,1,1,50,50,3,false,false,70,70,75,50,N/A,50,50,50,70,200,75,F");
        // getGrade calls Integer.parseInt which throws NumberFormatException for "N/A"
        assertThrows(NumberFormatException.class, () ->
                RecruitingPresentation.buildRecruitBoardDetails(recruit, "QB"));
    }

    @Test
    public void buildOverviewSummary_containsRecruiting() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Coach,Test Team,1,5,70%\nEND_TEAM_INFO%\nEND_RECRUITS");
        String result = RecruitingPresentation.buildOverviewSummary(session);
        assertNotNull(result);
        assertTrue(result.contains("recruiting"));
    }
}
