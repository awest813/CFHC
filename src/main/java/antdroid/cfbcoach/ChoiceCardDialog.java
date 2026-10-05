package antdroid.cfbcoach;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

/**
 * Non-cancellable "pick one" wizard step rendered as HUD cards: a label pill,
 * title, subtitle and one card per choice (title, optional badge, optional
 * description). Used by the new-dynasty chain in place of bare radio lists.
 */
public final class ChoiceCardDialog {

    /** One selectable card. {@code badge} and {@code body} may be null. */
    public static final class Choice {
        final String title;
        final String badge;
        final String body;

        public Choice(String title, String badge, String body) {
            this.title = title;
            this.badge = badge;
            this.body = body;
        }
    }

    public interface OnChosen {
        void onChosen(int index);
    }

    private ChoiceCardDialog() {
    }

    public static void show(final MainActivity activity, String pill,
                            String title, String subtitle, final List<Choice> choices,
                            final OnChosen callback) {
        View content = LayoutInflater.from(activity).inflate(R.layout.choice_card_dialog, null, false);
        TextView stepView = content.findViewById(R.id.choiceStep);
        if (pill != null && !pill.isEmpty()) {
            stepView.setText(pill);
        } else {
            stepView.setVisibility(View.GONE);
        }
        ((TextView) content.findViewById(R.id.choiceTitle)).setText(title);
        TextView subtitleView = content.findViewById(R.id.choiceSubtitle);
        if (subtitle == null || subtitle.isEmpty()) {
            subtitleView.setVisibility(View.GONE);
        } else {
            subtitleView.setText(subtitle);
        }

        ListView list = content.findViewById(R.id.choiceList);
        list.setAdapter(new ArrayAdapter<Choice>(activity, R.layout.choice_card_row, choices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView != null ? convertView
                        : LayoutInflater.from(getContext()).inflate(R.layout.choice_card_row, parent, false);
                Choice c = getItem(position);
                TextView rowTitle = row.findViewById(R.id.choiceRowTitle);
                TextView badge = row.findViewById(R.id.choiceRowBadge);
                TextView body = row.findViewById(R.id.choiceRowBody);
                rowTitle.setText(c != null ? c.title : "");
                bindOptional(badge, c != null ? c.badge : null);
                bindOptional(body, c != null ? c.body : null);
                return row;
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .setCancelable(false)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        list.setOnItemClickListener((parent, view, position, id) -> {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            dialog.dismiss();
            callback.onChosen(position);
        });
        activity.showImmersive(dialog);
    }

    private static void bindOptional(TextView view, String text) {
        if (text == null || text.trim().isEmpty()) {
            view.setVisibility(View.GONE);
            view.setText("");
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(text.trim());
        }
    }
}
