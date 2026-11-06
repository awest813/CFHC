package simulation;

/**
 * Platform-neutral math for a simple line chart of one value per season
 * (prestige or national rank over time). Shells draw it; this class decides
 * where each point goes and which labels to show, so the layout is testable
 * without a UI toolkit.
 */
public final class TrendChartModel {

    /** Turns a y value into its axis label (e.g. a stored "rank score" back into a rank). */
    public interface ValueLabeler {
        String label(double value);
    }

    private final double[] values;
    private final String[] xLabels;
    private final double minY;
    private final double maxY;
    private final ValueLabeler yLabeler;

    public TrendChartModel(double[] values, String[] xLabels, double minY, double maxY, ValueLabeler yLabeler) {
        if (values == null || xLabels == null || values.length != xLabels.length) {
            throw new IllegalArgumentException("values and xLabels must have the same length");
        }
        this.values = values.clone();
        this.xLabels = xLabels.clone();
        double lo = Math.min(minY, maxY);
        double hi = Math.max(minY, maxY);
        if (hi - lo < 1e-9) {
            hi = lo + 1; // flat range: give the line somewhere to sit
        }
        this.minY = lo;
        this.maxY = hi;
        this.yLabeler = yLabeler != null ? yLabeler : v -> Integer.toString((int) Math.round(v));
    }

    public int size() {
        return values.length;
    }

    public String xLabel(int i) {
        return xLabels[i];
    }

    /** Horizontal position in [0, 1]; a single point sits in the middle. */
    public double xFraction(int i) {
        return values.length <= 1 ? 0.5 : (double) i / (values.length - 1);
    }

    /** Vertical position in [0, 1] with 0 at the bottom; values outside the range are clamped. */
    public double yFraction(int i) {
        double v = Math.max(minY, Math.min(maxY, values[i]));
        return (v - minY) / (maxY - minY);
    }

    /**
     * Indexes of the x labels to draw: at most {@code maxLabels}, evenly
     * spread, always including the first and last season.
     */
    public int[] xLabelIndexes(int maxLabels) {
        int n = values.length;
        if (n == 0 || maxLabels <= 0) {
            return new int[0];
        }
        int count = Math.min(n, Math.max(1, maxLabels));
        if (count == 1) {
            return new int[]{n - 1};
        }
        int[] out = new int[count];
        for (int k = 0; k < count; k++) {
            out[k] = (int) Math.round((double) k * (n - 1) / (count - 1));
        }
        return out;
    }

    /** {@code steps + 1} evenly spaced y gridlines from bottom to top: fraction and label. */
    public double gridFraction(int step, int steps) {
        return steps <= 0 ? 0 : (double) step / steps;
    }

    public String gridLabel(int step, int steps) {
        return yLabeler.label(minY + (maxY - minY) * gridFraction(step, steps));
    }
}
