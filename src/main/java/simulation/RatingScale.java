package simulation;

/**
 * How a 0-100 rating is shown: a letter grade and a 0-5 star count. Desktop's
 * Team Overall card and Android's rating badge use the same scale.
 */
public final class RatingScale {

    private RatingScale() {
    }

    public static String grade(int rating) {
        if (rating >= 90) return "A+";
        if (rating >= 85) return "A";
        if (rating >= 80) return "A-";
        if (rating >= 75) return "B+";
        if (rating >= 70) return "B";
        if (rating >= 65) return "B-";
        if (rating >= 60) return "C+";
        if (rating > 0) return "C";
        return "N/A";
    }

    /** Whole stars, 0-5. */
    public static int stars(int rating) {
        return rating >= 92 ? 5 : rating >= 82 ? 4 : rating >= 70 ? 3 : rating >= 60 ? 2 : rating > 0 ? 1 : 0;
    }

    /** Parses a star count such as "4" or "3.5" (recruit strings); clamps to 0-5, 0 when unreadable. */
    public static float parseStars(String text) {
        if (text == null) return 0f;
        try {
            float v = Float.parseFloat(text.trim().split("\\s+")[0]);
            return Math.max(0f, Math.min(5f, v));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
