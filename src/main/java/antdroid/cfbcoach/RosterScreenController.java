package antdroid.cfbcoach;

import android.widget.ListView;

import java.util.ArrayList;

import simulation.Team;
import ui.TeamRoster;

final class RosterScreenController {

    private RosterScreenController() {
    }

    static void show(MainActivity activity, ListView mainList, Team currentTeam, int currentWeek) {
        ArrayList<String> roster = currentTeam.getRoster();
        final TeamRoster teamRoster = new TeamRoster(activity, roster, activity, currentWeek);
        mainList.setAdapter(teamRoster);
    }
}
