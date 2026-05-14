package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;
import simulation.Game;
import simulation.Team;

public class GameScheduleList extends ArrayAdapter<Game> {
    private final Context context;
    private final Game[] games;
    private final Team team;
    private final MainActivity mainAct;

    public GameScheduleList(Context context, MainActivity mainAct, Team team, Game[] games) {
        super(context, R.layout.game_schedule_list_item, games);
        this.context = context;
        this.mainAct = mainAct;
        this.games = games;
        this.team = team;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.game_schedule_list_item, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.gameScheduleLeft);
        Button gameButton = rowView.findViewById(R.id.gameScheduleButtonList);
        Button textRight = rowView.findViewById(R.id.gameScheduleRight);

        String[] gameSummary = team.getGameSummaryStr(position);
        textLeft.setText(valueAt(gameSummary, 0));
        gameButton.setText(valueAt(gameSummary, 1));
        textRight.setText(valueAt(gameSummary, 2));

        if (team.getGameWLSchedule().size() > position) {
            if (team.getGameWLSchedule().get(position).equals("W")) {
                gameButton.setBackgroundResource(R.drawable.bg_action_win);
            } else if (team.getGameWLSchedule().get(position).equals("L")) {
                gameButton.setBackgroundResource(R.drawable.bg_action_danger);
            } else if (team.getGameWLSchedule().get(position).equals("BYE")) {

            }
        }

        gameButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Game game = gameAt(position);
                if (game != null && !game.gameName.equals("BYE WEEK")) {
                    mainAct.showGameDialog(game);
                }
            }
        });

        textRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do something on click
                Game game = gameAt(position);
                if (game != null && !game.gameName.equals("BYE WEEK")) {
                    if (game.awayTeam == team)
                        mainAct.examineTeam(game.homeTeam.getName());
                    else mainAct.examineTeam(game.awayTeam.getName());
                }
            }
        });

        return rowView;
    }

    private Game gameAt(int index) {
        if (games == null || index < 0 || index >= games.length) {
            return null;
        }
        return games[index];
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
