package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */


public class CompTeamSoW implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamStrengthOfWins() > b.getTeamStrengthOfWins() ? -1 : a.getTeamStrengthOfWins() == b.getTeamStrengthOfWins() ? 0 : 1;
    }
}
