package antdroid.cfbcoach;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import positions.Archetypes;
import positions.Player;
import simulation.Team;

public final class PlayerDevelopmentDialogController {
    private PlayerDevelopmentDialogController() {}

    public static void show(MainActivity activity, Player p, Team userTeam) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Development Plan")
                .setView(activity.getLayoutInflater().inflate(R.layout.player_dev_plan, null, false))
                .setPositiveButton("Close", (dialog, which) -> {})
                .setNegativeButton("Reset to Default", (dialog, which) -> {
                    p.archetypeTag = "";
                    p.assignArchetype();
                });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        activity.showImmersive(dialog);
        bindDevPlan(dialog, activity, p);
    }

    static void bindDevPlan(AlertDialog dialog, MainActivity activity, Player p) {
        TextView dpPlayerName = dialog.findViewById(R.id.dpPlayerName);
        TextView dpPlayerInfo = dialog.findViewById(R.id.dpPlayerInfo);
        TextView dpCurrentPlan = dialog.findViewById(R.id.dpCurrentPlan);
        LinearLayout dpArchetypeList = dialog.findViewById(R.id.dpArchetypeList);

        String rs = p.wasRedshirt ? "RS " : "";
        dpPlayerName.setText(p.getName());
        dpPlayerInfo.setText(p.position + " | " + rs + p.getYrStr() + " | " + p.ratOvr + " OVR");
        dpCurrentPlan.setText("Current: " + p.getArchetypeDisplayName());

        String[] attrNames = getAttrNames(p.position);
        String[] tags = Archetypes.getArchetypesForPosition(p.position);

        // Token palette (style_guide: gold = active/selected, mono for every number).
        final int colorActive = ContextCompat.getColor(activity, R.color.cf_gold);
        final int colorPrimary = ContextCompat.getColor(activity, R.color.cf_text_primary);
        final int colorSecondary = ContextCompat.getColor(activity, R.color.cf_text_secondary);
        final int colorMuted = ContextCompat.getColor(activity, R.color.cf_text_muted);
        final Typeface mono = ResourcesCompat.getFont(activity, R.font.jetbrains_mono);
        final Resources res = activity.getResources();
        final int padH = res.getDimensionPixelSize(R.dimen.cf_card_padding_h);
        final int padV = res.getDimensionPixelSize(R.dimen.cf_card_padding_v);
        final int gap = res.getDimensionPixelSize(R.dimen.cf_space_sm);
        final int gapXs = res.getDimensionPixelSize(R.dimen.cf_space_xs);

        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            double[] mults = Archetypes.getMultipliers(p.position, tag);
            int[] caps = Archetypes.getCaps(p.position, tag);
            boolean isActive = tag.equals(p.archetypeTag);

            LinearLayout rowLayout = new LinearLayout(activity);
            rowLayout.setOrientation(LinearLayout.VERTICAL);
            rowLayout.setPadding(padH, padV, padH, padV);
            rowLayout.setBackgroundResource(isActive ? R.drawable.bg_cf_card_accent : R.drawable.bg_cf_card_canvas);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, gap);
            rowLayout.setLayoutParams(lp);

            String displayName = Archetypes.displayName(tag);
            TextView nameView = new TextView(activity);
            nameView.setText(displayName + (isActive ? "  \u2713 Active" : ""));
            nameView.setTextColor(isActive ? colorActive : colorPrimary);
            nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            nameView.setTypeface(nameView.getTypeface(), Typeface.BOLD);
            rowLayout.addView(nameView);

            StringBuilder multLine = new StringBuilder();
            for (int a = 0; a < 4; a++) {
                if (a > 0) multLine.append("  |  ");
                multLine.append(attrNames[a]).append(" ").append(String.format("%.1f", mults[a])).append("x");
            }
            TextView multView = new TextView(activity);
            multView.setText(multLine.toString());
            multView.setTextColor(colorSecondary);
            multView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
            if (mono != null) multView.setTypeface(mono);
            multView.setPadding(0, gap, 0, 0);
            rowLayout.addView(multView);

            StringBuilder capLine = new StringBuilder("Caps: ");
            for (int a = 0; a < 4; a++) {
                if (a > 0) capLine.append(", ");
                capLine.append(attrNames[a]).append(" ").append(caps[a]);
            }
            TextView capView = new TextView(activity);
            capView.setText(capLine.toString());
            capView.setTextColor(colorMuted);
            capView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
            if (mono != null) capView.setTypeface(mono);
            capView.setPadding(0, gapXs, 0, 0);
            rowLayout.addView(capView);

            if (!isActive) {
                final String selectedTag = tag;
                rowLayout.setOnClickListener(v -> confirmChange(activity, dialog, p, selectedTag));
            }

            dpArchetypeList.addView(rowLayout);
        }
    }

    private static void confirmChange(MainActivity activity, AlertDialog parentDialog, Player p, String newTag) {
        String displayName = Archetypes.displayName(newTag);
        AlertDialog.Builder confirm = new AlertDialog.Builder(activity);
        confirm.setTitle("Change Development Plan")
                .setMessage("Set " + p.getName() + "'s development plan to " + displayName
                        + "?\n\nThis changes how their attributes grow each season and which stats get capped.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    p.archetypeTag = newTag;
                    p.assignArchetype();
                    parentDialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {});
        AlertDialog d = confirm.create();
        d.setCancelable(false);
        activity.showImmersive(d);
    }

    static String[] getAttrNames(String position) {
        switch (position) {
            case "QB": return new String[]{"Pass Pow", "Pass Acc", "Evasion", "Speed"};
            case "RB": return new String[]{"Speed", "Evasion", "Power", "Catch"};
            case "WR": return new String[]{"Speed", "Catch", "Evasion", "Jump"};
            case "TE": return new String[]{"Run Blk", "Catch", "Evasion", "Speed"};
            case "OL": return new String[]{"Run Blk", "Pass Blk", "Strength", "Vision"};
            case "DL": return new String[]{"Run Stop", "Tackle", "Pass Rush", "Strength"};
            case "LB": return new String[]{"Tackle", "Run Stop", "Coverage", "Speed"};
            case "CB": return new String[]{"Coverage", "Speed", "Tackle", "Jump"};
            case "S":  return new String[]{"Tackle", "Coverage", "Speed", "Run Stop"};
            case "K":  return new String[]{"Kick Pow", "Kick Acc", "Pressure", "Form"};
            default:   return new String[]{"Attr 1", "Attr 2", "Attr 3", "Attr 4"};
        }
    }
}
