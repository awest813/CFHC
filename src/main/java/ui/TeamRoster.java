package ui;

/*
  Created by ahngu on 9/29/2017.
 */

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class TeamRoster extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final MainActivity mainAct;
    private final int week;
    private final Typeface interFont;


    public TeamRoster(Context context, ArrayList<String> values, MainActivity mainAct, int week) {
        super(context, R.layout.team_roster, values);
        this.context = context;
        this.interFont = ResourcesCompat.getFont(context, R.font.inter);
        this.values = values;
        this.mainAct = mainAct;
        this.week = week;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.team_roster, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.textTeamRosterLeft);
        TextView textClass = rowView.findViewById(R.id.textTeamRosterClass);
        final TextView textCenter = rowView.findViewById(R.id.textTeamRosterCenter);
        TextView textRight = rowView.findViewById(R.id.textTeamRosterRight);
        TextView textProg = rowView.findViewById(R.id.textTeamRosterProgression);

        final String[] teamStat = values.get(position).split(",", -1);
        final String role = valueAt(teamStat, 0);
        final String playerClass = valueAt(teamStat, 1);
        final String name = valueAt(teamStat, 2);
        String status = valueAt(teamStat, 3);
        final String ratingText = valueAt(teamStat, 4);
        textLeft.setText(role);
        textClass.setText(playerClass);
        textCenter.setText(name + " " + status);
        textRight.setText(ratingText);
        // Reset recycled-row styling to the HUD row defaults (tokens only, Inter kept).
        textLeft.setTextColor(color(R.color.cf_text_secondary));
        textClass.setTextColor(color(R.color.cf_text_secondary));
        textCenter.setTextColor(color(R.color.cf_text_primary));
        textRight.setTextColor(color(R.color.cf_text_primary));
        textProg.setTextColor(color(R.color.cf_gold));
        textCenter.setPaintFlags(textCenter.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        textClass.setPaintFlags(textClass.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        textCenter.setTypeface(interFont, Typeface.BOLD);
        textClass.setTypeface(interFont, Typeface.BOLD);
        textLeft.setVisibility(View.VISIBLE);
        textClass.setVisibility(View.VISIBLE);

        if (role.equals(" ")) {
            // Position-group header line: no POS/YR pills, gold section label.
            textLeft.setVisibility(View.INVISIBLE);
            textClass.setVisibility(View.INVISIBLE);
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_gold));
        }
        if (status.equals("*")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
        }
        if (status.contains("RS") || status.contains("[T]")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_text_muted));
        }
        if (status.contains("Suspended")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_negative));
        }
        if (status.contains("INJ")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_warning));
        }
        if (status.contains("Hot Seat")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_negative));
        }

        if(!ratingText.contains(" ")) {
            try {
                int rating = Integer.parseInt(ratingText);
                if (rating > 90) {
                    textRight.setTextColor(color(R.color.cf_emerald));
                } else if (rating > 80) {
                    textRight.setTextColor(color(R.color.cf_positive));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if(teamStat.length > 5) {
            if (teamStat[5].equals("1")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(color(R.color.cf_positive));
                status = " :  All-Fr";
                textCenter.setText(name + " " + status);
            } else if (teamStat[5].equals("2")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(color(R.color.cf_green));
                status = " :  All-Conf";
                textCenter.setText(name + " " + status);
            } else if (teamStat[5].equals("3")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(color(R.color.cf_info));
                status = " :  All-Am";
                textCenter.setText(name + " " + status);
            } else if (teamStat[5].equals("4")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(color(R.color.cf_gold));
                if(role.contains("HC")) status = " :  COTY";
                else status = " :  POTY";
                textCenter.setText(name + " " + status);
            }
        }

        if(week > 17 && week < 22 && playerClass.contains("Sr")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.ITALIC);
            textCenter.setTextColor(color(R.color.cf_text_muted));
            textCenter.setPaintFlags(textCenter.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textClass.setTypeface(textCenter.getTypeface(), Typeface.ITALIC);
            textClass.setTextColor(color(R.color.cf_text_muted));
            textClass.setPaintFlags(textClass.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        if(teamStat.length > 6) {
            textProg.setText(teamStat[6]);
        } else {
            textProg.setText("");
        }


        View.OnClickListener rowClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (role.equals("HC") || role.equals("OC") || role.equals("DC")) {
                    mainAct.examineCoachDB(name);
                } else {
                    mainAct.examinePlayer(name);
                }
            }
        };
        textCenter.setOnClickListener(rowClickListener);
        rowView.setOnClickListener(rowClickListener);

        return rowView;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

}

