package desktop;

import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DesktopRecruitingSessionStoreTest {

    private League league;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
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
    }

    @Test
    public void ensureLoaded_createsSessionAndPersistRoundTrips() throws Exception {
        DesktopRecruitingSessionStore store = new DesktopRecruitingSessionStore();
        File save = Files.createTempFile("cfhc-store", ".cfb").toFile();
        save.deleteOnExit();

        store.ensureLoaded(league, save);
        assertTrue(store.hasSession());
        assertNotNull(store.session());
        int budget = store.session().recruitingBudget;
        store.session().recruitingBudget = Math.max(1, budget - 25);

        assertNull(store.persist(league, save));
        File chk = DesktopRecruitingCheckpoint.pathFor(save, league);
        assertTrue(chk.isFile());

        DesktopRecruitingSessionStore reloaded = new DesktopRecruitingSessionStore();
        reloaded.ensureLoaded(league, save);
        assertEquals(store.session().recruitingBudget, reloaded.session().recruitingBudget);

        File saveAs = Files.createTempFile("cfhc-store-as", ".cfb").toFile();
        saveAs.deleteOnExit();
        reloaded.migrateAfterSaveAs(league, save, saveAs);
        assertFalse(chk.isFile());
        assertTrue(DesktopRecruitingCheckpoint.pathFor(saveAs, league).isFile());

        reloaded.clearAll(league, saveAs);
        assertFalse(reloaded.hasSession());
        assertFalse(DesktopRecruitingCheckpoint.pathFor(saveAs, league).isFile());
    }
}
