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

import positions.PlayerQB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Season baselines (projected wins/poll rank, starting prestige and talent, league
 * talent averages) are what the season-end prestige update and staff evaluation
 * grade against. They used to be set only by Android's goals dialog and were not
 * saved, so desktop careers (and any reload) graded every team as "projected #0"
 * and divided coach evaluation by a zero league average.
 */
public class SeasonBaselineTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private FileSystemResourceProvider resources;
    private League league;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setupUserCoach("Baseline Coach");
        league.userTeam.getHeadCoach().user = true;
        league.userTeam.setUserControlled(true);
    }

    @Test
    public void newLeague_hasProjectionsAndLeagueAverages() {
        assertProjectionsArePermutation(league);
        assertTrue("league offensive talent average must be set", league.leagueOffTal > 0);
        assertTrue("league defensive talent average must be set", league.leagueDefTal > 0);
        for (Team t : league.getTeamList()) {
            assertEquals(t.getName() + " starting prestige", t.teamPrestige, t.teamPrestigeStart);
            assertTrue(t.getName() + " starting offensive talent", t.teamStartOffTal > 0);
        }
    }

    @Test
    public void saveAndLoad_keepsBaselines() throws Exception {
        League loaded = load(save(league));

        for (Team original : league.getTeamList()) {
            Team t = find(loaded, original.getName());
            assertEquals(original.getName() + " projected rank", original.projectedPollRank, t.projectedPollRank);
            assertEquals(original.getName() + " projected wins", original.projectedWins, t.projectedWins);
            assertEquals(original.getName() + " prestige start", original.teamPrestigeStart, t.teamPrestigeStart);
            assertEquals(original.getName() + " start off talent", original.teamStartOffTal, t.teamStartOffTal, 0.001f);
        }
        assertEquals(league.leagueOffTal, loaded.leagueOffTal);
        assertEquals(league.leagueDefTal, loaded.leagueDefTal);
    }

    @Test
    public void loadingSaveWithoutBaselines_recomputesThemWithoutReorderingDepthCharts() throws Exception {
        Team user = league.userTeam;
        assertTrue("need two QBs to swap", user.getTeamQBs().size() >= 2);
        assertTrue(user.swapDepthChartOrder("QB", 0, 1));
        List<String> depth = qbNames(user);

        File save = save(league);
        // Strip the six trailing baseline fields from every T: line: a pre-baseline save.
        List<String> lines = Files.readAllLines(save.toPath(), StandardCharsets.UTF_8);
        List<String> legacy = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("T:")) {
                String[] p = line.split("\t", -1);
                assertEquals("current T: lines carry 23 fields", 23, p.length);
                line = String.join("\t", java.util.Arrays.copyOf(p, 17));
            }
            legacy.add(line);
        }
        Files.write(save.toPath(), legacy, StandardCharsets.UTF_8);

        League loaded = load(save);

        assertProjectionsArePermutation(loaded);
        assertTrue(loaded.leagueOffTal > 0);
        assertEquals("depth chart must survive the load-time repair", depth, qbNames(find(loaded, user.getName())));
    }

    @Test
    public void staffTalentShare_isZeroWhenLeagueAverageIsUnknown() {
        assertEquals(0.0, Team.talentShare(12.5, 0), 0.0);
        assertEquals(0.5, Team.talentShare(10, 20), 1e-9);
    }

    @Test
    public void addPlayer_makesTheReceivingTeamTheOwner() {
        Team from = league.getTeamList().get(1);
        Team to = league.getTeamList().get(2);
        PlayerQB qb = from.getTeamQBs().get(0);
        assertNotEquals(from, to);

        to.addPlayerQB(qb);

        assertSame("a player listed on a roster must point at that team", to, qb.team);
    }

    private static void assertProjectionsArePermutation(League l) {
        int n = l.getTeamList().size();
        boolean[] seen = new boolean[n + 1];
        for (Team t : l.getTeamList()) {
            int r = t.projectedPollRank;
            assertTrue(t.getName() + " projected rank " + r + " out of 1.." + n, r >= 1 && r <= n);
            assertTrue("duplicate projected rank " + r, !seen[r]);
            seen[r] = true;
        }
    }

    private static List<String> qbNames(Team t) {
        List<String> names = new ArrayList<>();
        for (PlayerQB qb : t.getTeamQBs()) {
            names.add(qb.name);
        }
        return names;
    }

    private File save(League l) throws Exception {
        File file = tmp.newFile();
        assertTrue(l.saveLeague(file));
        return file;
    }

    private League load(File file) throws Exception {
        League loaded = new League(file,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);
        loaded.setPlatformResourceProvider(resources);
        return loaded;
    }

    private static Team find(League l, String name) {
        for (Team t : l.getTeamList()) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        throw new AssertionError("team not found: " + name);
    }
}
