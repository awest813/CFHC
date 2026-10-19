package ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.MainActivity;
import antdroid.cfbcoach.R;
import simulation.CoachDirectoryRow;

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
        View rowView = convertView;
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (rowView == null) {
            rowView = inflater.inflate(R.layout.team_rankings_list_item, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.textTeamRankingsLeft);
        TextView textCenter = rowView.findViewById(R.id.textTeamRankingsCenter);
        TextView textDetail = rowView.findViewById(R.id.textTeamRankingsDetail);
        TextView textRight = rowView.findViewById(R.id.textTeamRankingsRight);

        final String[] teamStat = values.get(position).split(",", -1);
        final String left = valueAt(teamStat, 0);
        final String center = valueAt(teamStat, 1);
        final String right = valueAt(teamStat, 2);
        final String coachName = CoachDirectoryRow.bareName(center);
        final String status = statusWords(CoachDirectoryRow.statusLabel(center));
        textLeft.setText(left);
        textCenter.setText(coachName);
        textRight.setText(right);
        if (textDetail != null) {
            textDetail.setText(status);
            textDetail.setVisibility(status.isEmpty() ? View.GONE : View.VISIBLE);
        }

        // Reset recycled-row styling to the HUD row defaults.
        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        textLeft.setTypeface(textLeft.getTypeface(), Typeface.NORMAL);
        textCenter.setTypeface(textCenter.getTypeface(), Typeface.NORMAL);
        textRight.setTypeface(textRight.getTypeface(), Typeface.NORMAL);
        textLeft.setTextColor(color(R.color.cf_gold));
        textCenter.setTextColor(color(R.color.cf_text_primary));
        textRight.setTextColor(color(R.color.cf_text_primary));

        if (!center.contains("[U]") && !center.contains("[R]")) {
            // Active (employed) coach
            textCenter.setTextColor(color(R.color.cf_emerald));
            textRight.setTextColor(color(R.color.cf_emerald));
        }
        if (center.contains("[R]")) {
            // Retired coach
            textCenter.setTextColor(color(R.color.cf_text_muted));
            textRight.setTextColor(color(R.color.cf_text_muted));
        }

        if (isPastUser(center, coachName)) {
            // Coaches the user has controlled in this save
            textLeft.setTextColor(color(R.color.cf_amber));
            textCenter.setTextColor(color(R.color.cf_amber));
            textRight.setTextColor(color(R.color.cf_amber));
        }


        if (center.equals(userHC) || coachName.equals(userHC)) {
            // Current user head coach: emerald leading bar + gold text
            rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            textLeft.setTypeface(textLeft.getTypeface(), Typeface.BOLD);
            textLeft.setTextColor(color(R.color.cf_gold));
            textCenter.setTypeface(textCenter.getTypeface(), Typeface.BOLD);
            textCenter.setTextColor(color(R.color.cf_gold));
            textRight.setTypeface(textRight.getTypeface(), Typeface.BOLD);
            textRight.setTextColor(color(R.color.cf_gold));
        }
        if (textDetail != null && !status.isEmpty()) {
            textDetail.setTextColor(textCenter.getCurrentTextColor());
        }

        String[] rightParts = right.split(" ");
        if (rightParts.length > 2 && rightParts[2].contains("+")) {
            // Highlight Prestige Changes in off-season
            textRight.setTextColor(color(R.color.cf_positive));
        } else if (rightParts.length > 2 && rightParts[2].contains("-")) {
            textRight.setTextColor(color(R.color.cf_negative));
        }


        textCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainAct.examineCoachDB(coachName);
            }
        });


        return rowView;
    }



    public void setupUserHC(String userHC) {
        this.userHC = userHC;
    }

    /** Engine tags stay on the lookup string; the caption uses words. */
    private String statusWords(String label) {
        if ("Retired".equals(label)) return context.getString(R.string.dlg_coach_retired);
        if ("Unemployed".equals(label)) return context.getString(R.string.dlg_coach_unemployed);
        return label;
    }

    private boolean isPastUser(String decorated, String bare) {
        if (userNames == null) return false;
        for (String saved : userNames) {
            if (saved == null) continue;
            if (saved.equals(decorated) || CoachDirectoryRow.bareName(saved).equals(bare)) {
                return true;
            }
        }
        return false;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    private static String valueAt(String[] values, int index) {
        return index < values.length ? values[index] : "";
    }
}
