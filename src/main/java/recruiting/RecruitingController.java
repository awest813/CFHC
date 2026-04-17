package recruiting;

import simulation.GameFlowManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public void recruitPlayer(RecruitingPlayerRecord recruit, boolean autoFilter) {
        sessionData.recruitPlayer(recruit, autoFilter, sessionData.recruitOffBoard, random);
    }

    public boolean scoutPlayer(RecruitingPlayerRecord recruit) {
        return sessionData.scoutPlayer(recruit);
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

    public List<RecruitingPlayerRecord> getPlayersForPosition(int positionIndex, String currentPositionLabel) {
        if (positionIndex > 1 && positionIndex < 12) {
            String[] splitty = currentPositionLabel.split(" ");
            return getPlayersByPos(splitty[0]);
        } else {
            if (positionIndex == 0) return sessionData.avail50;
            if (positionIndex == 12) return sessionData.west;
            if (positionIndex == 13) return sessionData.midwest;
            if (positionIndex == 14) return sessionData.central;
            if (positionIndex == 15) return sessionData.east;
            if (positionIndex == 16) return sessionData.south;
            return sessionData.availAll;
        }
    }

    private List<RecruitingPlayerRecord> getPlayersByPos(String pos) {
        if (pos.equals("QB")) return sessionData.availQBs;
        if (pos.equals("RB")) return sessionData.availRBs;
        if (pos.equals("WR")) return sessionData.availWRs;
        if (pos.equals("TE")) return sessionData.availTEs;
        if (pos.equals("OL")) return sessionData.availOLs;
        if (pos.equals("K")) return sessionData.availKs;
        if (pos.equals("DL")) return sessionData.availDLs;
        if (pos.equals("LB")) return sessionData.availLBs;
        if (pos.equals("CB")) return sessionData.availCBs;
        if (pos.equals("S")) return sessionData.availSs;
        return sessionData.availAll;
    }

    public Map<String, List<String>> buildPlayersInfoMap(List<RecruitingPlayerRecord> players, int positionIndex, String currentPositionLabel) {
        Map<String, List<String>> playersInfo = new LinkedHashMap<>();
        String posLabel = currentPositionLabel.split(" ")[0];

        for (RecruitingPlayerRecord record : players) {
            ArrayList<String> pInfoList = new ArrayList<>();
            
            String targetPos;
            if (positionIndex > 1 && positionIndex < 12) {
                targetPos = posLabel;
            } else {
                targetPos = record.position();
            }
            
            pInfoList.add(RecruitingPresentation.buildRecruitBoardDetails(record, targetPos));
            playersInfo.put(record.listKey(), pInfoList);
        }
        return playersInfo;
    }
}

