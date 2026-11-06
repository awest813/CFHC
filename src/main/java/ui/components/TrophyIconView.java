package ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import antdroid.cfbcoach.R;

/** Trophy badge (redesign component 6): crystal, Heisman or ring sprite, pixel-crisp. */
public class TrophyIconView extends PixelSpriteView {

    public enum Trophy {
        CRYSTAL(R.drawable.sprite_trophy_crystal),
        HEISMAN(R.drawable.sprite_trophy_heisman),
        RING(R.drawable.sprite_trophy_ring);

        final int drawable;

        Trophy(int drawable) {
            this.drawable = drawable;
        }
    }

    public TrophyIconView(Context context) {
        this(context, null);
    }

    public TrophyIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int index = 0;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TrophyIconView);
            index = a.getInt(R.styleable.TrophyIconView_trophy, 0);
            a.recycle();
        }
        setTrophy(Trophy.values()[Math.max(0, Math.min(Trophy.values().length - 1, index))]);
    }

    public void setTrophy(Trophy trophy) {
        setImageResource(trophy.drawable);
    }
}
