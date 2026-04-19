package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamOPPG implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return (float) a.getTeamOppPoints() / a.numGames() < (float) b.getTeamOppPoints() / b.numGames() ? -1 : (float) a.getTeamOppPoints() / a.numGames() == (float) b.getTeamOppPoints() / b.numGames() ? 0 : 1;
    }
}
