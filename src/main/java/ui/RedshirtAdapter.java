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

import static android.view.View.INVISIBLE;

/**
 * Created by Achi Jones on 3/18/2016.
 */
public class RedshirtAdapter extends RowAdapter<Player> {
    public final ArrayList<Player> players;
    public final ArrayList<Player> playersSelected;
    public final ArrayList<Player> playersRemoved;
    public int playersRequired;

    public RedshirtAdapter(Context context, ArrayList<Player> values, int playersRequired) {
        super(context, R.layout.team_lineup_list_item, values);
        this.players = values;
        this.playersRequired = playersRequired;
        playersSelected = new ArrayList<>();
        playersRemoved = new ArrayList<>();
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
        // Reset recycled-row text colour to the HUD default before flagging eligibility.
        playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_text_primary));
        // Reset the recycled checkbox before binding this player. Detach the old
        // listener first, or setChecked would toggle the row's previous player.
        isPlayerStarting.setOnCheckedChangeListener(null);
        isPlayerStarting.setEnabled(true);
        // Checked = already redshirted and not being removed, or newly picked this session,
        // so a row scrolled off and back keeps the user's pending choice.
        Player p = players.get(position);
        isPlayerStarting.setChecked((p.isRedshirt && !playersRemoved.contains(p)) || playersSelected.contains(p));
        if (players.get(position).isInjured) {
            // Is injured
            isPlayerStarting.setEnabled(false);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_info));
        } else if (players.get(position).isTransfer) {
            // Is Transfer
            isPlayerStarting.setEnabled(false);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_text_muted));
        } else if (players.get(position).isSuspended) {
            // Is suspended
            isPlayerStarting.setEnabled(false);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_positive));
        } else if (players.get(position).wasRedshirt) {
            isPlayerStarting.setEnabled(false);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_text_muted));
        } else if (players.get(position).isRedshirt) {
            isPlayerStarting.setEnabled(true);
            playerInfo.setTextColor(ContextCompat.getColor(context, R.color.cf_negative));
        }

        isPlayerStarting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    playersSelected.add(players.get(position));
                } else {
                    playersSelected.remove(players.get(position));
                }
                if (!isChecked && players.get(position).isRedshirt) {
                    playersRemoved.add(players.get(position));
                }
            }
        });


        return;


    }
}
