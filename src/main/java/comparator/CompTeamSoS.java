package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamSoS implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamSOS() > b.getTeamSOS() ? -1 : a.getTeamSOS() == b.getTeamSOS() ? 0 : 1;
    }
}
