package ui;

/*
  Created by ahngu on 9/29/2017.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;

public class IndividualStats extends RowAdapter<String> {
    private final ArrayList<String> values;
    private final MainActivity mainact;

    public IndividualStats(Context context, ArrayList<String> values, MainActivity mainact) {
        super(context, R.layout.individual_stats, values);
        this.values = values;
        this.mainact = mainact;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;


        TextView textPlayer = holder.view(R.id.textName);
        TextView[] stats = new TextView[] {
                holder.view(R.id.textStat0),
                holder.view(R.id.textStat1),
                holder.view(R.id.textStat2),
                holder.view(R.id.textStat3),
                holder.view(R.id.textStat4),
                holder.view(R.id.textStat5)
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
        final boolean header = isCategoryHeader(playerName, valueAt(teamStat, 1).trim());
        if (playerName.isEmpty() && !anyStat) {
            rowView.setVisibility(View.GONE);
            setRowHeight(rowView, 0);
            textPlayer.setOnClickListener(null);
            return;
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

        return;
    }

    /**
     * Category banners in the player-stat list. The following cells are the column labels.
     * A real player row starts with a number, so a player who shares a category name still renders as a player.
     */
    private static boolean isCategoryHeader(String name, String firstStat) {
        boolean category = "Passing".equals(name) || "Rushing".equals(name) || "Receiving".equals(name)
                || "Defense".equals(name) || "Kicking".equals(name);
        if (!category) return false;
        return firstStat.isEmpty() || !firstStat.matches("[-+]?\\d.*");
    }


    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
