package comparator;

import java.util.Comparator;

import simulation.Team;

public class CompTeamChemistry  implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        // Each score is computed once per side; same result as calling it per test.
        double sa = a.getTeamChemistry();
        double sb = b.getTeamChemistry();
        return sa > sb ? -1 : sa == sb ? 0 : 1;
    }
}
