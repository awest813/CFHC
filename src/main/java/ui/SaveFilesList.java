package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import antdroid.cfbcoach.R;

public class SaveFilesList extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;

    public SaveFilesList(Context context, String[] values) {
        super(context, R.layout.save_list, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.save_list, parent, false);
        }

        TextView itemL = rowView.findViewById(R.id.textPlayerStatsLeftChild);
        TextView itemC = rowView.findViewById(R.id.textPlayerStatsCenter);
        TextView itemR = rowView.findViewById(R.id.textPlayerStatsRightChild);

        String[] lines = values[position].split("\\n");
        itemL.setText(lines[0]);

        if (lines.length > 1) {
            itemC.setVisibility(View.VISIBLE);
            itemC.setText(lines[1]);
        } else {
            itemC.setVisibility(View.GONE);
            itemC.setText("");
        }

        if (lines.length > 2) {
            itemR.setVisibility(View.VISIBLE);
            itemR.setText(lines[2]);
        } else {
            itemR.setVisibility(View.GONE);
            itemR.setText("");
        }

        return rowView;
    }
}
