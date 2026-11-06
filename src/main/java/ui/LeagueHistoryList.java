package ui;

/*
  Created by Achi Jones on 3/29/2016.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

public class LeagueHistoryList extends RowAdapter<String> {
    private final String[] values;
    private final String userTeamAbbr;

    public LeagueHistoryList(Context context, String[] values, String userTeamAbbr) {
        super(context, R.layout.league_history_list_item, values);
        this.values = values;
        this.userTeamAbbr = userTeamAbbr;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView textTop = holder.view(R.id.textViewLeagueHistoryTop);
        TextView textMiddle = holder.view(R.id.textViewLeagueHistoryMiddle);
        TextView textBottom = holder.view(R.id.textViewLeagueHistoryBottom);
        // Reset recycled-row colours to the HUD defaults before applying user-team accents.
        textMiddle.setTextColor(ContextCompat.getColor(context, R.color.cf_text_secondary));
        textBottom.setTextColor(ContextCompat.getColor(context, R.color.cf_text_secondary));
        String[] record = values[position].split("\n");
        if (record.length == 3) {
            textTop.setText(record[0]);
            textMiddle.setText(record[1]);
            textBottom.setText(record[2]);
            if (wordAt(record[1], 1).equals(userTeamAbbr)) {
                // User team won NCG, make it special color
                textMiddle.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            }
            if (wordAt(record[2], 5).equals(userTeamAbbr)) {
                // User team won POTY, make it special color
                textBottom.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            }
        } else if (record.length == 4) {
            textTop.setText(record[0]);
            textMiddle.setText(record[1]);
            textBottom.setText(record[2] + "\n" + record[3]);
            if (wordAt(record[1], 1).equals(userTeamAbbr)) {
                // User team won NCG, make it special color
                textMiddle.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            }
            if (wordAt(record[2], 5).equals(userTeamAbbr)) {
                // User team won POTY, make it special color
                textBottom.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            }
            if (wordAt(record[3], 5).equals(userTeamAbbr)) {
                // User team won POTY, make it special color
                textBottom.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            }
        } else if (record.length > 0) {
            textTop.setText(record[0]);
            textMiddle.setText("");
            textBottom.setText("");
        }

        return;
    }

    private static String wordAt(String value, int index) {
        String[] parts = value.split(" ");
        return index < parts.length ? parts[index] : "";
    }
}
