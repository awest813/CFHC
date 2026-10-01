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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** A team's scheme: the book that plays, what the save keeps, how CPU staffs pick. */
public class PlaybookStateTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    private League league;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(424242L);
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setupUserCoach("Scheme Coach");
        league.userTeam.setUserControlled(true);
    }

    @Test
    public void theBookThatPlaysFollowsTheSchemeNumber() {
        Team t = league.getTeamList().get(3);

        // Game-day counters and coaching decisions set only the number; the sim reads the book.
        t.setPlaybookOffNum(3);
        t.setPlaybookDefNum(4);
        assertEquals("Air Raid", t.getPlaybookOffense().getStratName());
        assertEquals("Cover 3 Match", t.getPlaybookDefense().getStratName());

        // Android writes the public fields directly.
        t.playbookOffNum = 1;
        t.playbookDefNum = 2;
        assertEquals("Power Spread", t.getPlaybookOffense().getStratName());
        assertEquals("Zero Pressure", t.getPlaybookDefense().getStratName());

        t.setPlaybookOffense(PlaybookOffense.forIndex(5));
        assertEquals(5, t.getPlaybookOffNum());

        t.setPlaybookOffNum(9);
        assertEquals(0, t.getPlaybookOffNum());
        assertEquals("Multiple Pro", t.getPlaybookOffense().getStratName());
    }

    @Test
    public void aBookNumberOutOfRangeIsTheDefaultNotARandomBook() {
        for (int i = 0; i < 20; i++) {
            assertEquals("Multiple Pro", new PlaybookOffense(0).getStratName());
            assertEquals("Multiple 4-2-5", new PlaybookDefense(0).getStratName());
        }
        assertEquals(0, new PlaybookOffense(7).getIndex());
        assertEquals(4, PlaybookDefense.forIndex(4).getIndex());
        assertEquals("Cover 3 Match", PlaybookDefense.forIndex(4).getStratName());
    }

    @Test
    public void saveKeepsEveryTeamsSchemes() throws Exception {
        Team user = league.userTeam;
        Team cpu = league.getTeamList().get(5);
        user.setPlaybookOffNum(3);
        user.setPlaybookDefNum(4);
        int cpuOff = (cpu.getPlaybookOffNum() + 2) % Team.OFFENSE_PLAYBOOKS;
        cpu.setPlaybookOffNum(cpuOff);
        cpu.setPlaybookDefNum(1);

        League loaded = load(save(league));

        Team u = find(loaded, user.getName());
        assertEquals(3, u.getPlaybookOffNum());
        assertEquals(4, u.getPlaybookDefNum());
        assertEquals("Air Raid", u.getPlaybookOffense().getStratName());
        assertEquals("Cover 3 Match", u.getPlaybookDefense().getStratName());
        Team c = find(loaded, cpu.getName());
        assertEquals(cpuOff, c.getPlaybookOffNum());
        assertEquals(1, c.getPlaybookDefNum());
        for (Team t : loaded.getTeamList()) {
            assertEquals(t.getName(), t.getPlaybookOffNum(), t.getPlaybookOffense().getIndex());
            assertEquals(t.getName(), t.getPlaybookDefNum(), t.getPlaybookDefense().getIndex());
        }
    }

    @Test
    public void anOlderSaveGivesTeamsTheirCoordinatorsBooks() throws Exception {
        league.userTeam.setPlaybookOffNum(3);
        File save = save(league);
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(save.toPath(), StandardCharsets.UTF_8)) {
            if (line.startsWith("T:")) {
                String[] p = line.split("\t", -1);
                assertEquals("scheme fields follow the baseline", 25, p.length);
                line = String.join("\t", java.util.Arrays.copyOf(p, 23));
            }
            lines.add(line);
        }
        Files.write(save.toPath(), lines, StandardCharsets.UTF_8);

        League loaded = load(save);
        for (Team t : loaded.getTeamList()) {
            assertEquals(t.getName(), t.getCPUOffense(), t.getPlaybookOffNum());
            assertEquals(t.getName(), t.getCPUDefense(), t.getPlaybookDefNum());
            assertEquals(t.getPlaybookOffNum(), t.getPlaybookOffense().getIndex());
        }
    }

    @Test
    public void schemeFieldsKeepTheirPlaceWithoutABaseline() {
        LeagueRecord.TeamRecord r = league.getTeamList().get(2).toRecord();
        LeagueRecord.TeamRecord noBaseline = new LeagueRecord.TeamRecord(r.name(), r.abbr(), r.prestige(), r.wins(), r.losses(),
                r.oocWeeks(), r.oocOpponentNames(), r.teamPollScore(), r.rankTeamPollScore(), r.headCoach(), r.offenseCoach(),
                r.defenseCoach(), r.roster(), r.history(), r.records(), r.practiceFocus(), r.practicePositionGroup(),
                r.focusIntensity(), r.nilCollectiveLevel(), r.nickname(), r.prevRankTeamPollScore(), r.rivalName(),
                r.rivalryTrophyName(), r.rivalryWins(), r.holdsRivalryTrophy(), r.teamStadium(), null, 2, 3);
        String fields = SaveManager.playbookFields(noBaseline);
        String[] p = ("x" + fields).split("\t", -1);
        assertEquals(9, p.length); // leading token, 6 empty baseline slots, 2 schemes
        assertEquals("2", p[7]);
        assertEquals("3", p[8]);
    }

    @Test
    public void cpuStaffsCallTheirCurrentCoordinatorsBooksEachSeason() {
        Team cpu = league.getTeamList().get(7);
        cpu.getOC().offStrat = 2;   // Quick Game: no running QB needed
        cpu.getDC().defStrat = 3;   // Tampa 2
        Team user = league.userTeam;
        user.setPlaybookOffNum(3);
        user.getOC().offStrat = 1;

        league.startNextSeason();

        assertEquals(2, cpu.getPlaybookOffNum());
        assertEquals(3, cpu.getPlaybookDefNum());
        assertEquals("Quick Game", cpu.getPlaybookOffense().getStratName());
        assertEquals("the user's pick stands", 3, user.getPlaybookOffNum());
    }

    @Test
    public void schemeFitMatchesTheStaffPenaltyTiers() {
        Team t = league.getTeamList().get(4);
        t.getOC().offStrat = 1;
        t.getHeadCoach().offStrat = 2;
        t.getDC().defStrat = 3;
        t.getHeadCoach().defStrat = 3;
        t.getQB(0).ratAttr4 = 90;

        assertEquals(Team.SchemeFit.COORDINATOR, t.schemeFit(true, 1));
        assertEquals(Team.SchemeFit.HEAD_COACH_ONLY, t.schemeFit(true, 2));
        assertEquals(Team.SchemeFit.NEITHER, t.schemeFit(true, 3));
        assertEquals(Team.SchemeFit.COORDINATOR, t.schemeFit(false, 3));
        assertTrue(t.schemeFitNote(false, 3).contains("DC's scheme and your head coach's"));
        assertTrue(t.schemeFitNote(true, 3).contains("Your OC runs Power Spread"));

        t.getQB(0).ratAttr4 = 60;
        assertTrue(t.schemeFitNote(true, 4).contains("starter's speed is 60"));
        assertTrue(!t.schemeFitNote(true, 3).contains("speed"));

        t.setOC(null);
        assertEquals(Team.SchemeFit.NO_COORDINATOR, t.schemeFit(true, 1));
    }

    @Test
    public void readOptionBooksNeedARunningQuarterback() {
        Team cpu = league.getTeamList().get(9);
        assertNotNull(cpu.getQB(0));
        cpu.getOC().offStrat = 4;
        cpu.getQB(0).ratAttr4 = Team.READ_OPTION_QB_SPEED - 1;
        assertEquals("a slow QB runs the balanced book", 0, cpu.getCPUOffense());
        cpu.getQB(0).ratAttr4 = Team.READ_OPTION_QB_SPEED + 5;
        assertEquals(4, cpu.getCPUOffense());
        assertTrue(PlaybookOffense.forIndex(4).featuresQbRuns());
        assertTrue(PlaybookOffense.forIndex(5).featuresQbRuns());
        for (int i = 0; i < 4; i++) {
            assertTrue(!PlaybookOffense.forIndex(i).featuresQbRuns());
        }
    }

    private File save(League l) throws Exception {
        File file = tmp.newFile();
        assertTrue(l.saveLeague(file));
        return file;
    }

    private League load(File file) {
        League loaded = new League(file,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        loaded.setPlatformResourceProvider(resources);
        return loaded;
    }

    private static Team find(League l, String name) {
        for (Team t : l.getTeamList()) {
            if (t.getName().equals(name)) return t;
        }
        throw new AssertionError("no team " + name);
    }
}
