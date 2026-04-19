package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamRPI implements Comparator<Team> {
    @Override

    public int compare(Team a, Team b) {
        return a.getTeamRPI() > b.getTeamRPI() ? -1 : a.getTeamRPI() == b.getTeamRPI() ? 0 : 1;
    }
}
