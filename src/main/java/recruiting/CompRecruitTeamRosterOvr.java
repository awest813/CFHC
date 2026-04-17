package recruiting;

import java.util.Comparator;

class CompRecruitTeamRosterOvr implements Comparator<RecruitingPlayerRecord> {
    @Override
    public int compare(RecruitingPlayerRecord a, RecruitingPlayerRecord b) {
        int ovrA = a.currentOverall();
        int ovrB = b.currentOverall();
        return Integer.compare(ovrB, ovrA);
    }
}

