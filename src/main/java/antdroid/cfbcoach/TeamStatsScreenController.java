package antdroid.cfbcoach;

import androidx.recyclerview.widget.RecyclerView;


import simulation.Team;
import ui.TeamStatsList;

final class TeamStatsScreenController {

    private TeamStatsScreenController() {
    }

    static void show(MainActivity activity, RecyclerView mainList, Team currentTeam) {
        String[] teamStatsStr = currentTeam.getTeamStatsStrCSV().split("%\n");
        ui.ListRowsAdapter.show(mainList, new TeamStatsList(activity, teamStatsStr));
    }
}
