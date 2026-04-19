package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests that {@link League#saveLeague(File)} and the save-file loading
 * constructor produce a league that is semantically equivalent to the
 * original — i.e. that the full round-trip (save → reload) is lossless for
 * all observable properties.
 */
public class SaveRoundTripTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private League original;
    private DesktopResourceProvider resources;

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        resources = new DesktopResourceProvider(projectRoot);

        original = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        original.setPlatformResourceProvider(resources);

        assertFalse("League should have teams", original.teamList.isEmpty());
        original.userTeam = original.teamList.get(0);
        original.userTeam.userControlled = true;
    }

    // -------------------------------------------------------------------------
    // Round-trip via saveLeague / File constructor
    // -------------------------------------------------------------------------

    @Test
    public void roundTrip_teamCount_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        assertEquals("Team count must survive round-trip",
                original.teamList.size(), loaded.teamList.size());
    }

    @Test
    public void roundTrip_conferenceCount_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        assertEquals("Conference count must survive round-trip",
                original.conferences.size(), loaded.conferences.size());
    }

    @Test
    public void roundTrip_leagueName_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        assertEquals("League name must survive round-trip",
                original.leagueName, loaded.leagueName);
    }

    @Test
    public void roundTrip_year_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        assertEquals("Year must survive round-trip",
                original.getYear(), loaded.getYear());
    }

    @Test
    public void roundTrip_teamPrestige_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        for (int i = 0; i < original.teamList.size(); i++) {
            Team orig = original.teamList.get(i);
            Team reloaded = findTeam(loaded, orig.name);
            assertNotNull("Team " + orig.name + " must exist after reload", reloaded);
            assertEquals("Prestige for " + orig.name + " must survive round-trip",
                    orig.teamPrestige, reloaded.teamPrestige);
        }
    }

    @Test
    public void roundTrip_rosterSize_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        for (int i = 0; i < original.teamList.size(); i++) {
            Team orig = original.teamList.get(i);
            Team reloaded = findTeam(loaded, orig.name);
            assertNotNull("Team " + orig.name + " must exist after reload", reloaded);
            assertEquals("Roster size for " + orig.name + " must survive round-trip",
                    orig.getAllPlayers().size(), reloaded.getAllPlayers().size());
        }
    }

    @Test
    public void roundTrip_userTeam_nameDataIsPreserved() throws Exception {
        // The new save/load format (LeagueRecord) does not track which team is
        // user-controlled — callers must re-assign userTeam after loading.
        // This test verifies that the user team's *data* (roster, prestige, etc.)
        // survives the round-trip so it can be re-assigned correctly.
        League loaded = saveAndLoad();
        String originalName = original.userTeam.name;
        Team reloaded = findTeam(loaded, originalName);
        assertNotNull("User team data must survive round-trip: " + originalName, reloaded);
        assertEquals("User team prestige must survive round-trip",
                original.userTeam.teamPrestige, reloaded.teamPrestige);
        assertEquals("User team roster size must survive round-trip",
                original.userTeam.getAllPlayers().size(), reloaded.getAllPlayers().size());
    }

    @Test
    public void roundTrip_headCoachName_isPreserved() throws Exception {
        League loaded = saveAndLoad();
        for (Team orig : original.teamList) {
            Team reloaded = findTeam(loaded, orig.name);
            assertNotNull("Team " + orig.name + " must exist after reload", reloaded);
            assertNotNull("HC for " + orig.name + " must not be null after reload", reloaded.HC);
            assertEquals("HC name for " + orig.name + " must survive round-trip",
                    orig.HC.name, reloaded.HC.name);
        }
    }

    @Test
    public void roundTrip_viaSaveManagerDirectly() throws Exception {
        // Verify that the LeagueRecord produced by toRecord() can be serialized and
        // deserialized via SaveManager round-trip with no data loss.
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        SaveManager.save(original.toRecord(), baos);

        byte[] bytes = baos.toByteArray();
        assertTrue("Saved bytes must not be empty", bytes.length > 0);

        LeagueRecord record = SaveManager.load(new java.io.ByteArrayInputStream(bytes));
        assertNotNull("Loaded record must not be null", record);
        assertEquals("League name must survive SaveManager round-trip",
                original.leagueName, record.leagueName());
        assertEquals("Year must survive SaveManager round-trip",
                original.getYear(), record.year());
        int origTeams = original.conferences.stream()
                .mapToInt(c -> c.confTeams.size()).sum();
        int loadedTeams = record.conferences().stream()
                .mapToInt(c -> c.teams().size()).sum();
        assertEquals("Team count must survive SaveManager round-trip",
                origTeams, loadedTeams);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private League saveAndLoad() throws Exception {
        File save = tmp.newFile("league_test.cfb");
        assertTrue("League must save successfully", original.saveLeague(save));

        League loaded = new League(save,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        loaded.setPlatformResourceProvider(resources);
        return loaded;
    }

    private Team findTeam(League league, String name) {
        return league.getTeamList().stream()
                .filter(t -> t.name.equals(name))
                .findFirst()
                .orElse(null);
    }
}
