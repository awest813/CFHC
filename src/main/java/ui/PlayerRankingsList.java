package ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class PlayerRankingsList extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final MainActivity mainAct;

    public PlayerRankingsList(Context context, ArrayList<String> values, String userTeamStrRep, MainActivity mainAct) {
        super(context, R.layout.team_rankings_list_item, values);
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
            rowView = inflater.inflate(R.layout.team_rankings_list_item, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.textTeamRankingsLeft);
        TextView textCenter = rowView.findViewById(R.id.textTeamRankingsCenter);
        TextView textRight = rowView.findViewById(R.id.textTeamRankingsRight);


        final String[] teamStat = values.get(position).split(",", -1);
        final String rank = valueAt(teamStat, 0);
        final String player = valueAt(teamStat, 1);
        final String team = valueAt(teamStat, 2);
        textLeft.setText(rank);
        textCenter.setText(player + " (" + team + ")");
        textRight.setText(valueAt(teamStat, 3));
        textLeft.setTextColor(Color.parseColor("#B7C6D1"));
        textCenter.setTextColor(Color.parseColor("#F5F7FA"));
        textRight.setTextColor(Color.parseColor("#F4C95D"));
        textLeft.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textCenter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textRight.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        if (team.equals(userTeamStrRep)) {
            // Bold user team
            textLeft.setTypeface(textLeft.getTypeface(), Typeface.BOLD);
            textLeft.setTextColor(Color.parseColor("#5994de"));
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.parseColor("#5994de"));
            textRight.setTypeface(textRight.getTypeface(), Typeface.BOLD);
            textRight.setTextColor(Color.parseColor("#5994de"));
        }

        textCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.examinePlayerandTeam(player, team);
            }
        });

        return rowView;
    }



    public void setUserTeamStrRep(String userTeamStrRep) {
        this.userTeamStrRep = userTeamStrRep;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
