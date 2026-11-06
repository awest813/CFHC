package antdroid.cfbcoach;

import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

import simulation.Team;
import ui.TeamRoster;

final class RosterScreenController {

    private RosterScreenController() {
    }

    static void show(MainActivity activity, RecyclerView mainList, Team currentTeam, int currentWeek) {
        ArrayList<String> roster = currentTeam.getRoster();
        final TeamRoster teamRoster = new TeamRoster(activity, roster, activity, currentWeek);
        mainList.setAdapter(teamRoster);
    }
}
