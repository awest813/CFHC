package ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import antdroid.cfbcoach.R;
import simulation.RatingScale;

/**
 * Rating badge (redesign component 3): the giant emerald rating digit with a
 * grade pill and a drawn star gauge beside it. Grade and stars come from the
 * engine's {@link RatingScale}, the same scale desktop's Team Overall card uses.
 */
public class RatingBadgeView extends LinearLayout {

    private final TextView value;
    private final TextView grade;
    private final StarGaugeView stars;

    public RatingBadgeView(Context context) {
        this(context, null);
    }

    public RatingBadgeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.component_rating_badge, this, true);
        value = findViewById(R.id.cmpRatingValue);
        grade = findViewById(R.id.cmpRatingGrade);
        stars = findViewById(R.id.cmpRatingStars);
        setRating(isInEditMode() ? 82 : 0);
    }

    public void setRating(int rating) {
        value.setText(rating > 0 ? String.valueOf(rating) : "—");
        grade.setText(RatingScale.grade(rating));
        stars.setStars(RatingScale.stars(rating));
        setContentDescription(getContext().getString(R.string.cmp_rating_a11y, rating, RatingScale.grade(rating),
                RatingScale.stars(rating)));
    }
}
