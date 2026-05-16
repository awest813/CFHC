package antdroid.cfbcoach;

import android.widget.ListView;

import simulation.Game;
import simulation.League;
import simulation.Team;
import ui.TeamHome;

final class HomeScreenController {

    private HomeScreenController() {
    }

    static void show(MainActivity activity, ListView mainList, Team currentTeam, League simLeague) {
        String[] teamStatsStr = currentTeam.getTeamHomeInfo().split("!!");

        Game[] games = new Game[currentTeam.getGameSchedule().size()];
        for (int i = 0; i < games.length; ++i) {
            games[i] = currentTeam.getGameSchedule().get(i);
        }
        int week = simLeague.currentWeek;

        mainList.setAdapter(new TeamHome(activity, teamStatsStr, activity, games, week));
    }
}
