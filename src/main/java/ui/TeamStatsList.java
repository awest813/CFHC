package ui;

/*
  Created by Achi Jones on 2/20/2016.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import antdroid.cfbcoach.R;

public class TeamStatsList extends RowAdapter<String> {
    private final String[] values;

    public TeamStatsList(Context context, String[] values) {
        super(context, R.layout.team_stats_list_item, values);
        this.values = values;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;
        TextView textLeft = holder.view(R.id.textTeamStatsLeft);
        TextView textCenter = holder.view(R.id.textTeamStatsCenter);
        TextView textRight = holder.view(R.id.textTeamStatsRight);

        simulation.rows.TeamStatRow row = simulation.rows.TeamStatRow.parse(values[position]);
        // Engine order is value, label, rank. The row reads label, then the number, then the rank.
        textLeft.setText(row.label());
        textCenter.setText(groupDigits(row.value()));
        textRight.setText(row.rank());

        return;
    }

    /** "34350" reads as "34,350" (locale grouping); decimals, records and ranks pass through. */
    static String groupDigits(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (!v.matches("-?\\d{4,}")) return value;
        try {
            return java.text.NumberFormat.getIntegerInstance().format(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return value;
        }
    }

}
