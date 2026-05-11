package ui;

/*
  Created by Achi Jones on 2/20/2016.
 */

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.save_list, parent, false);

        String[] detailSplit = values[position].split(">", -1);
        TextView itemL = rowView.findViewById(R.id.textPlayerStatsLeftChild);
        itemL.setText(valueAt(detailSplit, 0));
        TextView itemR = rowView.findViewById(R.id.textPlayerStatsRightChild);
        String detail = valueAt(detailSplit, 1);
        itemR.setText(detail);

        String[] split = detail.split("\n");

        if (valueAt(split, 1).equals(userTeamStrRep)) {
            itemL.setTextColor(Color.parseColor("#5994de"));
            itemR.setTextColor(Color.parseColor("#5994de"));
        }

        return rowView;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
