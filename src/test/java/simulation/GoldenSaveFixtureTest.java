package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

/**
 * Golden save compatibility for the current {@link League#CURRENT_SAVE_VERSION}
 * / {@code SaveManager} {@code L:} format.
 *
 * <p>Fixture path (repo-relative):
 * {@code src/test/resources/fixtures/saves/v1.4e-fresh-league.cfb.gz}.
 * Regenerate with:
 * {@code ./gradlew -p desktop-standalone :engine:test --tests simulation.GoldenSaveFixtureTest -DregenGoldenSaves=true}
 */
public class GoldenSaveFixtureTest {

    private static final String FIXTURE_REL =
            "src/test/resources/fixtures/saves/v1.4e-fresh-league.cfb.gz";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private DesktopResourceProvider resources;
    private File fixtureGz;
    private File fixtureCfB;

    @Before
    public void setUp() throws Exception {
        String projectRoot = System.getProperty("user.dir");
        resources = new DesktopResourceProvider(projectRoot);
        fixtureGz = new File(projectRoot, FIXTURE_REL);

        if (Boolean.getBoolean("regenGoldenSaves") || !fixtureGz.isFile()) {
            regenerateFixture(fixtureGz);
        }

        assertTrue(
                "Missing golden save fixture at " + fixtureGz.getAbsolutePath()
                        + ". Regenerate with -DregenGoldenSaves=true",
                fixtureGz.isFile());

        fixtureCfB = tmp.newFile("v1.4e-fresh-league.cfb");
        gunzip(fixtureGz, fixtureCfB);
    }

    @Test
    public void goldenSave_loadsWithoutError() throws Exception {
        League loaded = loadFixture();
        assertNotNull(loaded);
        assertFalse("Fixture league must have teams", loaded.getTeamList().isEmpty());
        assertEquals(League.CURRENT_SAVE_VERSION, loaded.saveVer);
        assertTrue("Season year should be positive", loaded.getYear() > 0);
        assertTrue("Header should use SaveManager L: format",
                Files.readString(fixtureCfB.toPath()).startsWith("L:"));
    }

    @Test
    public void goldenSave_preservesTeamCountOnRoundTrip() throws Exception {
        League loaded = loadFixture();
        int teams = loaded.getTeamList().size();
        File roundTrip = tmp.newFile("roundtrip.cfb");
        assertTrue(loaded.saveLeague(roundTrip));

        League again = new League(
                roundTrip,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        again.setPlatformResourceProvider(resources);
        assertEquals(teams, again.getTeamList().size());
        assertEquals(loaded.getYear(), again.getYear());
    }

    @Test
    public void goldenSave_canAdvanceOneWeek() throws Exception {
        League loaded = loadFixture();
        if (loaded.userTeam == null) {
            loaded.userTeam = loaded.getTeamList().get(0);
            loaded.userTeam.setUserControlled(true);
        }
        SeasonController controller = new SeasonController(loaded, noOpBridge());
        int weekBefore = loaded.currentWeek;
        controller.advanceWeek();
        assertTrue("Advancing a week should progress or complete a phase transition",
                loaded.currentWeek >= weekBefore);
    }

    private League loadFixture() {
        League loaded = new League(
                fixtureCfB,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        loaded.setPlatformResourceProvider(resources);
        return loaded;
    }

    private void regenerateFixture(File targetGz) throws Exception {
        File parent = targetGz.getParentFile();
        assertTrue("Could not create fixture dir " + parent, parent.mkdirs() || parent.isDirectory());

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
        league.leagueName = "Golden Fixture League";
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setupUserCoach("Golden Fixture Coach");
        league.userTeam.getHeadCoach().user = true;
        league.userTeam.setUserControlled(true);

        File staging = tmp.newFile("staging-golden.cfb");
        assertTrue("Failed to write staging golden save", league.saveLeague(staging));
        gzip(staging, targetGz);
    }

    private static void gunzip(File gz, File out) throws Exception {
        try (InputStream in = new GZIPInputStream(new FileInputStream(gz));
             OutputStream os = new FileOutputStream(out)) {
            in.transferTo(os);
        }
    }

    private static void gzip(File in, File gz) throws Exception {
        try (InputStream is = new FileInputStream(in);
             OutputStream out = new GZIPOutputStream(new FileOutputStream(gz))) {
            is.transferTo(out);
        }
    }

    private static GameUiBridge noOpBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {}
            @Override public void updateSimStatus(String statusText, String buttonText, boolean isMajorEvent) {}
            @Override public void showNotification(String title, String message) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String summaryText) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        };
    }
}
