package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The FCS promotion setting (none / capped / unlimited) and the league settings
 * that now travel with a save.
 */
public class FcsPromotionTest {

    private static final long SEED = 20260929L;
    /** Offseasons to simulate; each rolls the ~6% promotion and ~2% new-conference events. */
    private static final int OFFSEASONS = 250;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void unlimited_promotesPastWhatTheCappedTestAllows() {
        League league = realignmentLeague(FcsPromotionMode.UNLIMITED, 0);
        int before = league.getTeamList().size();

        runOffseasons(league);

        int added = league.getTeamList().size() - before;
        assertTrue("the seeded run must promote more than the cap below, got " + added, added > 2);
        assertEquals(added, league.fcsPromotionsUsed);
    }

    @Test
    public void none_neverAddsTeams() {
        League league = realignmentLeague(FcsPromotionMode.NONE, 0);
        int before = league.getTeamList().size();

        runOffseasons(league);

        assertEquals(before, league.getTeamList().size());
        assertEquals(0, league.fcsPromotionsUsed);
        for (Conference c : league.conferences) {
            assertFalse("no FCS-built conference without promotions", "Antdroid".equals(c.confName));
        }
    }

    @Test
    public void capped_stopsAtTheCap() {
        League league = realignmentLeague(FcsPromotionMode.CAPPED, 2);
        int before = league.getTeamList().size();

        runOffseasons(league);

        int added = league.getTeamList().size() - before;
        assertEquals("the seeded run reaches the cap exactly", 2, added);
        assertEquals(2, league.fcsPromotionsUsed);
        assertEquals(0, league.remainingFcsPromotions());
    }

    @Test
    public void remainingFcsPromotions_followsTheMode() {
        League league = newLeague();
        league.fcsPromotionCap = 5;
        league.fcsPromotionsUsed = 3;

        league.fcsPromotionMode = FcsPromotionMode.NONE;
        assertEquals(0, league.remainingFcsPromotions());
        league.fcsPromotionMode = FcsPromotionMode.CAPPED;
        assertEquals(2, league.remainingFcsPromotions());
        league.fcsPromotionsUsed = 7;
        assertEquals("never negative once past the cap", 0, league.remainingFcsPromotions());
        league.fcsPromotionMode = FcsPromotionMode.UNLIMITED;
        assertEquals(Integer.MAX_VALUE, league.remainingFcsPromotions());
    }

    @Test
    public void newLeague_defaultsToUnlimited() {
        League league = newLeague();
        assertEquals(FcsPromotionMode.UNLIMITED, league.fcsPromotionMode);
        assertEquals(League.DEFAULT_FCS_PROMOTION_CAP, league.fcsPromotionCap);
        assertEquals(0, league.fcsPromotionsUsed);
    }

    @Test
    public void capLimit_matchesTheFcsNamePool() {
        assertEquals(newLeague().teamsFCS.length, LeagueSettingsOptions.MAX_FCS_PROMOTION_CAP);
    }

    @Test
    public void settingsOptions_copyAndClampTheFcsFields() {
        League league = newLeague();
        LeagueSettingsOptions options = LeagueSettingsOptions.fromLeague(league);
        options.fcsPromotionMode = FcsPromotionMode.CAPPED;
        options.fcsPromotionCap = 500;

        options.applyTo(league, true, false, false);

        assertEquals(FcsPromotionMode.CAPPED, league.fcsPromotionMode);
        assertEquals(LeagueSettingsOptions.MAX_FCS_PROMOTION_CAP, league.fcsPromotionCap);
        assertEquals(FcsPromotionMode.CAPPED, LeagueSettingsOptions.fromLeague(league).fcsPromotionMode);
    }

    @Test
    public void saveAndLoad_keepsEveryLeagueSetting() throws Exception {
        League league = newLeague();
        league.careerMode = false;
        league.showPotential = true;
        league.fullGameLog = true;
        league.neverRetire = true;
        league.enableTV = false;
        league.expPlayoffs = false;
        league.confRealignment = true;
        league.advancedRealignment = true;
        league.enableUnivProRel = false;
        league.fcsPromotionMode = FcsPromotionMode.CAPPED;
        league.fcsPromotionCap = 3;
        league.fcsPromotionsUsed = 1;

        League loaded = load(save(league));

        assertFalse(loaded.careerMode);
        assertTrue(loaded.showPotential);
        assertTrue(loaded.fullGameLog);
        assertTrue(loaded.neverRetire);
        assertFalse(loaded.enableTV);
        assertFalse(loaded.expPlayoffs);
        assertTrue(loaded.confRealignment);
        assertTrue(loaded.advancedRealignment);
        assertFalse(loaded.enableUnivProRel);
        assertEquals(FcsPromotionMode.CAPPED, loaded.fcsPromotionMode);
        assertEquals(3, loaded.fcsPromotionCap);
        assertEquals(1, loaded.fcsPromotionsUsed);
    }

    @Test
    public void saveSummary_readsModeAndPlayoffsFromTheSettingsLine() throws Exception {
        League league = newLeague();
        league.careerMode = false;
        league.expPlayoffs = false;
        String summary = SaveFileSummary.summarize(save(league), League.CURRENT_SAVE_VERSION);
        assertTrue(summary, summary.contains("Open Dynasty"));
        assertTrue(summary, summary.contains("4-Team Playoff"));

        league.careerMode = true;
        league.expPlayoffs = true;
        summary = SaveFileSummary.summarize(save(league), League.CURRENT_SAVE_VERSION);
        assertTrue(summary, summary.contains("Head Coach Career"));
        assertTrue(summary, summary.contains("12-Team Playoff"));
    }

    @Test
    public void loadingSaveWithoutSettings_usesNewLeagueDefaults() throws Exception {
        League league = newLeague();
        // A school that realignment promoted in an earlier season.
        Team promoted = new Team("Montana", "FCS", "Independent", 35, "A", 0, league, true);
        league.teamList.add(promoted);
        league.conferences.get(league.getConfNumber("Independent")).confTeams.add(promoted);
        league.careerMode = false;
        league.enableTV = false;

        League loaded = load(withoutSettingsLine(save(league)));

        LeagueRecord.Settings d = LeagueRecord.Settings.defaults();
        assertEquals(d.careerMode(), loaded.careerMode);
        assertEquals(d.enableTv(), loaded.enableTV);
        assertEquals(d.expandedPlayoffs(), loaded.expPlayoffs);
        assertEquals(d.confRealignment(), loaded.confRealignment);
        assertFalse(loaded.enableUnivProRel);
        assertEquals(FcsPromotionMode.UNLIMITED, loaded.fcsPromotionMode);
        assertEquals("past promotions are counted from the league", 1, loaded.fcsPromotionsUsed);
    }

    @Test
    public void loadingProRelSaveWithoutSettings_staysInProRel() throws Exception {
        League league = newLeague();
        league.enableUnivProRel = true;
        league.confRealignment = false;
        league.convertUnivProRel();

        League loaded = load(withoutSettingsLine(save(league)));

        assertTrue(loaded.enableUnivProRel);
        assertFalse(loaded.confRealignment);
        assertFalse(loaded.advancedRealignment);
        assertEquals("pro/rel filler is not a promotion", 0, loaded.fcsPromotionsUsed);
    }

    @Test
    public void loadedLeague_canPromoteBeforeItsFirstSchedule() throws Exception {
        // New-format loads skip scheduling, which is what used to build the FCS name
        // pool; with advanced realignment now persisted, promotion must not NPE.
        League league = newLeague();
        league.advancedRealignment = true;
        League loaded = load(save(league));
        loaded.teamsFCSList = null;
        assertTrue(loaded.advancedRealignment);
        int before = loaded.getTeamList().size();

        SimRandom.bind(SEED);
        // ~6% promotion roll per offseason: 150 leave a no-promotion run at ~0.01%.
        for (int i = 0; i < 150; i++) {
            loaded.conferenceRealignmentV2(GameUiBridge.NO_OP);
        }

        assertTrue("promotion ran on the loaded league", loaded.getTeamList().size() > before);
    }

    @Test
    public void settingsLine_ignoresUnknownAndBadValues() {
        LeagueRecord.Settings s = LeagueRecord.Settings.fromSaveLine(
                "careerMode=0\tfutureOption=7\tfcsPromotion=SOMETIMES\tfcsPromotionCap=lots\tnoEquals");
        LeagueRecord.Settings d = LeagueRecord.Settings.defaults();

        assertFalse(s.careerMode());
        assertEquals(d.enableTv(), s.enableTv());
        assertEquals(d.fcsPromotionMode(), s.fcsPromotionMode());
        assertEquals(d.fcsPromotionCap(), s.fcsPromotionCap());
        assertEquals(s, LeagueRecord.Settings.fromSaveLine(s.toSaveLine()));
    }

    private League newLeague() {
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setupUserCoach("FCS Test Coach");
        league.userTeam.getHeadCoach().user = true;
        league.userTeam.setUserControlled(true);
        return league;
    }

    private League realignmentLeague(FcsPromotionMode mode, int cap) {
        League league = newLeague();
        league.confRealignment = true;
        league.advancedRealignment = true;
        league.fcsPromotionMode = mode;
        league.fcsPromotionCap = cap;
        return league;
    }

    private static void runOffseasons(League league) {
        SimRandom.bind(SEED);
        for (int i = 0; i < OFFSEASONS; i++) {
            league.conferenceRealignmentV2(GameUiBridge.NO_OP);
        }
    }

    private File save(League league) throws Exception {
        File file = tmp.newFile();
        assertTrue(league.saveLeague(file));
        return file;
    }

    private static File withoutSettingsLine(File save) throws Exception {
        List<String> kept = new ArrayList<>();
        boolean sawSettings = false;
        for (String line : Files.readAllLines(save.toPath(), StandardCharsets.UTF_8)) {
            if (line.startsWith(SaveManager.SETTINGS_PREFIX)) {
                sawSettings = true;
                continue;
            }
            kept.add(line);
        }
        assertTrue("current saves carry a settings line", sawSettings);
        Files.write(save.toPath(), kept, StandardCharsets.UTF_8);
        return save;
    }

    private League load(File file) {
        League loaded = new League(file,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        loaded.setPlatformResourceProvider(resources);
        return loaded;
    }
}
