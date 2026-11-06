package ui;

/*
  Created by Achi Jones on 2/20/2016.
 */

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;
import simulation.MockDraftLine;

public class MockDraft extends RowAdapter<String> {
    private final String[] values;
    private final String userTeamStrRep;

    public MockDraft(Context context, String[] values, String userTeamStrRep) {
        super(context, R.layout.mock_draft_row, values);
        this.values = values;
        this.userTeamStrRep = userTeamStrRep;
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;

        TextView roundView = holder.view(R.id.mockDraftRound);
        View card = holder.view(R.id.mockDraftCard);
        TextView pickView = holder.view(R.id.mockDraftPick);
        TextView playerView = holder.view(R.id.mockDraftPlayer);
        TextView metaView = holder.view(R.id.mockDraftMeta);
        TextView overallView = holder.view(R.id.mockDraftOverall);

        MockDraftLine line = MockDraftLine.parse(values[position]);
        boolean userRow = line.parsed && line.school.equals(userTeamStrRep);
        card.setBackgroundResource(userRow ? R.drawable.bg_cf_card_accent : R.drawable.bg_cf_card);

        if (!line.parsed) {
            roundView.setVisibility(View.GONE);
            pickView.setVisibility(View.GONE);
            metaView.setVisibility(View.GONE);
            overallView.setVisibility(View.GONE);
            playerView.setText(line.raw);
            playerView.setTextColor(color(R.color.cf_text_secondary));
            playerView.setMaxLines(4); // notes such as "No prospects have declared..." wrap
            playerView.setEllipsize(null);
            return;
        }
        playerView.setMaxLines(1);
        playerView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        boolean roundStart = "1".equals(line.pick);
        roundView.setVisibility(roundStart ? View.VISIBLE : View.GONE);
        if (roundStart) {
            roundView.setText(context.getString(R.string.dlg_draft_round, line.round));
        }
        pickView.setVisibility(View.VISIBLE);
        pickView.setText(line.pickLabel);
        pickView.setContentDescription(context.getString(R.string.dlg_draft_pick_cd, line.round, line.pick));

        playerView.setText(line.player);
        playerView.setTextColor(color(userRow ? R.color.cf_emerald : R.color.cf_text_primary));

        String meta = line.metaLine();
        metaView.setText(meta);
        metaView.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
        metaView.setTextColor(color(userRow ? R.color.cf_emerald : R.color.cf_text_secondary));

        overallView.setVisibility(line.overall.isEmpty() ? View.GONE : View.VISIBLE);
        overallView.setText(line.overall);
        overallView.setContentDescription(
                line.overall.isEmpty() ? null : context.getString(R.string.dlg_draft_overall_cd, line.overall));
        overallView.setTextColor(color(overallColor(line.overall, userRow)));

        return;
    }

    private int overallColor(String overall, boolean userRow) {
        try {
            int rating = Integer.parseInt(overall);
            if (rating > 90) return R.color.cf_emerald;
            if (rating > 80) return R.color.cf_positive;
        } catch (NumberFormatException ignored) {
        }
        return userRow ? R.color.cf_emerald : R.color.cf_text_primary;
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }
}
