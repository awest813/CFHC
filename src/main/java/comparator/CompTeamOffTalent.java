package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamOffTalent implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamOffTalent() > b.getTeamOffTalent() ? -1 : a.getTeamOffTalent() == b.getTeamOffTalent() ? 0 : 1;
    }
}
