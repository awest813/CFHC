package simulation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * A save made at any week must reload into the same league and keep playing.
 * Android autosaves every week, so a postseason save (conference title week through
 * the national title game) that lost its bracket crashed on the next Play, and team
 * program state (budget, discipline, facilities, sanctions, career totals) was
 * silently reset by every reload.
 */
public class SaveRoundTripAnyWeekTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider resources =
            new FileSystemResourceProvider(System.getProperty("user.dir"));
    private int recruitingFlows;

    private GameUiBridge bridge() {
        return new GameUiBridge() {
            @Override public void crash() { throw new AssertionError("bridge crash"); }
            @Override public void startRecruiting(File f, Team t) {}
            @Override public void transferPlayer(positions.Player p) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player p, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() { recruitingFlows++; }
        };
    }

    private League newLeague(boolean expandedPlayoffs) {
        League league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.expPlayoffs = expandedPlayoffs;
        Team user = league.getTeamList().get(0);
        user.setUserControlled(true);
        user.getHeadCoach().user = true;
        league.userTeam = user;
        return league;
    }

    private League reload(File save) throws Exception {
        League loaded = new League(save,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES), bridge(), true);
        loaded.setPlatformResourceProvider(resources);
        loaded.rebuildScheduleIfNeeded();
        loaded.updateTeamTalentRatings();
        return loaded;
    }

    private League simToWeekSaveAndReload(int week, boolean expandedPlayoffs, League[] originalOut) throws Exception {
        League league = newLeague(expandedPlayoffs);
        SeasonController controller = new SeasonController(league, bridge());
        while (league.currentWeek < week) {
            controller.advanceWeek();
        }
        assertEquals(week, league.currentWeek);
        File save = tmp.newFile("week" + week + (expandedPlayoffs ? "" : "-cfp4") + ".cfb");
        assertTrue(league.saveLeague(save));
        originalOut[0] = league;
        return reload(save);
    }

    /** Plays from the loaded week through recruiting into the next season's first weeks. */
    private void playIntoNextSeason(League league) {
        int year = league.getYear();
        SeasonController controller = new SeasonController(league, bridge());
        recruitingFlows = 0;
        int steps = 0;
        while (recruitingFlows == 0 && steps++ < 40) {
            controller.advanceWeek();
        }
        assertEquals("recruiting should start once", 1, recruitingFlows);
        league.recruitPlayers();
        league.finishRecruitingSeason("");
        assertEquals(0, league.currentWeek);
        while (league.currentWeek < 3) {
            controller.advanceWeek();
        }
        assertTrue("season should roll over", league.getYear() >= year);
    }

    private static String name(Game g) {
        return g == null ? "null" : g.gameName + ":" + g.homeTeam.getName() + "-" + g.awayTeam.getName();
    }

    private static void assertSameLeague(League before, League after) {
        assertEquals(before.currentWeek, after.currentWeek);
        assertEquals(before.getYear(), after.getYear());
        assertEquals(before.getLeagueHistory().size(), after.getLeagueHistory().size());
        assertEquals(before.userTeam.getName(), after.userTeam.getName());
        assertArrayEquals(before.bowlNames, after.bowlNames);
        assertEquals(before.longestWinStreak.getStreakLength(), after.longestWinStreak.getStreakLength());
        for (Team t : before.getTeamList()) {
            Team u = after.findTeam(t.getName());
            assertNotNull(t.getName(), u);
            String n = t.getName();
            assertEquals(n + " budget", t.teamBudget, u.teamBudget);
            assertEquals(n + " discipline", t.teamDisciplineScore, u.teamDisciplineScore);
            assertEquals(n + " facilities", t.teamFacilities, u.teamFacilities);
            assertEquals(n + " stadium", t.teamStadium, u.teamStadium);
            assertEquals(n + " location", t.getLocation(), u.getLocation());
            assertEquals(n + " conference", t.getConference(), u.getConference());
            assertEquals(n + " division", t.getDivision(), u.getDivision());
            assertEquals(n + " bowl ban", t.isBowlBan(), u.isBowlBan());
            assertEquals(n + " penalized", t.penalized, u.penalized);
            assertEquals(n + " recent penalty", t.recentPenalty, u.recentPenalty);
            assertEquals(n + " total wins", t.totalWins, u.totalWins);
            assertEquals(n + " total losses", t.totalLosses, u.totalLosses);
            assertEquals(n + " total CCs", t.totalCCs, u.totalCCs);
            assertEquals(n + " total NCs", t.totalNCs, u.totalNCs);
            assertEquals(n + " total bowls", t.totalBowls, u.totalBowls);
            assertEquals(n + " total bowl losses", t.totalBowlLosses, u.totalBowlLosses);
            assertEquals(n + " win streak", t.getWinStreak().getStreakLength(), u.getWinStreak().getStreakLength());
            assertEquals(n + " CC flag", t.confChampion, u.confChampion);
            assertEquals(n + " semifinal flag", t.semiFinalWL, u.semiFinalWL);
            assertEquals(n + " NC flag", t.natChampWL, u.natChampWL);
            assertEquals(n + " points", t.teamPoints, u.teamPoints);
            assertEquals(n + " W/L list", t.getGameWLSchedule(), u.getGameWLSchedule());
            assertEquals(n + " conf wins", t.getConfWins(), u.getConfWins());
            assertEquals(n + " head coach", t.getHeadCoach() == null, u.getHeadCoach() == null);
        }
        for (int i = 0; i < before.getConferences().size(); i++) {
            Conference c = before.getConferences().get(i);
            Conference d = after.getConferences().get(i);
            assertEquals(c.confName, d.confName);
            assertEquals(c.confName + " CCG", name(c.getChampionshipGame()), name(d.getChampionshipGame()));
            assertEquals(c.confName + " TV", c.TV, d.TV);
        }
        assertEquals(before.hasScheduledBowls, after.hasScheduledBowls);
        assertEquals(names(before.getPlayoffTeams()), names(after.getPlayoffTeams()));
        for (int i = 0; i < before.cfpGames.length; i++) {
            assertEquals("cfp " + i, name(before.cfpGames[i]), name(after.cfpGames[i]));
        }
        for (int i = 0; i < before.bowlGames.length; i++) {
            assertEquals("bowl " + i, name(before.bowlGames[i]), name(after.bowlGames[i]));
        }
        assertEquals(name(before.semiG14), name(after.semiG14));
        assertEquals(name(before.semiG23), name(after.semiG23));
        assertEquals(name(before.ncg), name(after.ncg));
    }

    private static List<String> names(List<Team> teams) {
        List<String> out = new ArrayList<>();
        for (Team t : teams) out.add(t.getName());
        return out;
    }

    private void roundTripAt(int week, boolean expandedPlayoffs) throws Exception {
        League[] original = new League[1];
        League loaded = simToWeekSaveAndReload(week, expandedPlayoffs, original);
        assertSameLeague(original[0], loaded);
        playIntoNextSeason(loaded);
    }

    @Test
    public void conferenceChampionshipWeekSave_resumes() throws Exception {
        League[] original = new League[1];
        int week = SeasonFlowOrder.conferenceChampionshipWeek(League.STANDARD_REG_SEASON_WEEKS);
        League loaded = simToWeekSaveAndReload(week, true, original);
        int scheduled = 0;
        for (Conference c : loaded.getConferences()) {
            if (c.getChampionshipGame() != null) scheduled++;
        }
        assertTrue("conference title games should survive the load", scheduled > 0);
        assertSameLeague(original[0], loaded);
        playIntoNextSeason(loaded);
    }

    @Test
    public void playoffFirstRoundWeekSave_resumes() throws Exception {
        League[] original = new League[1];
        League loaded = simToWeekSaveAndReload(SeasonFlowOrder.bowlWeek1(League.STANDARD_REG_SEASON_WEEKS),
                true, original);
        assertEquals(League.EXPANDED_PLAYOFF_TEAM_COUNT, loaded.getPlayoffTeams().size());
        for (int i = 0; i < 4; i++) assertNotNull("first round " + i, loaded.cfpGames[i]);
        assertTrue(loaded.hasScheduledBowls);
        assertNotNull(loaded.bowlGames[0]);
        assertSameLeague(original[0], loaded);
        playIntoNextSeason(loaded);
    }

    @Test
    public void quarterfinalWeekSave_resumes() throws Exception {
        League[] original = new League[1];
        League loaded = simToWeekSaveAndReload(SeasonFlowOrder.bowlWeek2(League.STANDARD_REG_SEASON_WEEKS),
                true, original);
        for (int i = 4; i < 8; i++) assertNotNull("quarterfinal " + i, loaded.cfpGames[i]);
        assertSameLeague(original[0], loaded);
        playIntoNextSeason(loaded);
    }

    @Test
    public void semifinalWeekSave_resumes() throws Exception {
        League[] original = new League[1];
        League loaded = simToWeekSaveAndReload(SeasonFlowOrder.bowlWeek3(League.STANDARD_REG_SEASON_WEEKS),
                true, original);
        assertNotNull(loaded.cfpGames[8]);
        assertNotNull(loaded.cfpGames[9]);
        assertSameLeague(original[0], loaded);
        playIntoNextSeason(loaded);
    }

    @Test
    public void nationalChampionshipWeekSave_resumes() throws Exception {
        League[] original = new League[1];
        League loaded = simToWeekSaveAndReload(
                SeasonFlowOrder.nationalChampionshipWeek(League.STANDARD_REG_SEASON_WEEKS), true, original);
        assertNotNull(loaded.ncg);
        assertSameLeague(original[0], loaded);
        playIntoNextSeason(loaded);
    }

    @Test
    public void fourTeamPlayoffWeeks_resume() throws Exception {
        roundTripAt(SeasonFlowOrder.bowlWeek3(League.STANDARD_REG_SEASON_WEEKS), false);
        roundTripAt(SeasonFlowOrder.nationalChampionshipWeek(League.STANDARD_REG_SEASON_WEEKS), false);
    }

    @Test
    public void midOffseasonSave_resumes() throws Exception {
        // Between the job-offer step and the carousel some head-coach posts are vacant.
        roundTripAt(SeasonFlowOrder.coachCarouselWeek(League.STANDARD_REG_SEASON_WEEKS), true);
    }

    @Test
    public void regularSeasonSave_keepsConferenceStandings() throws Exception {
        roundTripAt(6, true);
    }

    /** A v1.4e save (no state lines) from the playoff weeks still loads and finishes the season. */
    @Test
    public void legacyPostseasonSave_loadsAndFinishesSeason() throws Exception {
        League league = newLeague(true);
        SeasonController controller = new SeasonController(league, bridge());
        while (league.currentWeek < SeasonFlowOrder.bowlWeek2(League.STANDARD_REG_SEASON_WEEKS)) {
            controller.advanceWeek();
        }
        List<String> legacy = new ArrayList<>();
        for (String line : new String(league.serializeSave(), StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith(SaveSchema.VERSION_PREFIX)) {
                legacy.add(SaveSchema.VERSION_PREFIX + SaveSchema.V1_4E);
            } else if (!line.matches("^(TX|CX|LS|LH|HH|BN|PO):.*")) {
                legacy.add(line);
            }
        }
        File save = tmp.newFile("legacy.cfb");
        Files.write(save.toPath(), legacy);
        League loaded = reload(save);
        assertEquals(SaveSchema.V1_4E, loaded.saveVer);
        assertEquals(League.EXPANDED_PLAYOFF_TEAM_COUNT, loaded.getPlayoffTeams().size());
        for (int i = 4; i < 8; i++) assertNotNull("quarterfinal " + i, loaded.cfpGames[i]);
        playIntoNextSeason(loaded);
    }

    @Test
    public void importOfCurrentFormatSave_findsUserTeam() throws Exception {
        League league = newLeague(true);
        SeasonController controller = new SeasonController(league, bridge());
        while (league.currentWeek < 3) {
            controller.advanceWeek();
        }
        byte[] save = league.serializeSave();
        File filesDir = tmp.newFolder("files");
        LeagueLaunchCoordinator.LaunchResult result = LeagueLaunchCoordinator.load(
                LeagueLaunchCoordinator.LaunchRequest.importSave("content://save"),
                filesDir, bridge(), League.DEFAULT_NEW_SAVE_YEAR,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                null,
                uri -> new ByteArrayInputStream(save));
        assertTrue(result.loadedLeague);
        assertNotNull(result.userTeam);
        assertEquals(league.userTeam.getName(), result.userTeam.getName());
        assertEquals(3, result.league.currentWeek);
        File[] left = filesDir.listFiles();
        assertEquals("the staged import file is removed", 0, left == null ? 0 : left.length);
    }
}
