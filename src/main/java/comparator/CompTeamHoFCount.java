package comparator;

import java.util.Comparator;

import simulation.Team;

public class CompTeamHoFCount implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getHoFCount() > b.getHoFCount() ? -1 : a.getHoFCount() == b.getHoFCount() ? 0 : 1;
    }
}

