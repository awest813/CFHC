package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * The box score of every game already played (play-by-play log, team totals,
 * quarter scores, returners and every player's stat line) must read the same after
 * save and load as before it, for the user's games and the CPU's. Saves before
 * v1.5c dropped all of it, so a reloaded season showed empty game summaries.
 */
public class SaveBoxScoreRoundTripTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider resources =
            new FileSystemResourceProvider(System.getProperty("user.dir"));

    private League newLeague() {
        SimRandom.pinNextSeed(0xB0C5L);
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

    private League load(byte[] save) throws Exception {
        File f = tmp.newFile();
        Files.write(f.toPath(), save);
        League loaded = new League(f,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                GameUiBridge.NO_OP, true);
        loaded.setPlatformResourceProvider(resources);
        loaded.rebuildScheduleIfNeeded();
        loaded.refreshAfterLoad();
        return loaded;
    }

    /** Everything a box-score screen (Android game dialog, desktop box-score view) reads. */
    static String boxScore(Game g) {
        StringBuilder sb = new StringBuilder();
        sb.append(g.gameName).append(' ').append(g.awayTeam.getName()).append('@').append(g.homeTeam.getName())
                .append(' ').append(g.awayScore).append('-').append(g.homeScore).append(" OT").append(g.numOT)
                .append(" q=").append(Arrays.toString(g.awayQScore)).append(Arrays.toString(g.homeQScore))
                .append(" weather=").append(g.weather).append(" td=").append(g.tdInfo)
                .append(" timeouts=").append(g.homeTimeouts).append(',').append(g.awayTimeouts).append('\n');
        sb.append("LOG ").append(g.getPlayByPlayLog()).append('\n');
        for (List<String> lines : Arrays.asList(g.homePassingStats, g.awayPassingStats, g.homeRushingStats,
                g.awayRushingStats, g.homeReceivingStats, g.awayReceivingStats, g.homeKickingStats,
                g.awayKickingStats, g.homeDefenseStats, g.awayDefenseStats)) {
            sb.append("LINES ").append(lines).append('\n');
        }
        sb.append("SUMMARY ").append(String.join("|", g.getGameSummaryStr())).append('\n');
        sb.append("SUMMARY2 ").append(String.join("|", g.getGameSummaryStrV2())).append('\n');
        return sb.toString();
    }

    /** Box scores of every played game, by team and schedule slot. */
    private static List<String> playedBoxScores(League l, boolean userGames) {
        List<String> out = new ArrayList<>();
        for (Team t : l.getTeamList()) {
            if (t.isUserControlled() != userGames) {
                continue;
            }
            List<Game> schedule = t.getGameSchedule();
            for (int i = 0; i < schedule.size(); i++) {
                Game g = schedule.get(i);
                if (g != null && g.hasPlayed && !g.isByeWeek()) {
                    out.add(t.getName() + "#" + i + " " + boxScore(g));
                }
            }
        }
        return out;
    }

    private static void assertBoxScoresSurvive(League before, League after, int week) {
        for (boolean user : new boolean[]{true, false}) {
            List<String> a = playedBoxScores(before, user);
            List<String> b = playedBoxScores(after, user);
            String who = (user ? "user" : "CPU") + " games at week " + week;
            assertFalse("some " + who + " were played", a.isEmpty());
            assertEquals(who + ": count", a.size(), b.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals(who + ": " + a.get(i).substring(0, a.get(i).indexOf('\n')), a.get(i), b.get(i));
            }
            for (String s : a) {
                assertTrue("a played game has a play-by-play log: " + s.substring(0, s.indexOf('\n')),
                        !s.contains("No play-by-play data available"));
                assertTrue("a played game has passing lines", s.contains("LINES [") && !s.contains("LINES null"));
            }
        }
    }

    @Test
    public void boxScores_identicalAfterSaveAndLoad_weeks5_13_16() throws Exception {
        League league = newLeague();
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        for (int week : new int[]{5, 13, 16}) {
            while (league.currentWeek < week) {
                controller.advanceWeek();
            }
            String rng = SimRandom.exportState();
            byte[] save = league.serializeSave();
            League loaded = load(save);
            assertBoxScoresSurvive(league, loaded, week);
            // The reloaded league saves the same box scores again.
            assertArrayEquals("save -> load -> save at week " + week, save, loaded.serializeSave());
            SimRandom.restoreState(rng);
        }
    }

    @Test
    public void boxScores_surviveWithFullGameLog() throws Exception {
        League league = newLeague();
        league.fullGameLog = true;
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        while (league.currentWeek < 3) {
            controller.advanceWeek();
        }
        League loaded = load(league.serializeSave());
        assertBoxScoresSurvive(league, loaded, 3);
    }

    /** A v1.5b save (no box-score list) still loads; its past games just have no box score. */
    @Test
    public void v15bSave_withoutBoxScores_stillLoadsAndPlays() throws Exception {
        League league = newLeague();
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        while (league.currentWeek < 4) {
            controller.advanceWeek();
        }
        StringBuilder older = new StringBuilder();
        for (String line : new String(league.serializeSave(), StandardCharsets.UTF_8).split("\n", -1)) {
            if (line.startsWith(SaveSchema.VERSION_PREFIX)) {
                older.append(SaveSchema.VERSION_PREFIX).append(SaveSchema.V1_5B);
            } else if (line.startsWith(SaveManager.LEAGUE_LIST_PREFIX + BoxScoreCodec.KEY + "\t")) {
                continue;
            } else {
                older.append(line);
            }
            older.append('\n');
        }
        League loaded = load(older.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(SaveSchema.V1_5B, loaded.saveVer);
        Game played = loaded.userTeam.getGameSchedule().get(0);
        assertTrue(played.hasPlayed);
        assertEquals("No play-by-play data available for this game.", played.getPlayByPlayLog());
        SeasonController c = new SeasonController(loaded, GameUiBridge.NO_OP);
        while (loaded.currentWeek < 6) {
            c.advanceWeek();
        }
        assertTrue(loaded.userTeam.getGameSchedule().get(4).hasPlayed);
    }

    /** A damaged box-score list is skipped; the rest of the save loads. */
    @Test
    public void damagedBoxScores_areSkipped() throws Exception {
        League league = newLeague();
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        while (league.currentWeek < 2) {
            controller.advanceWeek();
        }
        StringBuilder damaged = new StringBuilder();
        for (String line : new String(league.serializeSave(), StandardCharsets.UTF_8).split("\n", -1)) {
            if (line.startsWith(SaveManager.LEAGUE_LIST_PREFIX + BoxScoreCodec.KEY + "\t")) {
                line = SaveManager.LEAGUE_LIST_PREFIX + BoxScoreCodec.KEY + "\tz1\tnot*base64";
            }
            damaged.append(line).append('\n');
        }
        League loaded = load(damaged.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(league.currentWeek, loaded.currentWeek);
        assertEquals(league.userTeam.getGameSchedule().get(0).homeScore,
                loaded.userTeam.getGameSchedule().get(0).homeScore);
    }

    @Test
    public void base64_roundTrips() {
        java.util.Random r = new java.util.Random(7);
        for (int n = 0; n < 40; n++) {
            byte[] b = new byte[n];
            r.nextBytes(b);
            String enc = BoxScoreCodec.base64(b);
            assertEquals(java.util.Base64.getEncoder().encodeToString(b), enc);
            assertArrayEquals(b, BoxScoreCodec.unbase64(enc));
        }
    }
}
