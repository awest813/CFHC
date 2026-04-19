package comparator;

import java.util.Comparator;

import simulation.Team;

public class CompTeamBudget implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamBudget() > b.getTeamBudget() ? -1 : a.getTeamBudget() == b.getTeamBudget() ? 0 : 1;
    }
}

