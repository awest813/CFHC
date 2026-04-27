package simulation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LeagueImportWorkflowTest {
    @Test
    public void noImportOnNewGame_finishesFlow() {
        LeagueImportWorkflow workflow = new LeagueImportWorkflow(true);

        LeagueImportWorkflow.Step step = workflow.chooseImportData(false);

        assertEquals(LeagueImportWorkflow.Prompt.COMPLETE, step.prompt);
        assertTrue(step.finishImportFlowForNewGame);
        assertNull(step.requestedImportType);
    }

    @Test
    public void selectCoach_requestsCoachThenAsksForMoreImports() {
        LeagueImportWorkflow workflow = new LeagueImportWorkflow(false);
        workflow.chooseImportData(true);

        LeagueImportWorkflow.Step step = workflow.selectImportType(LeagueImportWorkflow.ImportType.COACH);

        assertEquals(LeagueImportWorkflow.Prompt.IMPORT_MORE, step.prompt);
        assertEquals(LeagueImportWorkflow.ImportType.COACH, step.requestedImportType);
        assertFalse(step.finishImportFlowForNewGame);
    }

    @Test
    public void importMoreYes_returnsToTypePrompt() {
        LeagueImportWorkflow workflow = new LeagueImportWorkflow(false);
        workflow.chooseImportData(true);
        workflow.selectImportType(LeagueImportWorkflow.ImportType.ROSTER);

        LeagueImportWorkflow.Step step = workflow.chooseImportMore(true);

        assertEquals(LeagueImportWorkflow.Prompt.IMPORT_TYPE, step.prompt);
        assertNull(step.requestedImportType);
    }

    @Test
    public void noImportOnExistingGame_completesWithoutNewGameFinishSignal() {
        LeagueImportWorkflow workflow = new LeagueImportWorkflow(false);

        LeagueImportWorkflow.Step step = workflow.chooseImportData(false);

        assertEquals(LeagueImportWorkflow.Prompt.COMPLETE, step.prompt);
        assertFalse(step.finishImportFlowForNewGame);
    }

    @Test(expected = IllegalArgumentException.class)
    public void selectImportType_rejectsNullType() {
        new LeagueImportWorkflow(false).selectImportType(null);
    }
}
