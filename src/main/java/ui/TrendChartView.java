package ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import antdroid.cfbcoach.R;
import simulation.TrendChartModel;

/**
 * Season-by-season line chart (prestige or rank history) in the HUD style:
 * muted gridlines with axis labels, an emerald line, and a dot per season.
 * Replaces the GraphView library; layout math lives in {@link TrendChartModel}.
 */
public class TrendChartView extends View {

    private static final int GRID_STEPS = 4;

    private TrendChartModel model;
    private int maxXLabels = 5;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public TrendChartView(Context context) {
        this(context, null);
    }

    public TrendChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.cf_border));
        gridPaint.setStrokeWidth(dp(1));
        labelPaint.setColor(ContextCompat.getColor(context, R.color.cf_text_secondary));
        labelPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
                getResources().getDisplayMetrics()));
        linePaint.setColor(ContextCompat.getColor(context, R.color.cf_emerald));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        dotPaint.setColor(ContextCompat.getColor(context, R.color.cf_emerald));
    }

    public void setModel(TrendChartModel model, int maxXLabels) {
        this.model = model;
        this.maxXLabels = Math.max(1, maxXLabels);
        setContentDescription(describe(model));
        invalidate();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    /** Screen readers get the first and last season instead of a silent picture. */
    private static String describe(TrendChartModel m) {
        if (m == null || m.size() == 0) return "";
        return m.xLabel(0) + " to " + m.xLabel(m.size() - 1) + ", " + m.size() + " seasons";
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (model == null || model.size() == 0) {
            return;
        }
        float textH = labelPaint.getTextSize();
        float yLabelW = 0;
        for (int s = 0; s <= GRID_STEPS; s++) {
            yLabelW = Math.max(yLabelW, labelPaint.measureText(model.gridLabel(s, GRID_STEPS)));
        }
        float left = getPaddingLeft() + yLabelW + dp(8);
        float right = getWidth() - getPaddingRight() - dp(8);
        float top = getPaddingTop() + textH;
        float bottom = getHeight() - getPaddingBottom() - textH - dp(8);
        if (right <= left || bottom <= top) {
            return;
        }
        float w = right - left;
        float h = bottom - top;

        labelPaint.setTextAlign(Paint.Align.RIGHT);
        for (int s = 0; s <= GRID_STEPS; s++) {
            float y = bottom - (float) model.gridFraction(s, GRID_STEPS) * h;
            canvas.drawLine(left, y, right, y, gridPaint);
            canvas.drawText(model.gridLabel(s, GRID_STEPS), left - dp(6), y + textH / 3, labelPaint);
        }

        labelPaint.setTextAlign(Paint.Align.CENTER);
        for (int i : model.xLabelIndexes(maxXLabels)) {
            float x = left + (float) model.xFraction(i) * w;
            canvas.drawText(model.xLabel(i), x, bottom + textH + dp(4), labelPaint);
        }

        path.reset();
        float r = dp(3);
        for (int i = 0; i < model.size(); i++) {
            float x = left + (float) model.xFraction(i) * w;
            float y = bottom - (float) model.yFraction(i) * h;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        if (model.size() > 1) {
            canvas.drawPath(path, linePaint);
        }
        for (int i = 0; i < model.size(); i++) {
            float x = left + (float) model.xFraction(i) * w;
            float y = bottom - (float) model.yFraction(i) * h;
            canvas.drawCircle(x, y, r, dotPaint);
        }
    }
}
