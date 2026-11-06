package ui;

/*
  Created by Achi Jones on 3/29/2016.
 */

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

public class LeagueRecordsList extends RowAdapter<String> {
    private final String[] values;
    private final String userTeamAbbr;
    private final String userTeamName;

    public LeagueRecordsList(Context context, String[] values, String userTeamAbbr, String userTeamName) {
        super(context, R.layout.league_record_list_item, values);
        this.values = values;
        this.userTeamAbbr = userTeamAbbr;
        this.userTeamName = userTeamName;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView textLeft = holder.view(R.id.textLeagueRecordLeft);
        TextView textCenter = holder.view(R.id.textLeagueRecordCenter);
        TextView textRight = holder.view(R.id.textLeagueRecordRight);

        String[] record = values[position].split(",", -1);
        String name = valueAt(record, 0);
        String value = valueAt(record, 1);
        String recordHolder = valueAt(record, 2);
        String year = valueAt(record, 3);
        // Reset recycled-row styling to the HUD row defaults.
        textCenter.setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
        textCenter.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.cf_text_body));
        textRight.setTextColor(ContextCompat.getColor(context, R.color.cf_text_secondary));
        if (value.equals("-1")) {
            // Section header line: gold title-size label, no value / recordHolder
            textLeft.setText("");
            textCenter.setText(name);
            textCenter.setTextColor(ContextCompat.getColor(context, R.color.cf_gold));
            textCenter.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    context.getResources().getDimension(R.dimen.cf_text_title));
            textRight.setText("");
        } else if (!recordHolder.equals("XXX")) {
            // Only show record if it exists
            textLeft.setText(value);
            textCenter.setText(name);
            if (recordHolder.contains("%")) {
                String[] nameSplit = recordHolder.split("%", -1);
                String team = valueAt(nameSplit, 1);
                textRight.setText(valueAt(nameSplit, 0) + "\n" + team + " " + year);
                if (team.equals(userTeamAbbr) || team.equals(userTeamName)) {
                    // User team record, make it special color
                    textRight.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
                }
            } else {
                textRight.setText(recordHolder + "\n" + year);
                String[] holderParts = recordHolder.split(" ");
                String team = valueAt(holderParts, 0);
                if (team.equals(userTeamAbbr) || team.equals(userTeamName)) {
                    // User team record, make it special color
                    textRight.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
                }
            }
        }

        return;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
