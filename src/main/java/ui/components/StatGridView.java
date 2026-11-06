package ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import antdroid.cfbcoach.R;

/**
 * Stat grid (redesign component 2): label-over-value cells, {@code app:columns}
 * per row (default 2). Labels use the micro caption style, values the mono
 * stat style, matching the dashboard tiles.
 */
public class StatGridView extends LinearLayout {

    private final int columns;
    private LinearLayout row;

    public StatGridView(Context context) {
        this(context, null);
    }

    public StatGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        int cols = 2;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StatGridView);
            cols = a.getInt(R.styleable.StatGridView_columns, 2);
            a.recycle();
        }
        columns = Math.max(1, cols);
    }

    public void clear() {
        removeAllViews();
        row = null;
    }

    /** Adds one cell; returns the value view so callers can colour it. */
    public TextView addCell(CharSequence label, CharSequence value) {
        if (row == null || row.getChildCount() >= columns) {
            row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            row.setBaselineAligned(false);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (getChildCount() > 0) lp.topMargin = getResources().getDimensionPixelSize(R.dimen.cf_space_sm);
            addView(row, lp);
        }
        LinearLayout cell = new LinearLayout(getContext());
        cell.setOrientation(VERTICAL);
        cell.setGravity(Gravity.START);
        TextView l = new TextView(getContext());
        l.setTextAppearance(R.style.TextAppearance_Cfhc_Cf_Micro);
        l.setAllCaps(true);
        l.setText(label);
        TextView v = new TextView(getContext());
        v.setTextAppearance(R.style.TextAppearance_Cfhc_Cf_Stat);
        v.setText(value);
        cell.addView(l);
        cell.addView(v);
        cell.setContentDescription(label + " " + value);
        row.addView(cell, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        return v;
    }
}
