package ui;

/*
  Created by ahngu on 9/29/2017.
 */

import android.content.Context;
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

public class IndividualStats extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private final MainActivity mainact;

    public IndividualStats(Context context, ArrayList<String> values, MainActivity mainact) {
        super(context, R.layout.individual_stats, values);
        this.context = context;
        this.values = values;
        this.mainact = mainact;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.individual_stats, parent, false);
        }


        TextView textPlayer = rowView.findViewById(R.id.textName);
        TextView[] stats = new TextView[] {
                rowView.findViewById(R.id.textStat0),
                rowView.findViewById(R.id.textStat1),
                rowView.findViewById(R.id.textStat2),
                rowView.findViewById(R.id.textStat3),
                rowView.findViewById(R.id.textStat4),
                rowView.findViewById(R.id.textStat5)
        };

        final String[] teamStat = values.get(position).split(",", -1);
        final String playerName = valueAt(teamStat, 0).trim();
        boolean anyStat = false;
        for (int i = 0; i < stats.length; i++) {
            if (!valueAt(teamStat, i + 1).trim().isEmpty()) {
                anyStat = true;
                break;
            }
        }
        final boolean header = isCategoryHeader(playerName);
        if (playerName.isEmpty() && !anyStat) {
            rowView.setVisibility(View.GONE);
            setRowHeight(rowView, 0);
            textPlayer.setOnClickListener(null);
            return rowView;
        }

        rowView.setVisibility(View.VISIBLE);
        setRowHeight(rowView, ViewGroup.LayoutParams.WRAP_CONTENT);
        textPlayer.setText(playerName);
        textPlayer.setAllCaps(header);
        if (header) {
            rowView.setBackgroundResource(R.drawable.bg_section_bar);
            textPlayer.setTextColor(ContextCompat.getColor(context, R.color.cf_gold));
            textPlayer.setOnClickListener(null);
            textPlayer.setClickable(false);
        } else {
            rowView.setBackgroundResource(R.drawable.bg_cf_card);
            textPlayer.setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
            textPlayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainact.examinePlayer(playerName);
                }
            });
        }

        for (int i = 0; i < stats.length; i++) {
            String value = valueAt(teamStat, i + 1).trim();
            stats[i].setText(value);
            stats[i].setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
            boolean accent = !header && i == stats.length - 1 && !value.isEmpty();
            if (header) {
                stats[i].setBackgroundResource(R.drawable.bg_cf_card_canvas);
                stats[i].setTextColor(ContextCompat.getColor(context, R.color.cf_text_muted));
            } else if (accent) {
                stats[i].setBackgroundResource(R.drawable.bg_cf_rating_tile);
                stats[i].setTextColor(ContextCompat.getColor(context, R.color.cf_emerald));
            } else {
                stats[i].setBackgroundResource(R.drawable.bg_cf_card_canvas);
                stats[i].setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
            }
        }

        return rowView;
    }

    /** Category banners in the player-stat list. The following cells are the column labels. */
    private static boolean isCategoryHeader(String name) {
        return "Passing".equals(name) || "Rushing".equals(name) || "Receiving".equals(name)
                || "Defense".equals(name) || "Kicking".equals(name);
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

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
