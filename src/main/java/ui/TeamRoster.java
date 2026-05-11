package ui;

/*
  Created by ahngu on 9/29/2017.
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class TeamRoster extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final MainActivity mainAct;
    private final int week;


    public TeamRoster(Context context, ArrayList<String> values, MainActivity mainAct, int week) {
        super(context, R.layout.team_roster, values);
        this.context = context;
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
        textLeft.setTextColor(Color.parseColor("#B7C6D1"));
        textClass.setTextColor(Color.parseColor("#B7C6D1"));
        textCenter.setTextColor(Color.parseColor("#F5F7FA"));
        textRight.setTextColor(Color.parseColor("#F5F7FA"));
        textProg.setTextColor(Color.parseColor("#F4C95D"));
        textCenter.setPaintFlags(textCenter.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        textClass.setPaintFlags(textClass.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        textCenter.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        textClass.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);

        if (role.equals(" ")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.parseColor("#5994de"));
        }
        if (status.equals("*")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            //textCenter.setTextColor(Color.WHITE);
        }
        if (status.contains("RS") || status.contains("[T]")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.DKGRAY);
        }
        if (status.contains("Suspended")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.RED);
        }
        if (status.contains("INJ")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.parseColor("#ff9933"));
        }
        if (status.contains("Hot Seat")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(Color.RED);
        }

        if(!ratingText.contains(" ")) {
            try {
                int rating = Integer.parseInt(ratingText);
                if (rating > 90) {
                    textRight.setTextColor(Color.parseColor("#5994de"));
                } else if (rating > 80) {
                    textRight.setTextColor(Color.parseColor("#8FBC8F"));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if(teamStat.length > 5) {
            if (teamStat[5].equals("1")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(Color.parseColor("#8FBC8F"));
                status = " :  All-Fr";
                textCenter.setText(name + " " + status);
            } else if (teamStat[5].equals("2")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(Color.parseColor("#00B300"));
                status = " :  All-Conf";
                textCenter.setText(name + " " + status);
            } else if (teamStat[5].equals("3")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(Color.parseColor("#1A75FF"));
                status = " :  All-Am";
                textCenter.setText(name + " " + status);
            } else if (teamStat[5].equals("4")) {
                textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
                textCenter.setTextColor(Color.parseColor("#FF9933"));
                if(role.contains("HC")) status = " :  COTY";
                else status = " :  POTY";
                textCenter.setText(name + " " + status);
            }
        }

        if(week > 17 && week < 22 && playerClass.contains("Sr")) {
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.ITALIC);
            textCenter.setTextColor(Color.DKGRAY);
            textCenter.setPaintFlags(textCenter.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textClass.setTypeface(textCenter.getTypeface(), Typeface.ITALIC);
            textClass.setTextColor(Color.DKGRAY);
            textClass.setPaintFlags(textClass.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        //if(week > 5 && week < 8 && teamStat.length > 6 || week > 21 && teamStat.length > 6) {
        if(teamStat.length > 6) {
            textProg.setText(teamStat[6]);
        } else {
            textProg.setText("");
        }


        textCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(role.equals("HC") || role.equals("OC") || role.equals("DC") ) mainAct.examineCoachDB(name);
                else mainAct.examinePlayer(name);
            }
        });

        return rowView;
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

}

