package antdroid.cfbcoach;

import android.widget.ListView;

import java.util.ArrayList;

import simulation.League;
import simulation.Team;
import ui.MainRankings;

final class StandingsScreenController {

    private StandingsScreenController() {
    }

    static void showStandings(MainActivity activity, ListView mainList, League simLeague, Team userTeam) {
        ArrayList<String> standings = simLeague.getConfStandings();
        final MainRankings teamRankings = new MainRankings(activity, standings, userTeam.getName(), activity);
        mainList.setAdapter(teamRankings);
    }

    static void showRankings(MainActivity activity, ListView mainList, League simLeague, Team userTeam) {
        ArrayList<String> standings = simLeague.getTeamRankings();
        final MainRankings teamRankings = new MainRankings(activity, standings, userTeam.getName(), activity);
        mainList.setAdapter(teamRankings);
    }
}
