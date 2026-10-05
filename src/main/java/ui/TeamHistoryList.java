package ui;

/*
  Created by Achi Jones on 4/17/2016.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

import simulation.TeamHistoryRecord;

public class TeamHistoryList extends ArrayAdapter<TeamHistoryRecord> {
    private final Context context;
    private final TeamHistoryRecord[] values;

    public TeamHistoryList(Context context, TeamHistoryRecord[] values) {
        super(context, R.layout.team_history_list_item, values);

        this.context = context;
        this.values = values;
    }

    public TeamHistoryList(Context context, String[] values) {
        this(context, toRecords(values));
    }

    private static TeamHistoryRecord[] toRecords(String[] values) {
        TeamHistoryRecord[] records = new TeamHistoryRecord[values.length];
        for (int i = 0; i < values.length; i++) {
            records[i] = TeamHistoryRecord.fromCsv(values[i]);
        }
        return records;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.team_history_list_item, parent, false);
        }
        TextView textTop = rowView.findViewById(R.id.textViewTeamHistoryTitle);
        TextView textBottom = rowView.findViewById(R.id.textViewTeamHistoryDetail);

        // Reset recycled-row styling to the HUD row defaults.
        textTop.setTextColor(color(R.color.cf_text_primary));
        textBottom.setVisibility(View.VISIBLE);

        String summary = values[position].summary();
        String[] teamHist = summary.split(">");
        if (teamHist.length > 1) {
            textTop.setText(teamHist[0]);
            String[] yearSplit = teamHist[0].split(" ");
            boolean wonNC = false, wonCC = false, wonB = false;
            for (String s : yearSplit) {
                if (s.equals("NCW")) wonNC = true;
                else if (s.equals("CC")) wonCC = true;
                else if (s.equals("BW")) wonB = true;
            }

            if (wonNC) textTop.setTextColor(color(R.color.cf_gold));
            else if (wonCC) textTop.setTextColor(color(R.color.cf_positive));
            else if (wonB) textTop.setTextColor(color(R.color.cf_info));

            String detail = "";
            for (int i = 1; i < teamHist.length; ++i) {
                detail += teamHist[i];
                if (i != teamHist.length - 1) detail += "\n";
            }
            textBottom.setText(detail);
        } else {
            textTop.setText(summary);

            String[] yearSplit = summary.split(" ");

            boolean wonNC = false, wonCC = false, wonB = false;
            for (String s : yearSplit) {
                if (s.equals("NCW")) wonNC = true;
                else if (s.equals("CC")) wonCC = true;
                else if (s.equals("BW")) wonB = true;
            }

            if (wonNC) textTop.setTextColor(color(R.color.cf_gold));
            else if (wonCC) textTop.setTextColor(color(R.color.cf_positive));
            else if (wonB) textTop.setTextColor(color(R.color.cf_info));

            textBottom.setVisibility(View.GONE);
        }

        return rowView;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }
}
