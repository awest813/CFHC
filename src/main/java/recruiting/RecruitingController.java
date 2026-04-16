package recruiting;

import android.widget.ExpandableListView;
import simulation.GameFlowManager;
import java.util.Random;

/**
 * Platform-agnostic controller for managing the recruiting process logic.
 */
public final class RecruitingController {

    private final RecruitingSessionData sessionData;
    private final GameFlowManager flowManager;
    private final Random random = new Random();

    public RecruitingController(RecruitingSessionData sessionData, GameFlowManager flowManager) {
        this.sessionData = sessionData;
        this.flowManager = flowManager;
    }

    public RecruitingSessionData getSessionData() {
        return sessionData;
    }

    public void recruitPlayer(String playerCsv, boolean autoFilter) {
        sessionData.recruitPlayer(playerCsv, autoFilter, sessionData.recruitOffBoard, random);
    }

    public boolean scoutPlayer(String playerCsv) {
        return sessionData.scoutPlayer(playerCsv);
    }

    public void finishRecruiting() {
        flowManager.finishRecruiting(sessionData.buildRecruitsSaveData());
    }

    public void sortByGrade() {
        sessionData.sortBoardsByGrade();
    }

    public void sortByCost() {
        sessionData.sortBoardsByCost();
    }
}
