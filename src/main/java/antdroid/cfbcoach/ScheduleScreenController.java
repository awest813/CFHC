package antdroid.cfbcoach;

import androidx.recyclerview.widget.RecyclerView;


import simulation.Game;
import simulation.Team;
import ui.GameScheduleList;

final class ScheduleScreenController {

    private ScheduleScreenController() {
    }

    static void show(MainActivity activity, RecyclerView mainList, Team currentTeam) {
        Game[] games = new Game[currentTeam.getGameSchedule().size()];
        for (int i = 0; i < games.length; ++i) {
            games[i] = currentTeam.getGameSchedule().get(i);
        }
        ui.ListRowsAdapter.show(mainList, new GameScheduleList(activity, activity, currentTeam, games));
        mainList.scrollToPosition(Math.max(0, currentTeam.numGames() - 3));
    }
}
