package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Season-loop regressions: 12 regular-season weeks, legacy (13-week) saves,
 * and the shared play-button label.
 */
public class SeasonLengthTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
    }

    private static boolean isRegularSeasonGame(Game g) {
        return !g.isByeWeek() && ("OOC".equals(g.gameName) || "Conference".equals(g.gameName)
                || "Division".equals(g.gameName));
    }

    @Test
    public void newLeague_usesStandardCalendar() {
        assertEquals(League.STANDARD_REG_SEASON_WEEKS, league.regSeasonWeeks);
        assertEquals(League.REGULAR_SEASON_GAMES, league.regSeasonWeeks - 2);
    }

    @Test
    public void everyScheduledRegularSeasonGame_isPlayedBeforeChampionships() {
        SeasonController controller = new SeasonController(league, null);
        while (league.currentWeek < SeasonFlowOrder.conferenceChampionshipWeek(league.regSeasonWeeks)) {
            controller.advanceWeek();
        }
        for (Team t : league.getTeamList()) {
            int scheduled = 0;
            for (Game g : t.getGameSchedule()) {
                if (!isRegularSeasonGame(g)) continue;
                scheduled++;
                assertTrue(t.getName() + " has an unplayed " + g.gameName + " game", g.hasPlayed);
            }
            assertEquals(t.getName() + " record should match its schedule",
                    scheduled, t.getWins() + t.getLosses());
        }
    }

    @Test
    public void seasonLength_roundTripsThroughSave() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), out);
        LeagueRecord loaded = SaveManager.load(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(League.STANDARD_REG_SEASON_WEEKS, loaded.regSeasonWeeks());
    }

    @Test
    public void legacySaveWithoutSeasonLength_keepsThirteenWeekCalendar_untilRollover() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), out);
        // Strip field 8 (season length) from the L: header to mimic a save written before the fix.
        String text = out.toString(StandardCharsets.UTF_8.name());
        StringBuilder legacy = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (line.startsWith("L:")) {
                line = line.substring(0, line.lastIndexOf('\t'));
            }
            legacy.append(line).append('\n');
        }
        LeagueRecord record = SaveManager.load(new ByteArrayInputStream(
                legacy.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals("legacy header has no season length", 0, record.regSeasonWeeks());

        league.applyLeagueRecord(record);
        assertEquals(League.LEGACY_REG_SEASON_WEEKS, league.regSeasonWeeks);

        league.startNextSeason();
        assertEquals("rollover upgrades to the standard calendar",
                League.STANDARD_REG_SEASON_WEEKS, league.regSeasonWeeks);
    }

    @Test
    public void earlyFormatWithSeasonLengthInField7_stillLoadsTheCalendar() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SaveManager.save(league.toRecord(), out);
        // A short-lived build wrote the season length as field 7 (no gate flag).
        String text = out.toString(StandardCharsets.UTF_8.name());
        StringBuilder early = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (line.startsWith("L:")) {
                String[] p = line.split("\t", -1);
                line = String.join("\t", java.util.Arrays.copyOf(p, 6)) + "\t" + p[7];
            }
            early.append(line).append('\n');
        }
        LeagueRecord record = SaveManager.load(new ByteArrayInputStream(
                early.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(League.STANDARD_REG_SEASON_WEEKS, record.regSeasonWeeks());
        assertNull("no gate flag in that format", record.recruitingStarted());
    }

    @Test
    public void playLabels_nameTheNextAction() {
        int r = League.STANDARD_REG_SEASON_WEEKS;
        assertEquals("Begin Season", SeasonPresentation.getPlayWeekLabel(0, r, false, false));
        assertEquals("Play Week 1", SeasonPresentation.getPlayWeekLabel(1, r, false, false));
        assertEquals("Play Week 12", SeasonPresentation.getPlayWeekLabel(12, r, false, false));
        assertEquals("Play Conf Championships", SeasonPresentation.getPlayWeekLabel(13, r, false, false));
        assertEquals("Play Bowl Week 1", SeasonPresentation.getPlayWeekLabel(14, r, false, false));
        assertEquals("Play First Round", SeasonPresentation.getPlayWeekLabel(14, r, true, false));
        assertEquals("Play Quarterfinals", SeasonPresentation.getPlayWeekLabel(15, r, true, false));
        assertEquals("Play National Championship", SeasonPresentation.getPlayWeekLabel(17, r, true, false));
        assertEquals("Offseason: Season Summary", SeasonPresentation.getPlayWeekLabel(18, r, true, false));
        assertEquals("Offseason: Transfer Portal", SeasonPresentation.getPlayWeekLabel(24, r, true, false));
        assertEquals("Begin Recruiting", SeasonPresentation.getPlayWeekLabel(27, r, true, false));
        assertEquals("Complete Recruiting", SeasonPresentation.getPlayWeekLabel(27, r, true, true));
    }

    @Test
    public void controllerStatus_matchesSharedLabel_everyStep() {
        SeasonController controller = new SeasonController(league, null);
        for (int i = 0; i < 40; i++) {
            SeasonAdvanceResult res = controller.advanceWeek();
            SeasonAdvanceResult.Event status = res.lastStatusEvent();
            if (status != null) {
                assertEquals("week " + res.weekBefore + " -> " + res.weekAfter,
                        SeasonPresentation.getPlayWeekLabel(league), status.buttonText);
            }
            if (res.hasEvent(SeasonAdvanceResult.EventType.RECRUITING_STARTED)) break;
        }
    }
}
