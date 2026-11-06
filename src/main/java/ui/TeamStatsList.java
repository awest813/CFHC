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
        textCenter.setText(row.value());
        textRight.setText(row.rank());

        return;
    }

}
