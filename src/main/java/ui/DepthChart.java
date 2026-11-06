package ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import antdroid.cfbcoach.R;
import positions.Player;

/**
 * Created by Achi Jones on 3/18/2016.
 */
public class DepthChart extends RowAdapter<Player> {
    public final ArrayList<Player> players;
    public final ArrayList<Player> playersSelected;
    public int playersRequired;

    public DepthChart(Context context, ArrayList<Player> values, int playersRequired) {
        super(context, R.layout.team_lineup_list_item, values);
        this.players = values;
        this.playersRequired = playersRequired;
        playersSelected = new ArrayList<>();
        if (players.size() >= playersRequired) {
            for (int i = 0; i < playersRequired; ++i) {
                playersSelected.add(players.get(i));
            }
        } else {
            for (int i = 0; i < players.size(); ++i) {
                playersSelected.add(players.get(i));
            }
        }
    }

    @Override
    protected void bind(RowHolder holder, final int position) {
        View rowView = holder.itemView;

        TextView playerInfo = holder.view(R.id.textViewLineupPlayerInfo);

        if (players.get(position).injury == null && !players.get(position).isTransfer && !players.get(position).isSuspended) {
            playerInfo.setText(players.get(position).getInfoForLineup());
        } else if (players.get(position).isInjured) {
            playerInfo.setText(players.get(position).getInfoLineupInjury());
        } else if (players.get(position).isTransfer) {
            playerInfo.setText(players.get(position).getInfoLineupTransfer());
        } else if (players.get(position).isSuspended) {
            playerInfo.setText(players.get(position).getInfoLineupSuspended());

        }


        CheckBox isPlayerStarting = holder.view(R.id.checkboxPlayerStartingLineup);
        // Reset recycled-row text colour to the HUD default before flagging availability.
        playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
        // Reset the recycled checkbox before binding this player. Detach the old
        // listener first, or setChecked would toggle the row's previous player.
        isPlayerStarting.setOnCheckedChangeListener(null);
        isPlayerStarting.setEnabled(true);
        isPlayerStarting.setText(null);
        isPlayerStarting.setChecked(playersSelected.contains(players.get(position)));
        if (playersSelected.contains(players.get(position))) {
            // Selected starter: checked above.
        } else if (players.get(position).isInjured) {
            // Is injured
            isPlayerStarting.setEnabled(false);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_warning));
        } else if (players.get(position).isTransfer) {
            // Is Transfer
            isPlayerStarting.setEnabled(false);
            isPlayerStarting.setText("T");
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_negative));
        } else if (players.get(position).isSuspended) {
            // Is suspended
            isPlayerStarting.setEnabled(false);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_warning));
        } else if (players.get(position).isRedshirt) {
            isPlayerStarting.setEnabled(false);
            isPlayerStarting.setText("RS");
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_negative));
        } else {
            isPlayerStarting.setChecked(false);
        }

        isPlayerStarting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    playersSelected.add(players.get(position));
                } else {
                    playersSelected.remove(players.get(position));
                }
            }
        });


        return;


    }
}
