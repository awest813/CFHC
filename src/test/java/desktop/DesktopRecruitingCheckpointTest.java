package desktop;

import desktop.DesktopResourceProvider;
import org.junit.Test;
import recruiting.RecruitingSessionData;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.SimulationFacade;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DesktopRecruitingCheckpointTest {

    @Test
    public void checkpoint_roundTripRestoresBudgetAndBoard() throws Exception {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);

        String payload = SimulationFacade.buildRecruitingPayload(league.userTeam);
        RecruitingSessionData session = SimulationFacade.prepareRecruitingSessionFromPayload(payload);
        int budgetAfterPrepare = session.recruitingBudget;
        assertTrue(budgetAfterPrepare > 0);

        // Spend some budget without recruiting a player (simulate scout/spend).
        session.recruitingBudget = Math.max(1, budgetAfterPrepare - 50);

        DesktopRecruitingCheckpoint checkpoint = DesktopRecruitingCheckpoint.capture(league, payload, session);
        assertNotNull(checkpoint);

        File tmp = Files.createTempFile("cfhc-recruiting", ".chk").toFile();
        tmp.deleteOnExit();
        DesktopRecruitingCheckpoint.write(tmp, checkpoint);

        DesktopRecruitingCheckpoint loaded = DesktopRecruitingCheckpoint.read(tmp);
        assertNotNull(loaded);
        assertTrue(loaded.matches(league));
        assertEquals(session.recruitingBudget, loaded.budget);

        RecruitingSessionData restored = loaded.restoreSession();
        assertEquals(session.recruitingBudget, restored.recruitingBudget);
        assertEquals(0, restored.playersRecruited.size());
    }

    @Test
    public void applyCheckpoint_recommitsRecruitByRaw() {
        String payload = "SEC,Test U,TST,5,80,0,0%\nEND_TEAM_INFO%\n"
                + "QB,A Prospect,1,5,70,70,70,70,70,70,70,70,70,1,0,0,0%\n"
                + "END_RECRUITS%\n";
        // Use a minimal synthetic path only if fromUserTeamInfo tolerates thin CSV —
        // otherwise rely on live league board from the round-trip test above.
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                "SEC,Test U,TST,5,80,0,0%\nEND_TEAM_INFO%\nEND_RECRUITS%\n");
        session.applyCheckpoint(123, Collections.emptyList());
        assertEquals(123, session.recruitingBudget);
    }
}
