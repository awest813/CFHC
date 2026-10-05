package ui;

/*
  Created by ahngu on 9/29/2017.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class MainRankings extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final MainActivity mainAct;


    public MainRankings(Context context, ArrayList<String> values, String userTeamStrRep, MainActivity mainAct) {
        super(context, R.layout.team_stats_list_item, values);
        this.context = context;
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
        this.mainAct = mainAct;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.rankings_main, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.textRankLeft);
        final TextView textCenter = rowView.findViewById(R.id.textRankCenter);
        TextView textRight = rowView.findViewById(R.id.textRankRight);

        final String[] teamStat = values.get(position).split(",", -1);
        final String rank = valueAt(teamStat, 0);
        final String team = valueAt(teamStat, 1);
        final String record = valueAt(teamStat, 2);
        textLeft.setText(rank);
        textCenter.setText(team + " " + record);
        textRight.setText(valueAt(teamStat, 3));

        // Reset recycled-row styling to the HUD row defaults.
        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        textLeft.setVisibility(View.VISIBLE);
        textLeft.setTextColor(color(R.color.cf_gold));
        textCenter.setTextColor(color(R.color.cf_text_primary));
        textRight.setTextColor(color(R.color.cf_text_primary));

        if (team.equals(userTeamStrRep)) {
            // User team: emerald leading bar + emerald text
            rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            textLeft.setTypeface(textLeft.getTypeface(), Typeface.BOLD);
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_emerald));
            textRight.setTypeface(textRight.getTypeface(), Typeface.BOLD);
            textRight.setTextColor(color(R.color.cf_emerald));
        }
        if (rank.equals(" ")) {
            // Section header line (conference / division): no rank pill, gold label
            textLeft.setVisibility(View.INVISIBLE);
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_gold));
        }

        textCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.examineTeam(team);
            }
        });

        return rowView;
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
