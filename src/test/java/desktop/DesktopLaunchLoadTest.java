package desktop;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import simulation.GameUiBridge;
import simulation.League;
import simulation.LeagueLaunchCoordinator;
import simulation.PlatformResourceProvider;
import simulation.SaveLoadService;
import simulation.SeasonController;
import simulation.SimulationFacade;
import simulation.Team;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class DesktopLaunchLoadTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private DesktopResourceProvider resources;

    @Before
    public void setUp() {
        resources = new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void facadeNewGame_producesPlayableLeague() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        LeagueLaunchCoordinator.LaunchResult result = facade.loadDefaultLeague();

        assertNotNull(result.league);
        assertTrue(result.league.getTeamList().size() > 0);
        assertTrue("Year should be positive", result.league.getYear() > 0);
    }

    @Test
    public void facadeNewGame_selectUserTeam_setsUserControlled() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        Team selected = facade.getLeague().getTeamList().get(0);
        facade.selectUserTeam(selected);

        assertTrue(selected.isUserControlled());
        assertEquals(selected, facade.getUserTeam());
        assertEquals(selected, facade.getLeague().userTeam);
    }

    @Test
    public void facadeNewGame_saveAndLoad_roundTrip() throws Exception {
        SimulationFacade source = new SimulationFacade(tmp.getRoot(), resources);
        source.loadDefaultLeague();
        Team userTeam = source.getLeague().getTeamList().get(5);
        source.selectUserTeam(userTeam);
        assertTrue(source.saveToSlot(1));

        SimulationFacade loaded = new SimulationFacade(tmp.getRoot(), resources);
        LeagueLaunchCoordinator.LaunchResult result = loaded.loadInternalSlot(1, userTeam.getName());

        assertNotNull(result.league);
        assertEquals(userTeam.getName(), loaded.getUserTeam().getName());
        assertTrue(loaded.getUserTeam().isUserControlled());
        assertEquals(source.getLeague().getTeamList().size(), loaded.getLeague().getTeamList().size());
    }

    @Test
    public void facadeNewGame_advanceWeeksThroughSeason() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        facade.selectUserTeam(facade.getLeague().getTeamList().get(0));

        int teamCount = facade.getLeague().getTeamList().size();
        for (int i = 0; i < 5; i++) {
            facade.advanceWeek();
        }

        assertEquals(teamCount, facade.getLeague().getTeamList().size());
        assertTrue(facade.getLeague().currentWeek > 0);
    }

    @Test
    public void facadeSaveAndLoad_preservesYearAndWeek() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        facade.selectUserTeam(facade.getLeague().getTeamList().get(0));

        for (int i = 0; i < 8; i++) {
            facade.advanceWeek();
        }
        int expectedWeek = facade.getLeague().currentWeek;
        int expectedYear = facade.getLeague().getYear();
        String userTeamName = facade.getUserTeam().getName();

        assertTrue(facade.saveToSlot(3));

        SimulationFacade loaded = new SimulationFacade(tmp.getRoot(), resources);
        loaded.loadInternalSlot(3, userTeamName);

        assertEquals("Year should be preserved", expectedYear, loaded.getLeague().getYear());
        assertEquals("Week should be preserved", expectedWeek, loaded.getLeague().currentWeek);
    }

    @Test
    public void facadeImportSave_fromFile_producesValidLeague() throws Exception {
        League league = newLeague();
        File saveFile = tmp.newFile("export.cfb");
        assertTrue(league.saveLeague(saveFile));

        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        LeagueLaunchCoordinator.LaunchResult result = facade.importSave(
                saveFile.getAbsolutePath(),
                league.getTeamList().get(0).getName(),
                uri -> new FileInputStream(uri)
        );

        assertNotNull(result.league);
        assertTrue(result.league.getTeamList().size() > 0);
        assertEquals(league.getTeamList().get(0).getName(), facade.getUserTeam().getName());
    }

    @Test
    public void facadeMultipleSaves_independentSlots() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();

        Team team0 = facade.getLeague().getTeamList().get(0);
        Team team1 = facade.getLeague().getTeamList().get(1);

        facade.selectUserTeam(team0);
        assertTrue(facade.saveToSlot(1));

        facade.selectUserTeam(team1);
        assertTrue(facade.saveToSlot(2));

        SimulationFacade load1 = new SimulationFacade(tmp.getRoot(), resources);
        load1.loadInternalSlot(1, team0.getName());
        assertEquals(team0.getName(), load1.getUserTeam().getName());

        SimulationFacade load2 = new SimulationFacade(tmp.getRoot(), resources);
        load2.loadInternalSlot(2, team1.getName());
        assertEquals(team1.getName(), load2.getUserTeam().getName());
    }

    @Test
    public void facadeLoad_corruptedSlot_throwsIOException() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);

        boolean threw = false;
        try {
            facade.loadInternalSlot(99);
        } catch (IOException e) {
            threw = true;
        }
        assertTrue("Loading a non-existent slot should throw IOException", threw);
    }

    @Test
    public void facadeSaveLoad_allTeamsHaveCoaches() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        facade.selectUserTeam(facade.getLeague().getTeamList().get(0));

        assertTrue(facade.saveToSlot(4));

        SimulationFacade loaded = new SimulationFacade(tmp.getRoot(), resources);
        loaded.loadInternalSlot(4, facade.getUserTeam().getName());

        for (Team t : loaded.getLeague().getTeamList()) {
            assertNotNull(t.getName() + " should have a head coach", t.getHeadCoach());
        }
    }

    @Test
    public void saveLoadService_getSaveFileSummaries_returnsCorrectCount() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        facade.selectUserTeam(facade.getLeague().getTeamList().get(0));

        String[] summariesBefore = facade.getSaveFileSummaries();
        int emptyCount = 0;
        for (String s : summariesBefore) {
            if (s == null || s.isEmpty()) emptyCount++;
        }

        assertTrue(facade.saveToSlot(1));
        assertTrue(facade.saveToSlot(2));

        String[] summariesAfter = facade.getSaveFileSummaries();
        int filledCount = 0;
        for (String s : summariesAfter) {
            if (s != null && !s.isEmpty()) filledCount++;
        }
        assertTrue("At least 2 slots should be filled", filledCount >= 2);
    }

    private League newLeague() {
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        return league;
    }
}
