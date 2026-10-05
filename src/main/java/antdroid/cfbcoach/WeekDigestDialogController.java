package antdroid.cfbcoach;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import simulation.WeekDigestView;

/**
 * Renders the engine's week-in-review digest as HUD cards: a result callout
 * with a W/L/T pill, one card per section (Top 10 in mono, headlines,
 * injuries) and a crimson "Next up" opponent card.
 */
public final class WeekDigestDialogController {
    private WeekDigestDialogController() {
    }

    public static void show(MainActivity activity, String digest) {
        WeekDigestView view = WeekDigestView.parse(digest);
        View content = LayoutInflater.from(activity).inflate(R.layout.week_digest_dialog, null, false);

        TextView title = content.findViewById(R.id.digestTitle);
        title.setText(view.title.isEmpty() ? activity.getString(R.string.digest_pill) : view.title);

        View resultCard = content.findViewById(R.id.digestResultCard);
        TextView outcome = content.findViewById(R.id.digestOutcome);
        TextView result = content.findViewById(R.id.digestResult);
        if (view.result.isEmpty()) {
            resultCard.setVisibility(View.GONE);
        } else {
            String detail = view.result;
            if ("BYE".equals(view.outcome)) {
                outcome.setText("—");
                detail = activity.getString(R.string.digest_bye);
            } else if (!view.outcome.isEmpty()) {
                outcome.setText(view.outcome);
                detail = view.result.substring(1).trim();
            } else {
                outcome.setVisibility(View.GONE);
            }
            styleOutcome(activity, outcome, resultCard, view.outcome);
            result.setText(detail);
        }

        ViewGroup sections = content.findViewById(R.id.digestSections);
        LayoutInflater inflater = LayoutInflater.from(activity);
        for (WeekDigestView.Section section : view.sections) {
            View card = inflater.inflate(R.layout.week_digest_section, sections, false);
            ((TextView) card.findViewById(R.id.digestSectionHeading)).setText(section.heading);
            TextView body = card.findViewById(R.id.digestSectionBody);
            boolean ranking = section.heading.toLowerCase().startsWith("top ");
            if (ranking) {
                body.setTypeface(ResourcesCompat.getFont(activity, R.font.jetbrains_mono));
                body.setText(TextUtils.join("\n", section.lines));
            } else {
                StringBuilder sb = new StringBuilder();
                for (String line : section.lines) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append("•  ").append(line);
                }
                body.setText(sb.toString());
            }
            sections.addView(card);
        }

        View nextCard = content.findViewById(R.id.digestNextCard);
        if (view.nextUp.isEmpty()) {
            nextCard.setVisibility(View.GONE);
        } else {
            ((TextView) content.findViewById(R.id.digestNext)).setText(view.nextUp);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .setPositiveButton(R.string.digest_continue, null)
                .create();
        activity.showImmersive(dialog);
    }

    private static void styleOutcome(MainActivity activity, TextView pill, View card, String outcome) {
        if ("L".equals(outcome)) {
            pill.setBackgroundResource(R.drawable.bg_cf_pill_crimson);
            pill.setTextColor(ContextCompat.getColor(activity, R.color.cf_on_crimson));
            card.setBackgroundResource(R.drawable.bg_cf_card_crimson);
        } else if ("W".equals(outcome)) {
            pill.setBackgroundResource(R.drawable.bg_cf_pill_emerald);
            pill.setTextColor(ContextCompat.getColor(activity, R.color.cf_emerald));
        } else {
            pill.setBackgroundResource(R.drawable.bg_cf_pill_neutral);
            pill.setTextColor(ContextCompat.getColor(activity, R.color.cf_text_secondary));
            card.setBackgroundResource(R.drawable.bg_cf_card);
        }
    }
}
