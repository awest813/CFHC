package antdroid.cfbcoach;

import android.widget.ListView;

import simulation.Team;
import ui.TeamStatsList;

final class TeamStatsScreenController {

    private TeamStatsScreenController() {
    }

    static void show(MainActivity activity, ListView mainList, Team currentTeam) {
        String[] teamStatsStr = currentTeam.getTeamStatsStrCSV().split("%\n");
        mainList.setAdapter(new TeamStatsList(activity, teamStatsStr));
    }
}
