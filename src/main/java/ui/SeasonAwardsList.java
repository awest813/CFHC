package ui;

/*
  Created by Achi Jones on 4/18/2016.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

public class SeasonAwardsList extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final String userTeamAbbr;

    public SeasonAwardsList(Context context, String[] values, String userTeamAbbr) {
        super(context, R.layout.league_history_list_item, values);
        this.context = context;
        this.values = values;
        this.userTeamAbbr = userTeamAbbr;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.league_history_list_item, parent, false);
        }
        TextView textTop = rowView.findViewById(R.id.textViewLeagueHistoryTop);
        TextView textMiddle = rowView.findViewById(R.id.textViewLeagueHistoryMiddle);
        TextView textBottom = rowView.findViewById(R.id.textViewLeagueHistoryBottom);

        // Reset recycled-row styling to the HUD row defaults.
        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        textTop.setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
        textBottom.setVisibility(View.VISIBLE);
        String[] player = values[position].split("\n");
        if (player.length == 3) {
            textTop.setText(player[0]);
            textMiddle.setText(player[1]);
            textBottom.setText(player[2]);
            if (firstWord(player[0]).equals(userTeamAbbr)) {
                // highlight user team players
                textTop.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
                rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            }
        } else if (player.length == 2) {
            textTop.setText(player[0]);
            textMiddle.setText(player[1]);
            textBottom.setVisibility(View.GONE);
            if (firstWord(player[0]).equals(userTeamAbbr)) {
                // highlight user team players
                textTop.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
                rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            }
        } else {
            textMiddle.setText(values[position]);
        }

        return rowView;
    }

    private static String firstWord(String value) {
        String[] parts = value.split(" ");
        return parts.length > 0 ? parts[0] : "";
    }
}
