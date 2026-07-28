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

    @Test
    public void ensureLoaded_noUserTeamLeavesStoreEmpty() {
        league.userTeam = null;
        DesktopRecruitingSessionStore store = new DesktopRecruitingSessionStore();
        store.ensureLoaded(league, null);
        assertFalse(store.hasSession());
        assertNull(store.session());
    }

    @Test
    public void migrateAfterSaveAs_keepsOldCheckpointWhenNewPathUnwritable() throws Exception {
        DesktopRecruitingSessionStore store = new DesktopRecruitingSessionStore();
        File oldSave = Files.createTempFile("cfhc-mig-old", ".cfb").toFile();
        oldSave.deleteOnExit();
        store.ensureLoaded(league, oldSave);
        assertNull(store.persist(league, oldSave));
        File oldChk = DesktopRecruitingCheckpoint.pathFor(oldSave, league);
        assertTrue(oldChk.isFile());

        File blocker = Files.createTempFile("cfhc-mig-block", ".tmp").toFile();
        blocker.deleteOnExit();
        // Parent is a file, so mkdir for the sidecar must fail.
        File badSave = new File(blocker, "league.cfb");
        store.clearMemory();
        store.migrateAfterSaveAs(league, oldSave, badSave);
        assertTrue("Old checkpoint must survive failed migration", oldChk.isFile());
    }
}
