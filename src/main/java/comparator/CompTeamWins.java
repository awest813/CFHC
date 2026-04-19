package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by Anthony on 1/1/2018.
 */

public class CompTeamWins implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        if (a.league.currentWeek > 15)
            return a.getTotalWins() > b.getTotalWins() ? -1 : a.getTotalWins() == b.getTotalWins() ? 0 : 1;
        else
            return a.getTotalWins() + a.getWins() > b.getTotalWins() + b.getWins() ? -1 : a.getTotalWins() + a.getWins() == b.getTotalWins() + b.getWins() ? 0 : 1;
    }
}