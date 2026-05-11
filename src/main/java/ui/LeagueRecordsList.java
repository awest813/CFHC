package ui;

/*
  Created by Achi Jones on 3/29/2016.
 */

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import antdroid.cfbcoach.R;

public class LeagueRecordsList extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final String userTeamAbbr;
    private final String userTeamName;

    public LeagueRecordsList(Context context, String[] values, String userTeamAbbr, String userTeamName) {
        super(context, R.layout.league_record_list_item, values);
        this.context = context;
        this.values = values;
        this.userTeamAbbr = userTeamAbbr;
        this.userTeamName = userTeamName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.league_record_list_item, parent, false);
        TextView textLeft = rowView.findViewById(R.id.textLeagueRecordLeft);
        TextView textCenter = rowView.findViewById(R.id.textLeagueRecordCenter);
        TextView textRight = rowView.findViewById(R.id.textLeagueRecordRight);

        String[] record = values[position].split(",", -1);
        String name = valueAt(record, 0);
        String value = valueAt(record, 1);
        String holder = valueAt(record, 2);
        String year = valueAt(record, 3);
        if (value.equals("-1")) {
            textLeft.setText("");
            textCenter.setText(name);
            textCenter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            textRight.setText("");
        } else if (!holder.equals("XXX")) {
            // Only show record if it exists
            textLeft.setText(value);
            textCenter.setText(name);
            if (holder.contains("%")) {
                String[] nameSplit = holder.split("%", -1);
                String team = valueAt(nameSplit, 1);
                textRight.setText(valueAt(nameSplit, 0) + "\n" + team + " " + year);
                if (team.equals(userTeamAbbr) || team.equals(userTeamName)) {
                    // User team record, make it special color
                    textRight.setTextColor(Color.parseColor("#5994de"));
                }
            } else {
                textRight.setText(holder + "\n" + year);
                String[] holderParts = holder.split(" ");
                String team = valueAt(holderParts, 0);
                if (team.equals(userTeamAbbr) || team.equals(userTeamName)) {
                    // User team record, make it special color
                    textRight.setTextColor(Color.parseColor("#5994de"));
                }
            }
        }

        return rowView;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
