package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;

import recruiting.RecruitingSessionData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SimulationFacadeTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private DesktopResourceProvider resources;

    @Before
    public void setUp() {
        resources = new DesktopResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void loadDefaultLeague_setsLeagueAndSeasonController() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);

        LeagueLaunchCoordinator.LaunchResult result = facade.loadDefaultLeague();

        assertNotNull(result.league);
        assertNotNull(facade.getLeague());
        assertEquals(result.league, facade.getLeague());
        assertTrue(facade.getLeague().getTeamList().size() > 0);
    }

    @Test
    public void saveToSlot_usesSharedSaveLoadService() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        facade.selectUserTeam(facade.getLeague().getTeamList().get(0));

        assertTrue(facade.saveToSlot(1));
        assertTrue(facade.getSaveLoadService().getSlotFile(1).isFile());
    }

    @Test
    public void loadInternalSlot_loadsSavedLeagueThroughFacade() throws Exception {
        SimulationFacade source = new SimulationFacade(tmp.getRoot(), resources);
        source.loadDefaultLeague();
        source.selectUserTeam(source.getLeague().getTeamList().get(0));
        assertTrue(source.saveToSlot(2));

        SimulationFacade loaded = new SimulationFacade(tmp.getRoot(), resources);
        LeagueLaunchCoordinator.LaunchResult result = loaded.loadInternalSlot(2, source.getUserTeam().getName());

        assertNotNull(result.league);
        assertNotNull(loaded.getUserTeam());
        assertEquals(source.getLeague().userTeam.getName(), loaded.getUserTeam().getName());
    }

    @Test
    public void importSave_selectsUserTeamForPortableSave() throws Exception {
        League league = newLeague();
        Team userTeam = league.getTeamList().get(3);
        File save = tmp.newFile("portable.cfb");
        assertTrue(league.saveLeague(save));

        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        LeagueLaunchCoordinator.LaunchResult result = facade.importSave(
                save.getAbsolutePath(),
                userTeam.getName(),
                uri -> new FileInputStream(uri)
        );

        assertNotNull(result.league);
        assertEquals(userTeam.getName(), facade.getUserTeam().getName());
    }

    @Test
    public void setLeague_assignsFacadeUserTeamToLeague() {
        League league = newLeague();
        Team selectedTeam = league.getTeamList().get(2);
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);

        facade.setLeague(league, selectedTeam, null);

        assertEquals(selectedTeam, facade.getUserTeam());
        assertEquals(selectedTeam, league.userTeam);
        assertTrue(selectedTeam.userControlled);
    }

    @Test
    public void prepareRecruitingSession_buildsPortableRecruitingState() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        Team userTeam = facade.getLeague().getTeamList().get(0);
        facade.selectUserTeam(userTeam);

        RecruitingSessionData session = facade.prepareRecruitingSession();

        assertEquals(userTeam.getName(), session.teamName);
        assertTrue(session.recruitingBudget > 0);
        assertTrue(session.availAll.size() > 0);
    }

    @Test
    public void facadeImportsRosterThroughSharedImporter() throws Exception {
        SimulationFacade facade = new SimulationFacade(tmp.getRoot(), resources);
        facade.loadDefaultLeague();
        Team team = facade.getLeague().getTeamList().get(0);
        String csv = "Team,Name,Position,Rating,Year\n"
                + team.getName() + ",Facade Passer,QB,90,1\n"
                + "END_ROSTER\n";

        facade.importRoster(new java.io.ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        assertNotNull(team.getTeamQBs().stream()
                .filter(player -> player.name.equals("Facade Passer"))
                .findFirst()
                .orElse(null));
    }

    private League newLeague() {
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
        return league;
    }
}
