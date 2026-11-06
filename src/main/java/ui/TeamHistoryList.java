package ui;

/*
  Created by Achi Jones on 4/17/2016.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

import simulation.TeamHistoryRecord;

public class TeamHistoryList extends RowAdapter<TeamHistoryRecord> {
    private final TeamHistoryRecord[] values;

    public TeamHistoryList(Context context, TeamHistoryRecord[] values) {
        super(context, R.layout.team_history_list_item, values);
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
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView textTop = holder.view(R.id.textViewTeamHistoryTitle);
        TextView textBottom = holder.view(R.id.textViewTeamHistoryDetail);

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

        return;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }
}
