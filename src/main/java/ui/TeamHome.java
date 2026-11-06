package ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;
import simulation.Game;
import simulation.Team;

public class TeamHome extends RowAdapter<String> {
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
        this.values = values;
        this.mainAct = mainAct;
        this.games = games;
        this.week = week;
        this.team = team;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView textTeam = holder.view(R.id.home_teamname);
        TextView textNickname = holder.view(R.id.home_nickname);
        TextView textOverall = holder.view(R.id.home_overall);
        TextView textRecord = holder.view(R.id.home_record);
        TextView textRank = holder.view(R.id.home_rank);
        TextView textSeason = holder.view(R.id.home_season_chip);
        TextView textWeek = holder.view(R.id.home_week_chip);
        TextView textPhase = holder.view(R.id.home_phase_chip);
        TextView textTeamRatings = holder.view(R.id.home_teamRatings);
        TextView textInjuries = holder.view(R.id.home_injuries);
        TextView textSuspensions = holder.view(R.id.home_suspensions);
        TextView textNextGame = holder.view(R.id.home_nextgame);
        TextView textNews = holder.view(R.id.home_news);
        TextView textLastGame = holder.view(R.id.home_lastgame);

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
        bindMorale(rowView);
        bindProgram(rowView);

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


        return;
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

    /** Locker Room card: status word, the three morale values, and the factor lines. */
    private void bindMorale(View rowView) {
        View card = rowView.findViewById(R.id.home_morale_card);
        if (card == null) return;
        if (team == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        simulation.TeamMoraleSnapshot snap = team.getTeamMoraleSnapshot();
        TextView status = rowView.findViewById(R.id.home_morale_status);
        status.setText(snap.label());
        int tone = snap.tone();
        status.setTextColor(androidx.core.content.ContextCompat.getColor(context,
                tone > 0 ? R.color.cf_positive : (tone == 0 ? R.color.cf_warning : R.color.cf_negative)));
        ((TextView) rowView.findViewById(R.id.home_morale_values)).setText(context.getString(
                R.string.dash_morale_values, snap.chemistry(), snap.leadership(), snap.buyIn()));
        int streak = team.getWinStreak() != null ? team.getWinStreak().getStreakLength() : 0;
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        for (simulation.TeamMoraleSnapshot.Factor f : snap.factors(streak)) {
            if (sb.length() > 0) sb.append('\n');
            int start = sb.length();
            sb.append(f.text());
            sb.setSpan(new android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(context,
                    f.positive() ? R.color.cf_positive : R.color.cf_warning)),
                    start, sb.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ((TextView) rowView.findViewById(R.id.home_morale_factors)).setText(sb);
    }

    /** Program card: prestige trend, finances and the incoming class. */
    private void bindProgram(View rowView) {
        View card = rowView.findViewById(R.id.home_program_card);
        if (card == null) return;
        if (team == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        simulation.ProgramSnapshot p = simulation.ProgramSnapshot.of(team);
        TextView trend = rowView.findViewById(R.id.home_program_trend);
        int label;
        int color;
        switch (p.trend()) {
            case RISING -> { label = R.string.dash_program_rising; color = R.color.cf_positive; }
            case DECLINING -> { label = R.string.dash_program_declining; color = R.color.cf_negative; }
            default -> { label = R.string.dash_program_stable; color = R.color.cf_text_secondary; }
        }
        trend.setText(label);
        trend.setTextColor(androidx.core.content.ContextCompat.getColor(context, color));
        ((TextView) rowView.findViewById(R.id.home_program_body)).setText(context.getString(R.string.dash_program_body,
                p.prestige(), p.prestigeDelta(), simulation.ProgramSnapshot.formatMoney(p.budget()),
                p.nilTier(), simulation.League.NIL_MAX_TIER, p.facilities(), p.discipline(),
                p.commits(), p.classRating()));
    }
}
