package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamOYPG implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTeamOppYards() / a.numGames() < b.getTeamOppYards() / b.numGames() ? -1 : a.getTeamOppYards() / a.numGames() == b.getTeamOppYards() / b.numGames() ? 0 : 1;
    }
}
