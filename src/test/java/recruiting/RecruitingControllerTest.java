package recruiting;

import org.junit.Test;
import simulation.GameFlowManager;
import simulation.LeagueLaunchCoordinator;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecruitingControllerTest {

    private static final String QB_RECRUIT =
            "QB,Casey Arm,1,45,70,75,5,false,false,90,80,80,40,A,B,C,D,72,200,80,F";
    private static final String RB_RECRUIT =
            "RB,Jordan Burst,1,12,70,75,4,false,false,86,80,78,30,A,B,C,D,70,190,78,F";

    @Test
    public void scoutPlayer_updatesSharedBoardRecords() {
        CapturingFlow flow = new CapturingFlow();
        RecruitingSessionData session = newSession();
        RecruitingController controller = new RecruitingController(session, flow);

        RecruitingPlayerRecord recruit = session.availQBs.get(0);

        assertTrue(controller.scoutPlayer(recruit));

        List<RecruitingPlayerRecord> qbs = controller.getPlayersForPosition(2, "QB (Need: 1)");
        assertTrue(qbs.get(0).raw().endsWith(",T"));
        assertTrue(session.availAll.get(0).raw().endsWith(",T"));
    }

    @Test
    public void recruitPlayer_removesRecruitAndFinishSerializesSignedPlayers() {
        CapturingFlow flow = new CapturingFlow();
        RecruitingSessionData session = newSession();
        RecruitingController controller = new RecruitingController(session, flow);

        RecruitingPlayerRecord recruit = session.availQBs.get(0);

        controller.recruitPlayer(recruit, false);
        controller.finishRecruiting();

        assertFalse(session.availAll.contains(recruit));
        assertFalse(session.availQBs.contains(recruit));
        assertEquals(1, session.playersRecruited.size());
        assertTrue(flow.finishedRecruitingData.contains(QB_RECRUIT));
        assertTrue(flow.finishedRecruitingData.endsWith("END_RECRUITS%\n"));
    }

    private static RecruitingSessionData newSession() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "Big East,Test U,TST,5,75%\n"
                        + "QB,Current Starter,2,88,75,80,A,B,C,D,-,false,false,-,23,70,3,73,220,85,+2%\n"
                        + "END_TEAM_INFO%\n"
                        + QB_RECRUIT + "%\n"
                        + RB_RECRUIT + "%\n");
        session.recruitingBudget = 500;
        return session;
    }

    private static final class CapturingFlow implements GameFlowManager {
        private String finishedRecruitingData = "";

        @Override public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode prestigeMode, String userTeamName) {}
        @Override public void loadGame(String saveData) {}
        @Override public void importSave(String userTeamName) {}
        @Override public void finishRecruiting(String recruitsStr) { finishedRecruitingData = recruitsStr; }
        @Override public void startRecruiting(String userTeamInfo) {}
        @Override public void showNotification(String title, String message) {}
        @Override public void returnToMainHub() {}
    }
}
