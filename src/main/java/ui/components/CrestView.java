package ui.components;

import android.content.Context;
import android.util.AttributeSet;

import antdroid.cfbcoach.R;

/**
 * Team crest (redesign component 5): one of the crest sprites, chosen from the
 * team name so a team always gets the same crest, drawn pixel-crisp.
 */
public class CrestView extends PixelSpriteView {

    private static final int[] CRESTS = {
            R.drawable.sprite_crest_eagles, R.drawable.sprite_crest_bears,
            R.drawable.sprite_crest_bison, R.drawable.sprite_crest_wildcats
    };

    public CrestView(Context context) {
        this(context, null);
    }

    public CrestView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (getDrawable() == null) {
            setImageResource(CRESTS[0]);
        }
    }

    /** Stable for a team name across launches (String.hashCode is specified). */
    public static int crestFor(String teamName) {
        int h = teamName == null ? 0 : teamName.hashCode();
        return CRESTS[Math.floorMod(h, CRESTS.length)];
    }

    public void setTeam(String teamName) {
        setImageResource(crestFor(teamName));
        setContentDescription(teamName == null ? null
                : getContext().getString(R.string.cmp_crest_a11y, teamName));
    }
}
