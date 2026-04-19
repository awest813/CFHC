package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */


public class CompTeamOPYPG implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return (float) a.getTeamOppPassYards() / a.numGames() < (float) b.getTeamOppPassYards() / b.numGames() ? -1 : (float) a.getTeamOppPassYards() / a.numGames() == (float) b.getTeamOppPassYards() / b.numGames() ? 0 : 1;
    }
}
