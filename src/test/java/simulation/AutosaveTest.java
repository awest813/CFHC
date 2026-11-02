package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * The Android app keeps progress across process death with a rolling autosave:
 * the league is serialized on its own thread, the bytes are written atomically
 * on a background thread, and a recreated activity resumes from that file.
 */
public class AutosaveTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private FileSystemResourceProvider resources;
    private League league;
    private File filesDir;

    @Before
    public void setUp() throws IOException {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false);
        league.setPlatformResourceProvider(resources);
        Team user = league.getTeamList().get(0);
        user.setUserControlled(true);
        user.getHeadCoach().user = true; // saves identify the user team by its coach
        league.userTeam = user;
        filesDir = tmp.newFolder("files");
    }

    @Test
    public void serializeSave_matchesSaveLeagueOutput() throws Exception {
        File viaSave = new File(filesDir, "viaSave.cfb");
        assertTrue(league.saveLeague(viaSave));
        byte[] snapshot = league.serializeSave();
        assertArrayEquals(Files.readAllBytes(viaSave.toPath()), snapshot);
    }

    @Test
    public void writeAtomically_replacesExistingFileAndLeavesNoTemp() throws Exception {
        File target = new File(filesDir, "slot.cfb");
        Files.write(target.toPath(), "old".getBytes());
        assertTrue(LeagueSaveStorage.writeAtomically(target, "new".getBytes()));
        assertEquals("new", new String(Files.readAllBytes(target.toPath())));
        assertFalse(new File(target.getAbsolutePath() + ".tmp").exists());
    }

    @Test
    public void writeAtomically_failureKeepsPreviousSave() throws Exception {
        File missingDir = new File(filesDir, "does-not-exist");
        File target = new File(missingDir, "slot.cfb");
        assertFalse(LeagueSaveStorage.writeAtomically(target, "data".getBytes()));
        assertFalse(target.exists());
    }

    @Test
    public void autosave_isNotASlot() throws Exception {
        assertTrue(league.saveLeague(LeagueSaveStorage.getAutosaveFile(filesDir)));
        assertFalse("autosave must not count as a slot save", LeagueSaveStorage.hasAnySave(filesDir));
        for (String info : LeagueSaveStorage.getSaveFileInfos(filesDir)) {
            assertEquals("EMPTY", info);
        }
        SaveLoadService service = new SaveLoadService(filesDir);
        assertNotNull("autosave should be offered for resume", service.getAutosaveSummary());
    }

    @Test
    public void autosaveSummary_nullWhenMissing() {
        assertNull(new SaveLoadService(filesDir).getAutosaveSummary());
    }

    @Test
    public void resumeAutosave_restoresLeagueWithoutSeasonGoalsPrompt() throws Exception {
        String userTeamName = league.userTeam.getName();
        File autosave = LeagueSaveStorage.getAutosaveFile(filesDir);
        assertTrue(LeagueSaveStorage.writeAtomically(autosave, league.serializeSave()));

        LeagueLaunchCoordinator.LaunchResult result = LeagueLaunchCoordinator.load(
                LeagueLaunchCoordinator.LaunchRequest.resumeAutosave(),
                filesDir, null, league.getYear(),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                uri -> { throw new IOException("unused"); },
                uri -> { throw new IOException("unused"); });

        assertTrue(result.loadedLeague);
        assertFalse(result.showSeasonGoals);
        assertNotNull(result.userTeam);
        assertEquals(userTeamName, result.userTeam.getName());
        assertEquals(league.getYear(), result.league.getYear());
        assertEquals(league.currentWeek, result.league.currentWeek);
    }

    @Test(expected = IOException.class)
    public void resumeAutosave_missingFileFailsInsteadOfGeneratingNewLeague() throws Exception {
        LeagueLaunchCoordinator.load(
                LeagueLaunchCoordinator.LaunchRequest.resumeAutosave(),
                filesDir, null, 2026,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                uri -> { throw new IOException("unused"); },
                uri -> { throw new IOException("unused"); });
    }
}
