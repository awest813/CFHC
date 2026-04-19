package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamPoll implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        if (a.getTeamPollScore() > b.getTeamPollScore()) {
            return -1;

        } else if (b.getTeamPollScore() == a.getTeamPollScore()) {
            //check for  tiebreaker
            if (a.getTeamStrengthOfWins() > b.getTeamStrengthOfWins()) {
                return -1;
            } else if (a.getTeamStrengthOfWins() < b.getTeamStrengthOfWins()) {
                return 1;
            } else return 0;

        } else return 1;
    }
}
