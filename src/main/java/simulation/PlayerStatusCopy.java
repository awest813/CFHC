package simulation;

import positions.Player;

/**
 * Canonical player-facing status labels shared by roster lists, depth charts,
 * and profile dialogs so Android and desktop stay consistent.
 */
public final class PlayerStatusCopy {
    private PlayerStatusCopy() {
    }

    public static String weeksLabel(int weeks) {
        return weeks == 1 ? "1 wk" : weeks + " wks";
    }

    /** Short profile/status word used in attribute sheets. */
    public static String profileStatus(Player p) {
        if (p == null) {
            return "Active";
        }
        if (p.isTransfer) {
            return "Transfer";
        }
        if (p.isRedshirt) {
            return "Redshirt";
        }
        if (p.isMedicalRS) {
            return "Medical Redshirt";
        }
        if (p.isInjured) {
            return "Injured";
        }
        if (p.isSuspended) {
            return "Suspended";
        }
        return "Active";
    }

    /**
     * Bracket tag appended to roster lines (keeps substrings UI color code depends on:
     * {@code INJ}, {@code RS}, {@code [T]}, {@code Suspended}).
     */
    public static String rosterTag(Player p) {
        if (p == null) {
            return "";
        }
        if (p.isSuspended) {
            return " [Suspended - " + weeksLabel(p.weeksSuspended) + "]";
        }
        if (p.isMedicalRS) {
            return " [Med RS]";
        }
        if (p.isTransfer) {
            return " [T]";
        }
        if (p.isRedshirt) {
            return " [RS]";
        }
        if (p.isInjured && p.injury != null) {
            return " [INJ - " + weeksLabel(p.injury.duration) + "]";
        }
        if (p.isInjured) {
            return " [INJ]";
        }
        return "";
    }

    /** Injury detail for depth-chart / one-line displays. */
    public static String injuryDetail(Injury injury, boolean medicalRedshirt) {
        if (injury == null) {
            return "";
        }
        String base = injury.getDescription() + " (" + weeksLabel(injury.duration) + ")";
        return medicalRedshirt ? base + " Med RS" : base;
    }
}
