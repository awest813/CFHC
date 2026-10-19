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
import android.widget.AbsListView;
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
        final String role = valueAt(teamStat, 0).trim();
        final String playerClass = valueAt(teamStat, 1).trim();
        final String name = valueAt(teamStat, 2).trim();
        final String status = valueAt(teamStat, 3).trim();
        final String ratingText = valueAt(teamStat, 4).trim();
        final String awardCode = valueAt(teamStat, 5).trim();
        final String progression = valueAt(teamStat, 6).trim();
        TextView textStatus = rowView.findViewById(R.id.textTeamRosterStatus);
        final boolean spacer = role.isEmpty() && name.isEmpty();
        final boolean section = role.isEmpty() && !name.isEmpty();

        if (spacer) {
            rowView.setVisibility(View.GONE);
            setRowHeight(rowView, 0);
            textCenter.setOnClickListener(null);
            rowView.setOnClickListener(null);
            return rowView;
        }

        rowView.setVisibility(View.VISIBLE);
        setRowHeight(rowView, ViewGroup.LayoutParams.WRAP_CONTENT);
        textCenter.setText(name);
        textCenter.setAllCaps(section);
        textCenter.setPaintFlags(textCenter.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        textClass.setPaintFlags(textClass.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        textCenter.setTypeface(interFont, "*".equals(status) || section ? Typeface.BOLD : Typeface.NORMAL);
        textClass.setTypeface(interFont, Typeface.BOLD);

        if (section) {
            rowView.setBackgroundResource(R.drawable.bg_section_bar);
            textLeft.setVisibility(View.GONE);
            textClass.setVisibility(View.GONE);
            textRight.setVisibility(View.GONE);
            textProg.setVisibility(View.GONE);
            if (textStatus != null) textStatus.setVisibility(View.GONE);
            textCenter.setTextColor(color(R.color.cf_gold));
            textCenter.setOnClickListener(null);
            textCenter.setClickable(false);
            rowView.setOnClickListener(null);
            rowView.setClickable(false);
            return rowView;
        }

        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        textLeft.setVisibility(View.VISIBLE);
        textClass.setVisibility(playerClass.isEmpty() ? View.GONE : View.VISIBLE);
        textRight.setVisibility(ratingText.isEmpty() ? View.GONE : View.VISIBLE);
        textProg.setVisibility(progression.isEmpty() ? View.GONE : View.VISIBLE);
        textLeft.setText(role);
        textClass.setText(playerClass);
        textRight.setText(ratingText);
        textProg.setText(progression);
        textLeft.setTextColor(color(R.color.cf_text_secondary));
        textClass.setTextColor(color(R.color.cf_text_secondary));
        textRight.setTextColor(color(R.color.cf_text_primary));
        textProg.setTextColor(color(R.color.cf_gold));

        int nameColor = color(R.color.cf_text_primary);
        if (status.contains("RS") || status.contains("[T]")) {
            nameColor = color(R.color.cf_text_muted);
        }
        if (status.contains("Hot Seat") || status.contains("Suspended")) {
            nameColor = color(R.color.cf_negative);
        }
        if (status.contains("INJ")) {
            nameColor = color(R.color.cf_warning);
        }
        textCenter.setTextColor(nameColor);

        if (!ratingText.isEmpty() && !ratingText.contains(" ")) {
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

        if (textStatus != null) {
            String caption = statusCaption(role, status, awardCode);
            textStatus.setText(caption);
            textStatus.setVisibility(caption.isEmpty() ? View.GONE : View.VISIBLE);
            textStatus.setTextColor(statusColor(status, awardCode));
        }

        if (week > 17 && week < 22 && playerClass.contains("Sr")) {
            textCenter.setTypeface(interFont, Typeface.ITALIC);
            textCenter.setTextColor(color(R.color.cf_text_muted));
            textCenter.setPaintFlags(textCenter.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textClass.setTypeface(interFont, Typeface.ITALIC);
            textClass.setTextColor(color(R.color.cf_text_muted));
            textClass.setPaintFlags(textClass.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
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
        rowView.setClickable(true);

        return rowView;
    }

    /** Starter, availability, and award sit under the name instead of being glued onto it. */
    private String statusCaption(String role, String status, String awardCode) {
        String availability = "";
        if ("*".equals(status)) {
            availability = context.getString(R.string.page_roster_starter);
        } else if (!status.isEmpty()) {
            availability = plainTag(status);
        }
        String award = awardLabel(role, awardCode);
        if (award.isEmpty()) return availability;
        if (availability.isEmpty()) return award;
        return availability + "  ·  " + award;
    }

    /** Engine tags arrive as {@code [INJ - 3 wks]} or {@code [T]}; the caption uses words. */
    private String plainTag(String status) {
        String trimmed = status.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() > 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if ("T".equals(trimmed)) return context.getString(R.string.page_roster_transfer);
        if ("RS".equals(trimmed)) return context.getString(R.string.page_roster_redshirt);
        if ("Med RS".equals(trimmed)) return context.getString(R.string.page_roster_med_rs);
        if ("INJ".equals(trimmed)) return context.getString(R.string.page_roster_injured);
        if ("Hot Seat".equals(trimmed)) return context.getString(R.string.page_roster_hot_seat);
        if (trimmed.startsWith("INJ - ")) {
            return context.getString(R.string.page_roster_injured_weeks, trimmed.substring(6));
        }
        return trimmed;
    }

    private String awardLabel(String role, String awardCode) {
        if ("1".equals(awardCode)) return context.getString(R.string.page_roster_all_fr);
        if ("2".equals(awardCode)) return context.getString(R.string.page_roster_all_conf);
        if ("3".equals(awardCode)) return context.getString(R.string.page_roster_all_am);
        if ("4".equals(awardCode)) {
            return context.getString(role.contains("HC") ? R.string.page_roster_coty : R.string.page_roster_poty);
        }
        return "";
    }

    private int statusColor(String status, String awardCode) {
        if (status.contains("Suspended") || status.contains("Hot Seat")) return color(R.color.cf_negative);
        if (status.contains("INJ")) return color(R.color.cf_warning);
        if (status.contains("RS") || status.contains("[T]")) return color(R.color.cf_text_muted);
        if (!awardLabel("", awardCode).isEmpty() && !"*".equals(status)) return color(R.color.cf_gold);
        if ("*".equals(status)) return color(R.color.cf_emerald);
        return color(R.color.cf_text_secondary);
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

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }

}

