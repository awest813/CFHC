package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamDefTalent implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamDefTalent() > b.getTeamDefTalent() ? -1 : a.getTeamDefTalent() == b.getTeamDefTalent() ? 0 : 1;
    }
}
