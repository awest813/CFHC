package simulation;

import java.util.Locale;

/**
 * Whether offseason realignment may promote lower-division (FCS) schools into the
 * league. Promotion only happens under advanced realignment, as a new Independent
 * or as members of a newly formed conference.
 */
public enum FcsPromotionMode {
    /** No FCS school ever moves up; the team count only changes by pro/rel filler. */
    NONE("No promotions"),
    /** At most {@link League#fcsPromotionCap} FCS schools move up over the whole career. */
    CAPPED("Capped"),
    /** Original behavior: any FCS school still in the name pool can move up. */
    UNLIMITED("Unlimited");

    private final String label;

    FcsPromotionMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /** @return the mode named {@code name} (case-insensitive), or {@code fallback} when unknown */
    public static FcsPromotionMode parse(String name, FcsPromotionMode fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
