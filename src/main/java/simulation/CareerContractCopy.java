package simulation;

/**
 * Shared player-facing career/contract copy used by season summaries and
 * contract dialogs on both Android and desktop.
 */
public final class CareerContractCopy {
    private CareerContractCopy() {
    }

    public static String proveItExtension(int years) {
        return "Prove-it extension: " + years + " years based on recent momentum.";
    }

    public static String contractExtension(int years) {
        return "Contract extended: " + years + " years after this season's progress.";
    }

    public static String terminated() {
        return "Contract terminated. You are no longer the head coach at this school.";
    }

    public static String yearsRemaining(int yearsLeft, int prestige, int baseline, String status) {
        return yearsLeft + " years left on your contract. Prestige "
                + prestige + " (baseline " + baseline + "). Status: " + status + ".";
    }

    public static String seasonSummaryExtension(int years) {
        return "\n\nContract extension awarded: " + years + " years.";
    }

    public static String seasonSummaryFired() {
        return "\n\nFired: prestige goals were not met during your contract. The program may take an additional prestige hit.";
    }
}
