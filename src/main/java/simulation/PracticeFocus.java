package simulation;

import java.util.Locale;

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
            case FUNDAMENTALS -> "Extra growth in core position skills (ratings 1\u20132).";
            case ATHLETICISM -> "Extra growth in movement traits (ratings 3\u20134).";
            case PHYSICAL -> "Extra growth in durability and physical traits.";
        };
    }

    public enum PositionGroup {
        ALL,
        QB,
        RB,
        WR,
        TE,
        OL,
        DL,
        LB,
        CB,
        S,
        K;

        public static PositionGroup fromSave(String raw) {
            if (raw == null || raw.isEmpty()) return ALL;
            try {
                return PositionGroup.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ALL;
            }
        }

        public String toSave() {
            return name();
        }

        public String displayName() {
            return switch (this) {
                case ALL -> "All Positions";
                case QB -> "QB";
                case RB -> "RB";
                case WR -> "WR";
                case TE -> "TE";
                case OL -> "OL";
                case DL -> "DL";
                case LB -> "LB";
                case CB -> "CB";
                case S -> "S";
                case K -> "K";
            };
        }

        public boolean matches(String position) {
            if (this == ALL) return true;
            return name().equals(position);
        }
    }

    public enum FocusIntensity {
        NORMAL,
        INTENSE;

        public static FocusIntensity fromSave(String raw) {
            if (raw == null || raw.isEmpty()) return NORMAL;
            try {
                return FocusIntensity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return NORMAL;
            }
        }

        public String toSave() {
            return name();
        }

        public String displayName() {
            return switch (this) {
                case NORMAL -> "Normal";
                case INTENSE -> "Intense (+20% growth, +10% injury risk)";
            };
        }

        public String shortDescription() {
            return switch (this) {
                case NORMAL -> "Standard practice intensity.";
                case INTENSE -> "Players grow faster but have a 10% higher chance of injury each week.";
            };
        }

        public double growthMultiplier() {
            return switch (this) {
                case NORMAL -> 1.0;
                case INTENSE -> 1.2;
            };
        }

        public double injuryModifier() {
            return switch (this) {
                case NORMAL -> 0.0;
                case INTENSE -> 0.10;
            };
        }
    }
}
