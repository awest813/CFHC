package ui;

/*
  Created by Achi Jones on 2/20/2016.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

public class MockDraft extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final String userTeamStrRep;

    public MockDraft(Context context, String[] values, String userTeamStrRep) {
        super(context, R.layout.save_list, values);
        this.context = context;
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.save_list, parent, false);
        }

        String[] detailSplit = values[position].split(">", -1);
        TextView itemL = rowView.findViewById(R.id.textPlayerStatsLeftChild);
        itemL.setText(valueAt(detailSplit, 0));
        TextView itemR = rowView.findViewById(R.id.textPlayerStatsRightChild);
        String detail = valueAt(detailSplit, 1);
        itemR.setText(detail);

        String[] split = detail.split("\n");

        // Reset recycled-row styling, then accent the user's team.
        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        itemL.setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
        itemR.setTextColor(ContextCompat.getColor(context, R.color.cf_gold));
        if (valueAt(split, 1).equals(userTeamStrRep)) {
            rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            itemL.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            itemR.setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
        }

        return rowView;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
