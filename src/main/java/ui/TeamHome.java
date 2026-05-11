package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;
import simulation.Game;

public class TeamHome extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final MainActivity mainAct;
    private final Game[] games;
    private int week;

    public TeamHome(Context context, String[] values, MainActivity mainAct,  Game[] games, int week) {
        super(context, R.layout.team_home, values);
        this.context = context;
        this.values = values;
        this.mainAct = mainAct;
        this.games = games;
        this.week = week;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.team_home, parent, false);
        }
        TextView textTeam = rowView.findViewById(R.id.home_teamname);
        TextView textRecord = rowView.findViewById(R.id.home_record);
        TextView textRank = rowView.findViewById(R.id.home_rank);
        TextView textSeason = rowView.findViewById(R.id.home_season_chip);
        TextView textWeek = rowView.findViewById(R.id.home_week_chip);
        TextView textPhase = rowView.findViewById(R.id.home_phase_chip);
        TextView textTeamRatings = rowView.findViewById(R.id.home_teamRatings);
        TextView textInjuries = rowView.findViewById(R.id.home_injuries);
        TextView textSuspensions = rowView.findViewById(R.id.home_suspensions);
        TextView textNextGame = rowView.findViewById(R.id.home_nextgame);
        TextView textNews = rowView.findViewById(R.id.home_news);
        TextView textLastGame = rowView.findViewById(R.id.home_lastgame);

        String[] teamStat = values[position].split("&", -1);
        textTeam.setText(valueAt(teamStat, 0));
        textRank.setText(valueAt(teamStat, 1));
        textRecord.setText(valueAt(teamStat, 2));
        textSeason.setText(mainAct.getSeasonYearChipText());
        textWeek.setText(mainAct.getSeasonWeekChipText());
        textPhase.setText(mainAct.getSeasonPhaseChipText());
        textTeamRatings.setText(valueAt(teamStat, 3));
        textInjuries.setText(valueAt(teamStat, 4));
        textSuspensions.setText(valueAt(teamStat, 5));
        String nextGame = valueAt(teamStat, 6);
        String news = valueAt(teamStat, 7);
        String lastGame = valueAt(teamStat, 8);
        textNextGame.setText(nextGame);
        textNews.setText(news);
        textLastGame.setText(lastGame);

        textRank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.currPage = 5;
                mainAct.updateRankings();
            }
        });

        textRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.currPage = 5;
                mainAct.updateStandings();
            }
        });

        textTeamRatings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.currPage = 3;
                mainAct.updateTeamStats();
            }
        });

        textInjuries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.currPage = 1;
                mainAct.viewRoster();
            }
        });

        textSuspensions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.currPage = 1;
                mainAct.viewRoster();
            }
        });

        if(!nextGame.contains("Bye") && !nextGame.contains("End of Season") ) {
            textNextGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int gameIndex = safeGameIndex(week);
                    if (gameIndex >= 0) {
                        mainAct.showGameDialog(games[gameIndex]);
                    }

                }
            });
        }

        if(!lastGame.contains("Bye") && !lastGame.contains("No Game") ) {
            textLastGame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int gameIndex = safeGameIndex(week - 1);
                    if (gameIndex >= 0) {
                        mainAct.showGameDialog(games[gameIndex]);
                    }
                }
            });
        }

        textNews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.showNewsStoriesDialog();
            }
        });


        return rowView;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    private int safeGameIndex(int index) {
        if (games == null || games.length == 0) {
            return -1;
        }
        if (index < 0) {
            return -1;
        }
        return Math.min(index, games.length - 1);
    }
}
