package antdroid.cfbcoach;

import android.widget.ListView;

import simulation.Game;
import simulation.Team;
import ui.GameScheduleList;

final class ScheduleScreenController {

    private ScheduleScreenController() {
    }

    static void show(MainActivity activity, ListView mainList, Team currentTeam) {
        Game[] games = new Game[currentTeam.getGameSchedule().size()];
        for (int i = 0; i < games.length; ++i) {
            games[i] = currentTeam.getGameSchedule().get(i);
        }
        mainList.setAdapter(new GameScheduleList(activity, activity, currentTeam, games));
        mainList.setSelection(Math.max(0, currentTeam.numGames() - 3));
    }
}
