package antdroid.cfbcoach;

import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;

import simulation.League;
import simulation.Team;
import ui.MainRankings;

final class StandingsScreenController {

    private StandingsScreenController() {
    }

    static void showStandings(MainActivity activity, RecyclerView mainList, League simLeague, Team userTeam) {
        ArrayList<String> standings = simLeague.getConfStandings();
        final MainRankings teamRankings = new MainRankings(activity, standings, userTeam.getName(), activity);
        ui.ListRowsAdapter.show(mainList, teamRankings);
    }

    static void showRankings(MainActivity activity, RecyclerView mainList, League simLeague, Team userTeam) {
        ArrayList<String> standings = simLeague.getTeamRankings();
        final MainRankings teamRankings = new MainRankings(activity, standings, userTeam.getName(), activity);
        ui.ListRowsAdapter.show(mainList, teamRankings);
    }
}
