package recruiting;

import java.util.Comparator;

class CompRecruitCost implements Comparator<RecruitingPlayerRecord> {
    @Override
    public int compare(RecruitingPlayerRecord a, RecruitingPlayerRecord b) {
        int costA = a.cost();
        int costB = b.cost();
        return Integer.compare(costB, costA);
    }
}
