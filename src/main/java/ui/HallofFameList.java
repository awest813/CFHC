package ui;

/*
  Created by Achi Jones on 3/29/2016.
 */

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

import simulation.PlayerRecord;

public class HallofFameList extends ArrayAdapter<PlayerRecord> {
    private final Context context;
    private final PlayerRecord[] values;
    private final String userTeam;
    private final MainActivity mainAct;
    private final boolean team;

    public HallofFameList(Context context, PlayerRecord[] values, String userTeam, boolean team, MainActivity mainAct) {
        super(context, R.layout.hall_fame_list_item, values);

        this.context = context;
        this.values = values;
        this.userTeam = userTeam;
        this.team = team;
        this.mainAct = mainAct;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.hall_fame_list_item, parent, false);
        TextView textTop = rowView.findViewById(R.id.textViewHallFameName);

        PlayerRecord record = values[position];
        String nameStr = record.position() + " " + record.name();
        String teamNameStr = record.teamName();
        String entry = nameStr + ", " + teamNameStr + " [" + record.year() + "]\n"; // Simple version for now
        String entryTeam = nameStr + " [" + record.year() + "]\n";

        // Try to get old format for examineHOF if needed
        final String oldFormat = simulation.Persistence.toCsv(record);

        if(team) textTop.setText(entryTeam);
        else textTop.setText(entry);

        if (record.teamName().equals(userTeam)) {
            textTop.setTextColor(Color.parseColor("#5994de"));
        }

        textTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.examineHOF(oldFormat);
            }
        });



        return rowView;
    }
}
