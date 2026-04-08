package ui;

/*
  Created by Achi Jones on 2/20/2016.
 */

        import android.content.Context;
        import android.graphics.Color;
        import android.graphics.Typeface;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ArrayAdapter;
        import android.widget.TextView;

        import java.util.ArrayList;

        import antdroid.cfbcoach.R;

public class TeamRankingsList extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;

    public TeamRankingsList(Context context, ArrayList<String> values, String userTeamStrRep) {
        super(context, R.layout.team_rankings_list_item, values);
        this.context = context;
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
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


        String[] teamStat = values.get(position).split(",");
        textLeft.setText(teamStat[0]);
        textCenter.setText(teamStat[1]);
        textRight.setText(teamStat[2]);
        textLeft.setTextColor(Color.parseColor("#B7C6D1"));
        textCenter.setTextColor(Color.parseColor("#F5F7FA"));
        textRight.setTextColor(Color.parseColor("#F4C95D"));
        textLeft.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textCenter.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textRight.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        if (teamStat[1].equals(userTeamStrRep)) {
            // Bold user team
            textLeft.setTypeface(textLeft.getTypeface(), Typeface.BOLD);
            textLeft.setTextColor(Color.parseColor("#5994de"));
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.parseColor("#5994de"));
            textRight.setTypeface(textRight.getTypeface(), Typeface.BOLD);
            textRight.setTextColor(Color.parseColor("#5994de"));
        }
        if (teamStat[2].split(" ").length > 1 && teamStat[2].split(" ")[2].contains("+")) {
            // Highlight Prestige Changes in off-season
            textRight.setTextColor(Color.parseColor("#00b300"));
        } else if (teamStat[2].split(" ").length > 1 && teamStat[2].split(" ")[2].contains("-")) {
            textRight.setTextColor(Color.RED);
        }

        return rowView;
    }

    public void setUserTeamStrRep(String userTeamStrRep) {
        this.userTeamStrRep = userTeamStrRep;
    }
}
