package ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class PlayerRankingsList extends RowAdapter<String> {
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final MainActivity mainAct;
    private final Typeface interFont;
    private final Typeface monoFont;

    public PlayerRankingsList(Context context, ArrayList<String> values, String userTeamStrRep, MainActivity mainAct) {
        super(context, R.layout.team_rankings_list_item, values);
        this.interFont = ResourcesCompat.getFont(context, R.font.inter);
        this.monoFont = ResourcesCompat.getFont(context, R.font.jetbrains_mono);
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
        this.mainAct = mainAct;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView textLeft = holder.view(R.id.textTeamRankingsLeft);
        TextView textCenter = holder.view(R.id.textTeamRankingsCenter);
        TextView textRight = holder.view(R.id.textTeamRankingsRight);


        final String[] teamStat = values.get(position).split(",", -1);
        final String rank = valueAt(teamStat, 0);
        final String player = valueAt(teamStat, 1);
        final String team = valueAt(teamStat, 2);
        textLeft.setText(rank);
        textCenter.setText(player + " (" + team + ")");
        textRight.setText(valueAt(teamStat, 3));
        // Reset recycled-row styling to the HUD row defaults (gold rank pill, Inter name, mono stat).
        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        textLeft.setTextColor(color(R.color.cf_gold));
        textCenter.setTextColor(color(R.color.cf_text_primary));
        textRight.setTextColor(color(R.color.cf_text_primary));
        textLeft.setTypeface(monoFont, Typeface.BOLD);
        textCenter.setTypeface(interFont, Typeface.BOLD);
        textRight.setTypeface(monoFont, Typeface.BOLD);

        if (team.equals(userTeamStrRep)) {
            // User team: emerald leading bar + emerald text
            rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            textCenter.setTextColor(color(R.color.cf_emerald));
            textRight.setTextColor(color(R.color.cf_emerald));
        }

        textCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.examinePlayerandTeam(player, team);
            }
        });

        return;
    }



    public void setUserTeamStrRep(String userTeamStrRep) {
        this.userTeamStrRep = userTeamStrRep;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
