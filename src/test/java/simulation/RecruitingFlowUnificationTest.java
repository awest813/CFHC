package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Shared NLI recruiting setup used by desktop and Android.
 */
public class RecruitingFlowUnificationTest {

    private League league;
    private File filesDir;
    private SaveLoadService saveLoadService;
    private SimulationFacade facade;

    @Before
    public void setUp() throws IOException {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
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
        league.currentWeek = league.regSeasonWeeks + 13;
        league.recruitingPhaseActive = true;

        filesDir = File.createTempFile("cfhc-recruiting", "");
        assertTrue(filesDir.delete());
        assertTrue(filesDir.mkdir());
        saveLoadService = new SaveLoadService(filesDir);
        facade = new SimulationFacade(filesDir, resources);
        facade.setLeague(league, league.userTeam, league.userTeam);
    }

    @Test
    public void saveForUserRecruitingUi_preservesRecruitingWeek() throws Exception {
        int weekBefore = league.currentWeek;

        SimulationFacade.prepareCpuRecruiting(league);
        String payload = SimulationFacade.saveForUserRecruitingUi(league, league.userTeam, saveLoadService);

        assertEquals(weekBefore, league.currentWeek);
        assertNotNull(payload);
        assertTrue(payload.contains("END_TEAM_INFO"));

        File recruitingSave = LeagueSaveStorage.getRecruitingSaveFile(filesDir);
        assertTrue(recruitingSave.exists());

        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League loaded = new League(
                recruitingSave,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                GameUiBridge.NO_OP,
                false
        );
        assertEquals(weekBefore, loaded.currentWeek);
        assertTrue(loaded.recruitingPhaseActive);
    }

    @Test
    public void beginUserRecruitingFlow_observerSkipsInteractiveRecruiting() throws Exception {
        league.userTeam.setUserControlled(false);

        String payload = facade.beginUserRecruitingFlow();

        assertNull(payload);
        assertEquals(0, league.currentWeek);
        assertFalse(league.recruitingPhaseActive);
    }

    @Test
    public void beginUserRecruitingFlow_userTeamReturnsPayloadWithoutChangingWeek() throws Exception {
        int weekBefore = league.currentWeek;

        String payload = facade.beginUserRecruitingFlow();

        assertNotNull(payload);
        assertEquals(weekBefore, league.currentWeek);
        assertTrue(league.recruitingPhaseActive);
    }
}
