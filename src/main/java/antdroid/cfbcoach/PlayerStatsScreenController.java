package antdroid.cfbcoach;

import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

import simulation.Team;
import ui.IndividualStats;

final class PlayerStatsScreenController {

    private PlayerStatsScreenController() {
    }

    static void show(MainActivity activity, RecyclerView mainList, Team currentTeam) {
        ArrayList<String> players = currentTeam.getRosterStats();
        final IndividualStats playersStats = new IndividualStats(activity, players, activity);
        mainList.setAdapter(playersStats);
    }
}
