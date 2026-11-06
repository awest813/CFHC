package ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import antdroid.cfbcoach.PlayerSprites;
import antdroid.cfbcoach.R;

/**
 * Player sprite card (redesign component 4): the position's pixel avatar,
 * the player's name, a detail line and an emerald overall tile. Used for the
 * Team Home "Roster Spotlight".
 */
public class PlayerSpriteCardView extends LinearLayout {

    private final ImageView avatar;
    private final TextView name;
    private final TextView detail;
    private final TextView overall;

    public PlayerSpriteCardView(Context context) {
        this(context, null);
    }

    public PlayerSpriteCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.drawable.bg_cf_card_canvas);
        LayoutInflater.from(context).inflate(R.layout.component_player_sprite_card, this, true);
        avatar = findViewById(R.id.cmpSpriteAvatar);
        name = findViewById(R.id.cmpSpriteName);
        detail = findViewById(R.id.cmpSpriteDetail);
        overall = findViewById(R.id.cmpSpriteOverall);
    }

    public void bind(String position, String playerName, String detailLine, int rating) {
        avatar.setImageResource(PlayerSprites.avatarFor(position));
        name.setText(playerName);
        detail.setText(detailLine);
        overall.setText(String.valueOf(rating));
        setContentDescription(position + " " + playerName + ", " + detailLine + ", "
                + getContext().getString(R.string.cmp_overall_a11y, rating));
    }
}
