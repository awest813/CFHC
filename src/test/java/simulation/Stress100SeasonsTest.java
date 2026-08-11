package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Headless 100-season stress run. Advances every week of every offseason +
 * season + recruiting cycle 100 times, asserting league-health invariants at
 * each season boundary. Designed to surface crashes, state desync, and
 * numerical corruption over a long career.
 *
 * Not part of the default test task — run explicitly:
 *   ./gradlew -p desktop-standalone :engine:test --tests "simulation.Stress100SeasonsTest"
 */
public class Stress100SeasonsTest {

    private static final int TARGET_SEASONS = 100;
    private static final int MAX_ADVANCES = 40000;

    private League league;
    private SeasonController controller;
    private int advanceErrors;
    private final List<String> errors = new ArrayList<>();

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description d) {
            System.err.println("=== STRESS RUN FAILED after " + errors.size() + " season-boundary issues ===");
            for (String er : errors) {
                System.err.println(er);
            }
        }
    };

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        FileSystemResourceProvider resources = new FileSystemResourceProvider(projectRoot);

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
        assertFalse(league.getTeamList().isEmpty());
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.userControlled = true;
        league.careerMode = true;
        league.neverRetire = true;

        advanceErrors = 0;

        GameUiBridge bridge = new GameUiBridge() {
            @Override public void crash() {}
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
            @Override public void showJobOffersDialog() {
                // Recover from firing: keep the dynasty alive (mirrors the
                // PersonalityDynastyAuditTest strategy).
                if (league.userTeam != null && league.userTeam.fired) {
                    league.userTeam.fired = false;
                    league.userTeam.setUserControlled(true);
                    if (league.userTeam.getHeadCoach() != null) {
                        league.userTeam.getHeadCoach().retired = false;
                        league.userTeam.getHeadCoach().user = true;
                    }
                }
            }
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {
                league.recruitPlayers();
                if (league.userTeam != null) {
                    league.finishRecruitingSeason("");
                }
            }
        };

        controller = new SeasonController(league, bridge);
    }

    @Test
    public void hundredSeasons_completesWithoutErrorsOrCorruption() {
        int startYear = league.getYear();
        int lastYear = startYear;
        int seasonsCompleted = 0;
        int advances = 0;

        while (seasonsCompleted < TARGET_SEASONS && advances < MAX_ADVANCES) {
            try {
                controller.advanceWeek();
            } catch (Throwable e) {
                advanceErrors++;
                String msg = "CRASH season≈" + (seasonsCompleted + 1) + " week "
                        + league.currentWeek + " year " + league.getYear() + ": "
                        + e + "\n" + stack(e);
                errors.add(msg);
                fail(msg);
            }
            advances++;

            if (league.getYear() > lastYear) {
                seasonsCompleted++;
                lastYear = league.getYear();
                assertSeasonHealth(seasonsCompleted);
            }
        }

        assertTrue("advance budget exhausted before 100 seasons (completed "
                + seasonsCompleted + ")", seasonsCompleted >= TARGET_SEASONS);
        assertEquals("no advance errors should occur", 0, advanceErrors);
    }

    private void assertSeasonHealth(int seasonNum) {
        String tag = "[season " + seasonNum + "] ";
        try {
            assertNotNull(tag + "userTeam", league.userTeam);
            assertNotNull(tag + "HC", league.userTeam.getHeadCoach());
            assertTrue(tag + "team count sane", league.getTeamList().size() >= 80);
            assertTrue(tag + "roster size", league.userTeam.getAllPlayers().size() >= 40);

            // Ranks must be unique and in range across the league.
            league.setTeamRanks();
            assertUniqueRanks(tag + "poll", Team::getRankTeamPollScore);
            assertUniqueRanks(tag + "prestige", t -> t.rankTeamPrestige);

            // No team should have impossible win/loss/prestige values.
            for (Team t : league.getTeamList()) {
                assertTrue(tag + t.name + " wins>=0", t.wins >= 0);
                assertTrue(tag + t.name + " losses>=0", t.losses >= 0);
                assertTrue(tag + t.name + " prestige>=0", t.teamPrestige >= 0);
                assertTrue(tag + t.name + " prestige<250", t.teamPrestige < 250);
                assertNotNull(tag + t.name + " has HC", t.getHeadCoach());
            }

            // Depth chart sanity: user team has starters at key positions.
            Team u = league.userTeam;
            assertFalse(tag + "user QBs empty", u.getTeamQBs().isEmpty());
            assertFalse(tag + "user RBs empty", u.getTeamRBs().isEmpty());
            assertFalse(tag + "user OL empty", u.getTeamOLs().isEmpty());
        } catch (AssertionError ae) {
            errors.add(tag + ae.getMessage());
            throw ae;
        } catch (RuntimeException re) {
            errors.add(tag + "invariant check threw: " + re);
            throw re;
        }
    }

    private void assertUniqueRanks(String label, java.util.function.ToIntFunction<Team> rankFn) {
        List<Team> teams = league.getTeamList();
        int n = teams.size();
        Set<Integer> seen = new HashSet<>();
        for (Team t : teams) {
            int rank = rankFn.applyAsInt(t);
            assertTrue(label + " rank " + rank + " out of range for " + t.name,
                    rank >= 1 && rank <= n);
            assertTrue(label + " duplicate rank " + rank, seen.add(rank));
        }
        assertEquals(label + " incomplete", n, seen.size());
    }

    private static String stack(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        return s.length() > 3000 ? s.substring(0, 3000) + "..." : s;
    }
}
