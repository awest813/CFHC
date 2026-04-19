package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamPYPG implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return (float) a.getTeamPassYards() / a.numGames() > (float) b.getTeamPassYards() / b.numGames() ? -1 : (float) a.getTeamPassYards() / a.numGames() == (float) b.getTeamPassYards() / b.numGames() ? 0 : 1;
    }
}
