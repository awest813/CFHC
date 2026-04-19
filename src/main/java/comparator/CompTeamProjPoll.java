package comparator;

import java.util.Comparator;

import simulation.Team;

public class CompTeamProjPoll implements Comparator<Team> {
    @Override
    public int compare(Team a, Team b) {
        if (a.getProjectedPollScore() > b.getProjectedPollScore()) {
            return -1;

        } else if (b.getProjectedPollScore() == a.getProjectedPollScore()) {
            //check for  tiebreaker
            if (a.getTeamOffTalent() + a.getTeamDefTalent() > b.getTeamOffTalent() + b.getTeamDefTalent()) {
                return -1;
            } else if (a.getTeamOffTalent() + a.getTeamDefTalent() < b.getTeamOffTalent() + b.getTeamDefTalent()) {
                return 1;
            } else return 0;

        } else return 1;
    }
}
