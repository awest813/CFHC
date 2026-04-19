package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/14/2017.
 */

class CompTeamDiscipline implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamDiscipline() > b.getTeamDiscipline() ? -1 : a.getTeamDiscipline() == b.getTeamDiscipline() ? 0 : 1;
    }
}

