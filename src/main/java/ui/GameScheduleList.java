package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

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

        Game game = gameAt(position);
        String[] gameSummary = team.getGameSummaryStr(position);
        textLeft.setText(game == null ? valueAt(gameSummary, 0) : weekLabel(game, position));
        gameButton.setText(valueAt(gameSummary, 1));
        textRight.setText(game == null ? valueAt(gameSummary, 2) : opponentLabel(game));

        boolean upcoming = position == nextUnplayedIndex();
        rowView.setBackgroundResource(upcoming ? R.drawable.bg_cf_card_accent : R.drawable.bg_cf_card);

        // Recycled rows: start from the neutral secondary fill, then apply win/loss.
        gameButton.setBackgroundResource(R.drawable.bg_action_secondary);
        gameButton.setTextColor(color(R.color.cf_text_primary));
        if (team.getGameWLSchedule().size() > position) {
            if (team.getGameWLSchedule().get(position).equals("W")) {
                gameButton.setBackgroundResource(R.drawable.bg_action_win);
                gameButton.setTextColor(color(R.color.cf_on_emerald));
            } else if (team.getGameWLSchedule().get(position).equals("L")) {
                gameButton.setBackgroundResource(R.drawable.bg_action_danger);
                gameButton.setTextColor(color(R.color.cf_on_crimson));
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

    /** First game that has not been played, including a bye that is this week's slot. */
    private int nextUnplayedIndex() {
        if (games == null) return -1;
        for (int i = 0; i < games.length; i++) {
            Game game = games[i];
            if (game != null && !game.hasPlayed) return i;
        }
        return -1;
    }

    /** Week and site, instead of the engine's "Conference" / "OOC" token. */
    private String weekLabel(Game game, int position) {
        if (game.isByeWeek()) return context.getString(R.string.page_schedule_bye);
        String site = context.getString(game.homeTeam == team
                ? R.string.page_schedule_home : R.string.page_schedule_away);
        if (game.isRegularSeasonSlot()) {
            return context.getString(R.string.page_schedule_week, position + 1) + "\n" + site;
        }
        return game.gameName + "\n" + site;
    }

    /** Opponent with a poll rank only when they are in the top 25. */
    private String opponentLabel(Game game) {
        if (game.isByeWeek() || game.homeTeam == null || game.awayTeam == null) return "";
        boolean home = game.homeTeam == team;
        Team opponent = home ? game.awayTeam : game.homeTeam;
        int rank = opponent.rankTeamPollScore;
        if (rank >= 1 && rank <= 25) {
            return context.getString(home ? R.string.page_schedule_vs_ranked : R.string.page_schedule_at_ranked,
                    rank, opponent.getName());
        }
        return context.getString(home ? R.string.page_schedule_vs : R.string.page_schedule_at, opponent.getName());
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
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
