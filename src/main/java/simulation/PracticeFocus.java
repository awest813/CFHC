package simulation;

import java.util.Locale;

/**
 * Weekly / seasonal practice emphasis for user-controlled teams.
 * Biases player attribute growth toward football IQ, fundamentals (primary traits),
 * athletic traits, or conditioning — similar to dynasty-style practice focuses.
 */
public enum PracticeFocus {
    BALANCED,
    FOOTBALL_IQ,
    FUNDAMENTALS,
    ATHLETICISM,
    PHYSICAL;

    public static PracticeFocus fromSave(String raw) {
        if (raw == null || raw.isEmpty()) {
            return BALANCED;
        }
        try {
            return PracticeFocus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BALANCED;
        }
    }

    public String toSave() {
        return name();
    }

    public String displayName() {
        return switch (this) {
            case BALANCED -> "Balanced";
            case FOOTBALL_IQ -> "Football IQ & film";
            case FUNDAMENTALS -> "Position fundamentals";
            case ATHLETICISM -> "Speed & athleticism";
            case PHYSICAL -> "Strength & durability";
        };
    }

    public String shortDescription() {
        return switch (this) {
            case BALANCED -> "Even development across all traits.";
            case FOOTBALL_IQ -> "Extra growth in awareness and football IQ.";
            case FUNDAMENTALS -> "Extra growth in core position skills (ratings 1–2).";
            case ATHLETICISM -> "Extra growth in movement traits (ratings 3–4).";
            case PHYSICAL -> "Extra growth in durability and physical traits.";
        };
    }
}
