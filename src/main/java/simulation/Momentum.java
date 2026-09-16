package simulation;

/**
 * In-game momentum, stored from the home team's perspective in [-1, 1].
 * Scores and turnovers push the value toward the benefiting side; every snap
 * decays it. The per-play effect is a small, bounded variance modifier.
 */
final class Momentum {

    private double value;

    void reset() {
        value = 0;
    }

    /** A scoring play (TD = 6, FG = 3, safety = 2) by the home ({@code true}) side. */
    void score(boolean homeScored, int points) {
        double swing = Math.min(0.22, 0.05 + points * 0.025);
        add(homeScored ? swing : -swing);
    }

    /** A turnover caught by the home ({@code true}) side. */
    void turnover(boolean homeBeneficiary) {
        add(homeBeneficiary ? 0.18 : -0.18);
    }

    /** Decay toward neutral after each snap. */
    void decay() {
        value *= 0.995;
        if (Math.abs(value) < 0.02) {
            value = 0;
        }
    }

    double value() {
        return value;
    }

    /** Per-play advantage for the given side, bounded to ±2. */
    int adjFor(boolean homeSide) {
        int adj = (int) Math.round(value * 2.2);
        return homeSide ? adj : -adj;
    }

    private void add(double delta) {
        value = Math.max(-1.0, Math.min(1.0, value + delta));
    }
}
