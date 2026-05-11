package ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class CoachDatabase  extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userHC;
    private final MainActivity mainAct;
    private ArrayList<String> userNames;


    public CoachDatabase(Context context, ArrayList<String> values, String userHC, MainActivity mainAct, ArrayList<String> userNames) {
        super(context, R.layout.team_rankings_list_item, values);
        this.context = context;
        this.values = values;
        this.userHC = userHC;
        this.mainAct = mainAct;
        this.userNames = userNames;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.team_rankings_list_item, parent, false);
        TextView textLeft = rowView.findViewById(R.id.textTeamRankingsLeft);
        TextView textCenter = rowView.findViewById(R.id.textTeamRankingsCenter);
        TextView textRight = rowView.findViewById(R.id.textTeamRankingsRight);

        final String[] teamStat = values.get(position).split(",", -1);
        final String left = valueAt(teamStat, 0);
        final String center = valueAt(teamStat, 1);
        final String right = valueAt(teamStat, 2);
        textLeft.setText(left);
        textCenter.setText(center);
        textRight.setText(right);


        if (!center.contains("[U]") && !center.contains("[R]")) {
            // Bold user team
            textCenter.setTextColor(Color.parseColor("#5994de"));
            textRight.setTextColor(Color.parseColor("#5994de"));
        }
        if (center.contains("[R]")) {
            // Bold user team
            textCenter.setTextColor(Color.GRAY);
            textRight.setTextColor(Color.GRAY);
        }

        if(userNames != null && userNames.contains(center)) {
            textLeft.setTextColor(Color.parseColor("#B68044"));
            textCenter.setTextColor(Color.parseColor("#B68044"));
            textRight.setTextColor(Color.parseColor("#B68044"));
        }


        if (center.equals(userHC)) {
            // Bold user team
            textLeft.setTypeface(textLeft.getTypeface(), Typeface.BOLD);
            textLeft.setTextColor(Color.parseColor("#ff9933"));
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.parseColor("#ff9933"));
            textRight.setTypeface(textRight.getTypeface(), Typeface.BOLD);
            textRight.setTextColor(Color.parseColor("#ff9933"));
        }

        String[] rightParts = right.split(" ");
        if (rightParts.length > 2 && rightParts[2].contains("+")) {
            // Highlight Prestige Changes in off-season
            textRight.setTextColor(Color.GREEN);
        } else if (rightParts.length > 2 && rightParts[2].contains("-")) {
            textRight.setTextColor(Color.RED);
        }


        textCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.examineCoachDB(center);
            }
        });


        return rowView;
    }



    public void setupUserHC(String userHC) {
        this.userHC = userHC;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
