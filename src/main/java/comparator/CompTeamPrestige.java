package comparator;

import java.util.Comparator;

import simulation.Team;

/**
 * Created by ahngu on 11/13/2017.
 */

public class CompTeamPrestige implements Comparator<Team> {
    @Override

    public int compare(Team a, Team b) {
        if (a.getTeamPrestige() > b.getTeamPrestige()) {
            return -1;

        } else if (b.getTeamPrestige() == a.getTeamPrestige()) {
            //check for  tiebreaker
            if (a.getConfPrestige() > b.getConfPrestige()) {
                return -1;
            } else if (a.getConfPrestige() < b.getConfPrestige()) {
                return 1;
            } else {
                if (a.getTeamOffTalent() + a.getTeamDefTalent() > b.getTeamOffTalent() + b.getTeamDefTalent()) {
                    return -1;
                } else if (a.getTeamOffTalent() + a.getTeamDefTalent() < b.getTeamOffTalent() + b.getTeamDefTalent()) {
                    return 1;
                } else return 0;
            }

        } else return 1;
    }
}
