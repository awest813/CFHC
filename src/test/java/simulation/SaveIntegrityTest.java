package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.DeflaterOutputStream;

import positions.Player;
import staff.HeadCoach;
import staff.Staff;

import static org.junit.Assert.*;

/**
 * Saves that must not come back wrong: names holding the save's own delimiters,
 * a pool coach who still holds a post, files cut off mid-write, a Windows replace
 * interrupted between its two renames, and a box-score list that inflates without
 * end. Each either reloads exactly (and plays on as if never saved) or is refused.
 */
public class SaveIntegrityTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider resources =
            new FileSystemResourceProvider(System.getProperty("user.dir"));

    private League newLeague(long seed) {
        SimRandom.pinNextSeed(seed);
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        Team user = league.getTeamList().get(0);
        user.setUserControlled(true);
        user.getHeadCoach().user = true;
        league.userTeam = user;
        return league;
    }

    private League load(File f) {
        League loaded = new League(f,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                GameUiBridge.NO_OP, true);
        loaded.setPlatformResourceProvider(resources);
        loaded.rebuildScheduleIfNeeded();
        loaded.refreshAfterLoad();
        return loaded;
    }

    private League load(byte[] save) throws IOException {
        File f = tmp.newFile();
        Files.write(f.toPath(), save);
        return load(f);
    }

    private static String text(League l) throws IOException {
        return new String(l.serializeSave(), StandardCharsets.UTF_8);
    }

    private static void play(League l, int weeks) {
        SeasonController c = new SeasonController(l, GameUiBridge.NO_OP);
        for (int i = 0; i < weeks; i++) {
            c.advanceWeek();
        }
    }

    /**
     * Saves {@code a} and plays it on; then reloads the save (the RNG is global, so
     * only after {@code a} is done), requires it to write the same save, plays it as
     * far and requires the two leagues to match.
     */
    private void assertReloadsAndContinuesIdentically(League a, int weeks) throws IOException {
        String saved = text(a);
        play(a, weeks);
        String unbroken = text(a);
        League b = load(saved.getBytes(StandardCharsets.UTF_8));
        assertEquals("reload writes the same save", saved, text(b));
        play(b, weeks);
        assertEquals("save -> load -> continue matches the unbroken run", unbroken, text(b));
    }

    // ------------------------------------------------------------------------------------------
    // Names with the save's delimiters
    // ------------------------------------------------------------------------------------------

    /** Every character that splits a field somewhere in a save, plus non-ASCII text. */
    private static final String NASTY = "& | : ; , % > ^ # = @ \\ _AMP_ %41 é 東京 🏈";
    private static final String NASTY_LINES = NASTY + " \t tab \n newline \r cr";

    @Test
    public void namesWithSaveDelimiters_roundTripExactly() throws Exception {
        League a = newLeague(0xD311L);
        play(a, 2);
        Team team = a.conferences.get(1).confTeams.get(0);
        String teamName = "Team " + NASTY;
        team.setName(teamName);
        Conference conf = a.conferences.get(1);
        String confName = "Conf " + NASTY;
        a.updateTeamConf(confName, conf.confName, 1);
        String coachName = "Coach " + NASTY_LINES;
        team.getHeadCoach().name = coachName;
        team.getOC().name = "OC " + NASTY_LINES;
        team.getHeadCoach().history.add("2030: " + NASTY_LINES);
        Player p = team.getAllPlayers().get(0);
        // Player names are whitespace-normalized by Player itself on load, so no line breaks here.
        String playerName = "Player " + NASTY;
        p.name = playerName;
        Team other = a.getTeamList().get(2);
        HeadCoach pooled = unemployedCopy(other.getHeadCoach());
        pooled.name = "Pool " + NASTY_LINES;
        a.addCoachFreeAgent(pooled);
        String news = "Head " + NASTY_LINES + ">Body " + NASTY_LINES;
        a.newsStories.get(a.currentWeek).add(news);

        League b = load(a.serializeSave());
        Team t2 = b.findTeam(teamName);
        assertNotNull("team found by its name", t2);
        assertEquals(team.getAllPlayers().size(), t2.getAllPlayers().size());
        assertEquals(playerName, t2.getAllPlayers().get(0).name);
        assertEquals(coachName, t2.getHeadCoach().name);
        assertEquals("OC " + NASTY_LINES, t2.getOC().name);
        assertEquals("2030: " + NASTY_LINES,
                t2.getHeadCoach().history.get(t2.getHeadCoach().history.size() - 1));
        assertEquals(confName, b.conferences.get(1).confName);
        assertEquals(confName, t2.getConference());
        assertTrue("news kept", b.newsStories.get(b.currentWeek).contains(news));
        boolean pooledFound = false;
        for (Staff s : b.getCoachFreeAgents()) {
            pooledFound |= s.name.equals("Pool " + NASTY_LINES);
        }
        assertTrue("pool coach kept", pooledFound);
        // The renamed team's games and opponents still link up.
        assertEquals(team.getGameSchedule().size(), t2.getGameSchedule().size());

        assertReloadsAndContinuesIdentically(a, 2);
    }

    @Test
    public void legacyRecords_withRawDelimiters_stillParse() {
        PlayerRecord p = PlayerRecord.fromCsv(Persistence.toCsv(playerRecord("Texas A&M", "Al Smith")));
        assertEquals("Texas A&M", p.teamName());
        // A v1.5c line wrote the team raw: "A: B: QB" must keep the position.
        String legacy = Persistence.toCsv(playerRecord("", "Al Smith"));
        PlayerRecord raw = PlayerRecord.fromCsv("Team A: B: " + legacy);
        assertEquals("Team A: B", raw.teamName());
        assertEquals("QB", raw.position());
        assertEquals("Al Smith", raw.name());

        // A coach named with '&' made the whole save unloadable.
        StaffRecord coach = staffRecord("Bob Ray");
        String csv = Persistence.toCsv(coach).replace("Bob Ray", "Bob & Ray");
        StaffRecord read = StaffRecord.fromCsv(csv);
        assertEquals("Bob & Ray", read.name());
        assertEquals(coach.age(), read.age());
        assertArrayEquals(coach.stats(), read.stats());
        assertEquals(coach.history(), read.history());

        // Legacy record holder with a comma.
        DataRecord dr = DataRecord.fromCsv("Pass Yards,4000,Al, Jr.%TAMU,2031");
        assertEquals("Al, Jr.%TAMU", dr.holder());
        assertEquals(2031, dr.year());
    }

    @Test
    public void legacyConferenceAndOocLines_withRawPipes_stillParse() throws Exception {
        League a = newLeague(0xC0F1L);
        String save = text(a);
        // Rewrite as a v1.5c file (no trailer) whose names carry a raw '|'.
        String confName = a.conferences.get(1).confName;
        String legacy = save.replace("V:" + League.CURRENT_SAVE_VERSION, "V:" + SaveSchema.V1_5C)
                .replaceAll("(?m)^END_SAVE\t\\d+\n", "")
                .replace("C:" + confName + "|", "C:" + "Big|Pipe" + "|")
                .replace("C:" + confName + "\n", "C:" + "Big|Pipe" + "\n");
        LeagueRecord r = SaveManager.load(new ByteArrayInputStream(legacy.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Big|Pipe", r.conferences().get(1).name());
        assertEquals(a.conferences.get(1).confTeams.size(), r.conferences().get(1).teams().size());
    }

    @Test
    public void nameValidation_rejectsSaveDelimiters() {
        for (String bad : new String[]{"A,B", "A>B", "A%B", "A\\B", "A|B", "A:B", "A;B", "A^B",
                "A\tB", "A\nB", "A\rB", "", "   "}) {
            assertFalse(bad, League.isSaveSafeName(bad));
            assertFalse(bad, League.isPersonNameValid(bad));
        }
        assertTrue(League.isSaveSafeName("Texas A&M"));
        assertFalse(League.isPersonNameValid("Bob & Ray"));
        assertTrue(League.isPersonNameValid("José Ñúñez O'Brien-Smith Jr."));
        assertTrue(League.isSaveSafeName("東京 Tech"));
    }

    // ------------------------------------------------------------------------------------------
    // A pool coach who still holds a post
    // ------------------------------------------------------------------------------------------

    @Test
    public void pooledCoachOnAStaff_staysOneObject_andLaterIdsHold() throws Exception {
        League a = newLeague(0xA11A5L);
        play(a, 3);
        Team o = a.getTeamList().get(5);
        HeadCoach hc = o.getHeadCoach();
        a.addCoach(hc); // the same object in the pool and on the staff
        HeadCoach detached = unemployedCopy(a.getTeamList().get(9).getHeadCoach());
        detached.name = "Detached Star";
        a.addCoachStar(detached);

        League b = load(a.serializeSave());
        Team o2 = b.getTeamList().get(5);
        assertEquals(1, b.getCoachList().size());
        assertSame("pool entry and post are one coach", o2.getHeadCoach(), b.getCoachList().get(0));
        assertEquals(1, b.getCoachStarList().size());
        assertEquals("ids after the pool still resolve", "Detached Star", b.getCoachStarList().get(0).name);
        assertNull(b.getCoachStarList().get(0).team);

        assertReloadsAndContinuesIdentically(a, 2);
    }

    /** A v1.5c save that wrote the aliased coach as a CFA: line too reloads with the ids back in place. */
    @Test
    public void legacySaveWithAliasedPoolCoach_isRepaired() throws Exception {
        League a = newLeague(0xA11A6L);
        play(a, 3);
        Team o = a.getTeamList().get(5);
        HeadCoach hc = o.getHeadCoach();
        a.addCoach(hc);
        HeadCoach detached = unemployedCopy(a.getTeamList().get(9).getHeadCoach());
        detached.name = "Detached Star";
        a.addCoachStar(detached);
        String save = text(a);
        // What v1.5c wrote: the post's coach also as a CFA: line, no trailer.
        String cfa = SaveManager.FREE_AGENT_COACH_PREFIX + Persistence.toCsv(hc.toRecord()) + "\n";
        int at = save.indexOf("\nC:") + 1;
        String legacy = (save.substring(0, at) + cfa + save.substring(at))
                .replace("V:" + League.CURRENT_SAVE_VERSION, "V:" + SaveSchema.V1_5C)
                .replaceAll("(?m)^END_SAVE\t\\d+\n", "");

        League b = load(legacy.getBytes(StandardCharsets.UTF_8));
        assertSame(b.getTeamList().get(5).getHeadCoach(), b.getCoachList().get(0));
        assertEquals("Detached Star", b.getCoachStarList().get(0).name);
        // And it re-saves as the current format writes the league.
        assertEquals(save, text(b));
    }

    // ------------------------------------------------------------------------------------------
    // Truncated files
    // ------------------------------------------------------------------------------------------

    @Test
    public void truncatedSave_isRefused_neverLoadsPartially() throws Exception {
        League a = newLeague(0x7C0FFL);
        play(a, 4);
        byte[] full = a.serializeSave();
        String fullText = new String(full, StandardCharsets.UTF_8);
        assertTrue(fullText.endsWith("\n"));
        assertTrue(fullText.substring(fullText.lastIndexOf('\n', fullText.length() - 2) + 1)
                .startsWith("END_SAVE\t"));
        Random rnd = new Random(42);
        List<Integer> cuts = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            cuts.add(rnd.nextInt(full.length));
            int c = rnd.nextInt(full.length);
            while (c < full.length && full[c] != '\n') c++;
            cuts.add(Math.min(full.length, c + 1)); // at a line end
        }
        cuts.add(full.length - 1); // only the final newline lost: still complete
        cuts.add(full.length - 2); // trailer count cut short
        int refused = 0;
        for (int cut : cuts) {
            byte[] part = Arrays.copyOf(full, cut);
            try {
                League b = load(part);
                assertEquals("a cut save loads only when complete (cut at " + cut + ")",
                        fullText, text(b));
            } catch (IllegalStateException e) {
                refused++;
                Throwable root = e;
                while (root.getCause() != null) root = root.getCause();
                assertTrue("clean refusal at " + cut + ": " + root,
                        root instanceof IOException || root instanceof RuntimeException);
            }
            assertThrowsOrComplete(part, cut == full.length || cut == full.length - 1);
        }
        assertTrue("most cuts are refused", refused >= cuts.size() - 2);
    }

    private static void assertThrowsOrComplete(byte[] part, boolean complete) {
        try {
            SaveManager.load(new ByteArrayInputStream(part));
            if (!complete) {
                // A cut inside the V:/L: header lines fails on them; past them the trailer decides.
                fail("incomplete save loaded");
            }
        } catch (IOException | RuntimeException expected) {
            assertFalse("complete save refused", complete);
        }
    }

    @Test
    public void currentSchemaWithoutTrailer_isRefused_olderSchemaStillLoads() throws Exception {
        League a = newLeague(0x7C0F1L);
        play(a, 2);
        String save = text(a);
        String noTrailer = save.replaceAll("(?m)^END_SAVE\t\\d+\n", "");
        try {
            SaveManager.load(new ByteArrayInputStream(noTrailer.getBytes(StandardCharsets.UTF_8)));
            fail("a current-schema save without its trailer must be refused");
        } catch (IOException e) {
            assertEquals(SaveManager.INCOMPLETE_SAVE, e.getMessage());
        }
        File f = tmp.newFile("slot.cfb");
        Files.write(f.toPath(), noTrailer.getBytes(StandardCharsets.UTF_8));
        assertTrue(SaveLoadMessages.isUnreadable(SaveFileSummary.summarize(f, League.CURRENT_SAVE_VERSION)));
        Files.write(f.toPath(), save.getBytes(StandardCharsets.UTF_8));
        assertTrue(SaveLoadMessages.isLoadable(SaveFileSummary.summarize(f, League.CURRENT_SAVE_VERSION)));

        String older = noTrailer.replace("V:" + League.CURRENT_SAVE_VERSION, "V:" + SaveSchema.V1_5C);
        League b = load(older.getBytes(StandardCharsets.UTF_8));
        assertEquals(SaveSchema.V1_5C, b.saveVer);
        assertEquals(save, text(b));
    }

    // ------------------------------------------------------------------------------------------
    // Interrupted Windows replace
    // ------------------------------------------------------------------------------------------

    @Test
    public void saveMovedAsideByAnInterruptedReplace_isRecovered() throws Exception {
        League a = newLeague(0xBA4L);
        byte[] save = a.serializeSave();
        File dir = tmp.newFolder("files");
        File main = new File(dir, "saveFile3.cfb");
        Files.write(new File(main.getAbsolutePath() + ".bak").toPath(), save);
        assertFalse(main.exists());
        String[] infos = LeagueSaveStorage.getSaveFileInfos(dir);
        assertTrue("slot listed again", SaveLoadMessages.isLoadable(infos[3]));
        assertTrue(main.isFile());
        assertFalse(new File(main.getAbsolutePath() + ".bak").exists());
        assertEquals(text(a), text(load(main)));

        // Direct loads recover too.
        File auto = new File(dir, LeagueSaveStorage.AUTOSAVE_FILE_NAME);
        Files.write(new File(auto.getAbsolutePath() + ".bak").toPath(), save);
        assertEquals(text(a), text(load(auto)));
    }

    // ------------------------------------------------------------------------------------------
    // Box-score bomb
    // ------------------------------------------------------------------------------------------

    @Test
    public void boxScoreListThatInflatesPastTheCap_isSkipped() throws Exception {
        byte[] small = deflate(new byte[4096]);
        assertEquals(4096, BoxScoreCodec.inflate(small, 4096).length);
        try {
            BoxScoreCodec.inflate(small, 4095);
            fail("inflating past the cap must stop");
        } catch (IOException expected) {
            // skipped by apply()
        }
        byte[] bomb = deflate(new byte[BoxScoreCodec.MAX_INFLATED_BYTES + (1 << 20)]);
        List<String> items = List.of("z1", BoxScoreCodec.base64(bomb));
        BoxScoreCodec.apply(items, Map.of()); // no OutOfMemoryError, nothing applied
    }

    private static byte[] deflate(byte[] in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream z = new DeflaterOutputStream(out)) {
            z.write(in);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------------------------------
    // Recruiting resume with a vacant head-coach post
    // ------------------------------------------------------------------------------------------

    @Test
    public void doneRecruiting_withVacantHeadCoach_doesNotCrash() throws Exception {
        League a = newLeague(0x5EC0L);
        play(a, 2);
        a.userTeam.setHeadCoach(null);
        File dir = tmp.newFolder("rec");
        Files.write(LeagueSaveStorage.getRecruitingSaveFile(dir).toPath(), a.serializeSave());
        LeagueLaunchCoordinator.LaunchResult result = LeagueLaunchCoordinator.load(
                LeagueLaunchCoordinator.LaunchRequest.doneRecruiting(""),
                dir, GameUiBridge.NO_OP, League.DEFAULT_NEW_SAVE_YEAR,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                null, null);
        assertNotNull(result.userTeam);
        assertEquals(a.userTeam.getName(), result.userTeam.getName());
    }

    // ------------------------------------------------------------------------------------------

    private static PlayerRecord playerRecord(String team, String name) {
        return new PlayerRecord("QB", name, team, 2, 10, 50, 50, 3, false, false, 60, 70, 65, 5,
                new int[]{60, 61, 62, 63}, 74, 210, false, 0, 0, 0.5, new int[]{1, 2, 3},
                new ArrayList<>(List.of(new int[]{4, 5, 6})), new int[]{0, 1}, false, false, false, false,
                false, 0, "", "Gunslinger", "Mentor Name");
    }

    /** A copy with its own career arrays (a record-built coach would share them), on no team. */
    private static HeadCoach unemployedCopy(HeadCoach src) {
        HeadCoach c = new HeadCoach(src, src.team);
        c.team = null;
        return c;
    }

    private static StaffRecord staffRecord(String name) {
        return new StaffRecord("HC", name, 50, 5, 60, 61, 62, 63, 1, 2, 3, 6, 70, false, 65, 2, false,
                0, 0, new int[]{10, 3}, new int[]{1, 0}, new ArrayList<>(List.of("2030: Texas A&M 10-3")),
                "");
    }
}
