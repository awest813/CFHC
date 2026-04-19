package comparator;

import java.util.Comparator;

import simulation.Team;

public class CompTeamDisciplineScore implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
            return a.getTeamDisciplineScore() > b.getTeamDisciplineScore() ? -1 : a.getTeamDisciplineScore() == b.getTeamDisciplineScore() ? 0 : 1;
    }
}
