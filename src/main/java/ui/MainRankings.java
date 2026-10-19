package ui;

/*
  Created by ahngu on 9/29/2017.
 */

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class MainRankings extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final MainActivity mainAct;


    public MainRankings(Context context, ArrayList<String> values, String userTeamStrRep, MainActivity mainAct) {
        super(context, R.layout.team_stats_list_item, values);
        this.context = context;
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
        this.mainAct = mainAct;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.rankings_main, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.textRankLeft);
        final TextView textCenter = rowView.findViewById(R.id.textRankCenter);
        TextView textRecord = rowView.findViewById(R.id.textRankRecord);
        TextView textRight = rowView.findViewById(R.id.textRankRight);

        final String[] teamStat = values.get(position).split(",", -1);
        final String rank = valueAt(teamStat, 0).trim();
        final String team = valueAt(teamStat, 1).trim();
        final String record = valueAt(teamStat, 2).trim();
        final String stat = valueAt(teamStat, 3).trim();
        final boolean spacer = rank.isEmpty() && team.isEmpty();
        final boolean section = rank.isEmpty() && !team.isEmpty();

        if (spacer) {
            // Blank line the engine inserts between conferences.
            rowView.setVisibility(View.GONE);
            setRowHeight(rowView, 0);
            textCenter.setOnClickListener(null);
            return rowView;
        }

        rowView.setVisibility(View.VISIBLE);
        setRowHeight(rowView, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLeft.setText(rank);
        textCenter.setText(team);
        textCenter.setAllCaps(section);
        if (textRecord != null) {
            textRecord.setText(record);
            textRecord.setVisibility(record.isEmpty() ? View.GONE : View.VISIBLE);
        }
        textRight.setText(stat);
        textRight.setVisibility(stat.isEmpty() ? View.GONE : View.VISIBLE);

        boolean userRow = isUserTeam(team);
        int weight = userRow ? Typeface.BOLD : Typeface.NORMAL;
        textLeft.setTypeface(textLeft.getTypeface(), weight);
        textCenter.setTypeface(textCenter.getTypeface(), weight);
        textRight.setTypeface(textRight.getTypeface(), weight);
        if (textRecord != null) {
            textRecord.setTypeface(textRecord.getTypeface(), weight);
        }

        if (section) {
            rowView.setBackgroundResource(R.drawable.bg_section_bar);
            textLeft.setVisibility(View.GONE);
            textCenter.setTextColor(color(R.color.cf_gold));
            textCenter.setOnClickListener(null);
            textCenter.setClickable(false);
        } else {
            textLeft.setVisibility(View.VISIBLE);
            textLeft.setTextColor(color(R.color.cf_gold));
            if (userRow) {
                rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
                textCenter.setTextColor(color(R.color.cf_emerald));
                textRight.setTextColor(color(R.color.cf_emerald));
                if (textRecord != null) {
                    textRecord.setTextColor(color(R.color.cf_emerald));
                }
            } else {
                rowView.setBackgroundResource(R.drawable.bg_cf_card);
                textCenter.setTextColor(color(R.color.cf_text_primary));
                textRight.setTextColor(color(R.color.cf_text_primary));
                if (textRecord != null) {
                    textRecord.setTextColor(color(R.color.cf_text_secondary));
                }
            }
            final String lookup = bareTeamName(team);
            textCenter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainAct.examineTeam(lookup);
                }
            });
        }

        return rowView;
    }

    /** Ranked standings prefix the poll rank (`#12 Alabama`); the lookup name does not. */
    static String bareTeamName(String team) {
        if (team == null) {
            return "";
        }
        String trimmed = team.trim();
        if (trimmed.startsWith("#")) {
            int space = trimmed.indexOf(' ');
            if (space >= 0 && space + 1 < trimmed.length()) {
                return trimmed.substring(space + 1).trim();
            }
        }
        return trimmed;
    }

    private boolean isUserTeam(String teamField) {
        return userTeamStrRep != null && !userTeamStrRep.isEmpty()
                && bareTeamName(teamField).equals(userTeamStrRep);
    }

    private static void setRowHeight(View rowView, int height) {
        ViewGroup.LayoutParams lp = rowView.getLayoutParams();
        if (lp == null) {
            rowView.setLayoutParams(new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height));
            return;
        }
        if (lp.height != height) {
            lp.height = height;
            rowView.requestLayout();
        }
    }

    public void setUserTeamStrRep(String userTeamStrRep) {
        this.userTeamStrRep = userTeamStrRep;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
