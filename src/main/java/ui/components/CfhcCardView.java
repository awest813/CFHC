package ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import antdroid.cfbcoach.R;

/**
 * Slate card (redesign component 1): bordered card background, the card
 * padding tokens and an optional {@code app:cardTitle} header in the
 * CardTitle style. Children stack vertically under the title.
 */
public class CfhcCardView extends LinearLayout {

    private final TextView title;

    public CfhcCardView(Context context) {
        this(context, null);
    }

    public CfhcCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.bg_cf_card);
        int h = getResources().getDimensionPixelSize(R.dimen.cf_card_padding_h);
        int v = getResources().getDimensionPixelSize(R.dimen.cf_card_padding_v);
        setPaddingRelative(h, v, h, v);
        title = new TextView(context, null, 0, R.style.Widget_Cfhc_CardTitle);
        CharSequence text = null;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CfhcCardView);
            text = a.getText(R.styleable.CfhcCardView_cardTitle);
            a.recycle();
        }
        addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setTitle(text);
    }

    public void setTitle(CharSequence text) {
        title.setText(text);
        title.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
    }
}
