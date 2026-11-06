package ui.components;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * An ImageView for the 16-bit sprites: scaling uses nearest-neighbour, so the
 * pixel art stays crisp instead of blurring (the style guide's "rendered
 * pixelated"). Base for {@link CrestView} and {@link TrophyIconView}.
 */
public class PixelSpriteView extends AppCompatImageView {

    public PixelSpriteView(Context context) {
        this(context, null);
    }

    public PixelSpriteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.FIT_CENTER);
        crisp(getDrawable());
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        crisp(drawable);
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        crisp(getDrawable());
    }

    private static void crisp(Drawable d) {
        if (d instanceof BitmapDrawable) {
            BitmapDrawable b = (BitmapDrawable) d;
            b.setFilterBitmap(false);
            b.setAntiAlias(false);
        }
    }
}
