package simulation;

/**
 * Platform-neutral state machine for the optional coach/roster import flow.
 * Hosts render {@link Step#prompt} however they like, then feed choices back
 * through this object.
 */
public final class LeagueImportWorkflow {
    public enum ImportType {
        COACH,
        ROSTER
    }

    public enum Prompt {
        IMPORT_DATA,
        IMPORT_TYPE,
        IMPORT_MORE,
        COMPLETE
    }

    public static final class Step {
        public final Prompt prompt;
        public final ImportType requestedImportType;
        public final boolean finishImportFlowForNewGame;

        private Step(Prompt prompt, ImportType requestedImportType, boolean finishImportFlowForNewGame) {
            this.prompt = prompt;
            this.requestedImportType = requestedImportType;
            this.finishImportFlowForNewGame = finishImportFlowForNewGame;
        }
    }

    private final boolean newGame;
    private Prompt prompt = Prompt.IMPORT_DATA;

    public LeagueImportWorkflow(boolean newGame) {
        this.newGame = newGame;
    }

    public Step currentStep() {
        return step(null);
    }

    public Step chooseImportData(boolean yes) {
        prompt = yes ? Prompt.IMPORT_TYPE : Prompt.COMPLETE;
        return step(null);
    }

    public Step selectImportType(ImportType importType) {
        if (importType == null) {
            throw new IllegalArgumentException("importType is required");
        }
        prompt = Prompt.IMPORT_MORE;
        return step(importType);
    }

    public Step cancelImportType() {
        prompt = Prompt.IMPORT_MORE;
        return step(null);
    }

    public Step chooseImportMore(boolean yes) {
        prompt = yes ? Prompt.IMPORT_TYPE : Prompt.COMPLETE;
        return step(null);
    }

    private Step step(ImportType requestedImportType) {
        return new Step(prompt, requestedImportType, newGame && prompt == Prompt.COMPLETE);
    }
}
