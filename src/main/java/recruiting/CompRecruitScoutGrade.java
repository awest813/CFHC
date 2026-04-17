package recruiting;

import java.util.Comparator;

class CompRecruitScoutGrade implements Comparator<RecruitingPlayerRecord> {
    @Override
    public int compare(RecruitingPlayerRecord a, RecruitingPlayerRecord b) {
        float ovrA = (4 * a.recruitOverall() + a.potential()) / 5.0f;
        float ovrB = (4 * b.recruitOverall() + b.potential()) / 5.0f;
        return Float.compare(ovrB, ovrA);
    }
}