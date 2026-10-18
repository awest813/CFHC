package ui;

/*
  Created by Achi Jones on 2/20/2016.
 */

        import android.content.Context;
        import android.graphics.Typeface;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.ArrayAdapter;
        import android.widget.TextView;

        import androidx.core.content.ContextCompat;
        import androidx.core.content.res.ResourcesCompat;

        import java.util.ArrayList;

        import antdroid.cfbcoach.R;

public class TeamRankingsList extends ArrayAdapter<String> {
    private final Context context;
    private final ArrayList<String> values;
    private String userTeamStrRep;
    private final Typeface interFont;
    private final Typeface monoFont;

    public TeamRankingsList(Context context, ArrayList<String> values, String userTeamStrRep) {
        super(context, R.layout.team_rankings_list_item, values);
        this.context = context;
        this.interFont = ResourcesCompat.getFont(context, R.font.inter);
        this.monoFont = ResourcesCompat.getFont(context, R.font.jetbrains_mono);
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.team_rankings_list_item, parent, false);
        }
        TextView textLeft = rowView.findViewById(R.id.textTeamRankingsLeft);
        TextView textCenter = rowView.findViewById(R.id.textTeamRankingsCenter);
        TextView textDetail = rowView.findViewById(R.id.textTeamRankingsDetail);
        TextView textRight = rowView.findViewById(R.id.textTeamRankingsRight);


        String[] teamStat = values.get(position).split(",", -1);
        String rank = valueAt(teamStat, 0);
        String team = valueAt(teamStat, 1);
        String detail = valueAt(teamStat, 2);
        String score = detail;
        String headline = "";
        int dot = detail.indexOf(" · ");
        if (dot >= 0) {
            score = detail.substring(0, dot).trim();
            headline = detail.substring(dot + 3).trim();
        }
        textLeft.setText(rank);
        textCenter.setText(team);
        textRight.setText(score);
        if (textDetail != null) {
            textDetail.setText(headline);
            textDetail.setVisibility(headline.isEmpty() ? View.GONE : View.VISIBLE);
        }
        // Reset recycled-row styling to the HUD row defaults (gold rank pill, Inter name, mono stat).
        rowView.setBackgroundResource(R.drawable.bg_cf_card);
        textLeft.setTextColor(color(R.color.cf_gold));
        textCenter.setTextColor(color(R.color.cf_text_primary));
        textRight.setTextColor(color(R.color.cf_text_primary));
        textLeft.setTypeface(monoFont, Typeface.BOLD);
        textCenter.setTypeface(interFont, Typeface.BOLD);
        textRight.setTypeface(monoFont, Typeface.BOLD);

        boolean userRow = team.equals(userTeamStrRep);
        if (userRow) {
            // User team: emerald leading bar + emerald text
            rowView.setBackgroundResource(R.drawable.bg_cf_card_accent);
            textCenter.setTextColor(color(R.color.cf_emerald));
            textRight.setTextColor(color(R.color.cf_emerald));
        }
        if (textDetail != null && !headline.isEmpty()) {
            textDetail.setTextColor(color(userRow ? R.color.cf_emerald : R.color.cf_text_secondary));
        }
        String[] detailParts = detail.split(" ");
        if (detailParts.length > 2 && detailParts[2].contains("+")) {
            // Highlight Prestige Changes in off-season
            textRight.setTextColor(color(R.color.cf_positive));
        } else if (detailParts.length > 2 && detailParts[2].contains("-")) {
            textRight.setTextColor(color(R.color.cf_negative));
        }

        return rowView;
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
