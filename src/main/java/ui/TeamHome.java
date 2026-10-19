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
import simulation.Team;

public class TeamHome extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final MainActivity mainAct;
    private final Game[] games;
    private final Team team;
    private int week;

    public TeamHome(Context context, String[] values, MainActivity mainAct,  Game[] games, int week) {
        this(context, values, mainAct, games, week, null);
    }

    public TeamHome(Context context, String[] values, MainActivity mainAct, Game[] games, int week, Team team) {
        super(context, R.layout.team_home, values);
        this.context = context;
        this.values = values;
        this.mainAct = mainAct;
        this.games = games;
        this.week = week;
        this.team = team;
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
        TextView textNickname = rowView.findViewById(R.id.home_nickname);
        TextView textOverall = rowView.findViewById(R.id.home_overall);
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
        bindIdentity(textNickname, textOverall);
        textRank.setText(team != null ? rankLabel(team) : valueAt(teamStat, 1));
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
        textNextGame.setText(stripUnrankedMarks(nextGame));
        textNews.setText(news);
        textLastGame.setText(lastGame);

        textRank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.updateRankings();
            }
        });

        textRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.updateStandings();
            }
        });

        textTeamRatings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.updateTeamStats();
            }
        });

        textInjuries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.viewRoster();
            }
        });

        textSuspensions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    /** Top 25 as #12. The engine rank is the full league size until a team is ranked. */
    private String rankLabel(Team current) {
        int rank = current.getRankTeamPollScore();
        if (rank >= 1 && rank <= 25) {
            return context.getString(R.string.page_poll_rank, rank);
        }
        return context.getString(R.string.page_unranked);
    }

    /**
     * Opponent lines from the engine always append {@code #rank}. Drop marks outside the top 25
     * so an unranked opponent reads as {@code vs Alabama} rather than {@code vs #130 Alabama}.
     */
    static String stripUnrankedMarks(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '#' && i + 1 < text.length() && Character.isDigit(text.charAt(i + 1))) {
                int j = i + 1;
                int rank = 0;
                while (j < text.length() && Character.isDigit(text.charAt(j)) && rank < 1000) {
                    rank = rank * 10 + (text.charAt(j) - '0');
                    j++;
                }
                if (rank >= 1 && rank <= 25) {
                    out.append(text, i, j);
                } else if (j < text.length() && text.charAt(j) == ' ') {
                    j++;
                }
                i = j - 1;
                continue;
            }
            out.append(text.charAt(i));
        }
        return out.toString();
    }

    /** Script nickname and the same off/def overall the desktop card shows. */
    private void bindIdentity(TextView nicknameView, TextView overallView) {
        if (nicknameView != null) {
            String nickname = team != null && team.nickname != null ? team.nickname.trim() : "";
            nicknameView.setText(nickname);
            nicknameView.setVisibility(nickname.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (overallView != null) {
            if (team == null) {
                overallView.setText("");
            } else {
                int overall = (int) ((team.teamOffTalent + team.teamDefTalent) / 2f);
                overallView.setText(String.valueOf(overall));
                overallView.setContentDescription(
                        context.getString(R.string.hub_a11y_overall) + " " + overall);
            }
        }
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
