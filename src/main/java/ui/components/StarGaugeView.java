package ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;

/**
 * Drawn star gauge (redesign component 3): five stars, gold to the value,
 * slate after it; half values fill half a star. Replaces "4 Stars" text.
 */
public class StarGaugeView extends View {

    private static final int MAX = 5;
    private final Paint filled = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint empty = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path star = new Path();
    private float stars;

    public StarGaugeView(Context context) {
        this(context, null);
    }

    public StarGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        filled.setColor(ContextCompat.getColor(context, R.color.cf_gold));
        empty.setColor(ContextCompat.getColor(context, R.color.cf_star_empty));
        setStars(isInEditMode() ? 3.5f : 0f);
    }

    /** 0-5; halves are drawn as half stars. */
    public void setStars(float value) {
        stars = Math.max(0f, Math.min(MAX, value));
        String shown = stars == Math.floor(stars) ? String.valueOf((int) stars) : String.valueOf(stars);
        setContentDescription(getContext().getString(R.string.cmp_stars_a11y, shown, MAX));
        invalidate();
    }

    public float getStars() {
        return stars;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = getResources().getDimensionPixelSize(R.dimen.cf_star_size);
        int gap = getResources().getDimensionPixelSize(R.dimen.cf_space_xxs);
        int w = getPaddingLeft() + getPaddingRight() + MAX * size + (MAX - 1) * gap;
        int h = getPaddingTop() + getPaddingBottom() + size;
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int gap = getResources().getDimensionPixelSize(R.dimen.cf_space_xxs);
        float size = Math.min(getHeight() - getPaddingTop() - getPaddingBottom(),
                (getWidth() - getPaddingLeft() - getPaddingRight() - (MAX - 1) * gap) / (float) MAX);
        if (size <= 0) return;
        float top = getPaddingTop() + (getHeight() - getPaddingTop() - getPaddingBottom() - size) / 2f;
        for (int i = 0; i < MAX; i++) {
            float left = getPaddingLeft() + i * (size + gap);
            buildStar(left, top, size);
            canvas.drawPath(star, empty);
            float fill = Math.max(0f, Math.min(1f, stars - i));
            if (fill > 0f) {
                int save = canvas.save();
                canvas.clipRect(left, top, left + size * fill, top + size);
                canvas.drawPath(star, filled);
                canvas.restoreToCount(save);
            }
        }
    }

    private void buildStar(float left, float top, float size) {
        star.reset();
        float cx = left + size / 2f;
        float cy = top + size / 2f;
        float outer = size / 2f;
        float inner = outer * 0.45f;
        for (int k = 0; k < 10; k++) {
            double angle = Math.toRadians(-90 + k * 36);
            float r = (k % 2 == 0) ? outer : inner;
            float x = cx + (float) (r * Math.cos(angle));
            float y = cy + (float) (r * Math.sin(angle));
            if (k == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.close();
    }
}
