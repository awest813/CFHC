package comparator;

import java.util.Comparator;

import simulation.Team;

public class CompTeamFacilities implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamFacilities() > b.getTeamFacilities() ? -1 : a.getTeamFacilities() == b.getTeamFacilities() ? 0 : 1;
    }
}