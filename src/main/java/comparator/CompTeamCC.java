package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by Anthony on 12/30/2017.
 */

public class CompTeamCC implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        return a.getTotalCCs() > b.getTotalCCs() ? -1 : a.getTotalCCs() == b.getTotalCCs() ? 0 : 1;
    }
}
