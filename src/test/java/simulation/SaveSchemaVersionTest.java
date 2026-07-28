package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Schema versioning for {@link SaveManager} / {@link SaveSchema}.
 */
public class SaveSchemaVersionTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private League league;
    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
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
        league.leagueName = "Schema Version League";
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setupUserCoach("Schema Coach");
        league.userTeam.getHeadCoach().user = true;
        league.userTeam.setUserControlled(true);
    }

    @Test
    public void save_writesVersionHeaderBeforeLeagueLine() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);
        String text = baos.toString(StandardCharsets.UTF_8);
        String[] lines = text.split("\n", 3);
        assertTrue(lines[0].startsWith(SaveSchema.VERSION_PREFIX));
        assertEquals(SaveSchema.VERSION_PREFIX + League.CURRENT_SAVE_VERSION, lines[0].trim());
        assertTrue(lines[1].startsWith("L:"));
    }

    @Test
    public void load_readsVersionAndSetsLeagueSaveVer() throws Exception {
        File save = tmp.newFile("versioned.cfb");
        assertTrue(league.saveLeague(save));

        League loaded = new League(
                save,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        assertEquals(League.CURRENT_SAVE_VERSION, loaded.saveVer);
        assertEquals("Schema Version League", loaded.leagueName);
    }

    @Test
    public void load_unversionedLFormat_defaultsToCurrent() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);
        String withV = baos.toString(StandardCharsets.UTF_8);
        // Strip the V: line to simulate Wave A fixtures.
        String withoutV = withV.substring(withV.indexOf('\n') + 1);
        assertTrue(withoutV.startsWith("L:"));

        SaveManager.LoadResult result = SaveManager.loadWithVersion(
                new ByteArrayInputStream(withoutV.getBytes(StandardCharsets.UTF_8)));
        assertEquals(League.CURRENT_SAVE_VERSION, result.schemaVersion());
        assertEquals("Schema Version League", result.record().leagueName());
    }

    @Test
    public void load_unknownVersion_failsLoudly() {
        String bogus = SaveSchema.VERSION_PREFIX + "v9.9.9\n"
                + "L:Broken\t2026\t0\t\t\n";
        try {
            SaveManager.load(new ByteArrayInputStream(bogus.getBytes(StandardCharsets.UTF_8)));
            fail("Expected IOException for unsupported schema version");
        } catch (IOException ex) {
            assertTrue(ex.getMessage(), ex.getMessage().contains("Unsupported save schema version"));
            assertTrue(ex.getMessage(), ex.getMessage().contains("v9.9.9"));
        }
    }

    @Test
    public void saveFileSummary_showsCurrentVersionForNewFormat() throws Exception {
        File save = tmp.newFile("summary.cfb");
        assertTrue(league.saveLeague(save));
        String summary = SaveFileSummary.summarize(save, League.CURRENT_SAVE_VERSION);
        assertTrue(summary, summary.contains("Version: " + League.CURRENT_SAVE_VERSION));
        assertFalse(summary, summary.contains("Legacy Save"));
        assertTrue(summary, summary.contains("Schema Version League"));
    }

    @Test
    public void saveFileSummary_unversionedLFormat_stillShowsCurrent() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), baos);
        String withoutV = baos.toString(StandardCharsets.UTF_8);
        withoutV = withoutV.substring(withoutV.indexOf('\n') + 1);
        File save = tmp.newFile("legacy-new.cfb");
        Files.write(save.toPath(), withoutV.getBytes(StandardCharsets.UTF_8));

        String summary = SaveFileSummary.summarize(save, League.CURRENT_SAVE_VERSION);
        assertTrue(summary, summary.contains("Version: " + League.CURRENT_SAVE_VERSION));
    }

    @Test
    public void migrate_identityForCurrentVersion() throws Exception {
        LeagueRecord record = league.toRecord();
        LeagueRecord migrated = SaveSchema.migrate(League.CURRENT_SAVE_VERSION, record);
        assertSame(record, migrated);
    }
}
