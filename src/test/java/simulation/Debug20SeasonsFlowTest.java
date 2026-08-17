package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Debug-focused 20-season run. Unlike Stress100SeasonsTest (invariant
 * checking), this traces the FULL menu/dialog flow: every SeasonAdvanceResult
 * event (dialog type, status text, button label) is recorded so the UI flow
 * can be audited against what the engine actually emits.
 *
 * Run on demand:
 *   ./gradlew -p desktop-standalone :engine:test --tests "simulation.Debug20SeasonsFlowTest"
 */
public class Debug20SeasonsFlowTest {

    private static final int TARGET_SEASONS = 20;
    private static final int MAX_ADVANCES = 8000;

    private League league;
    private SeasonController controller;
    private final List<String> flowLog = new ArrayList<>();
    private final List<String> anomalies = new ArrayList<>();

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
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.userControlled = true;
        league.careerMode = true;
        league.neverRetire = true;

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
    public void twentySeasons_flowTraced_noAnomalies() {
        int lastYear = league.getYear();
        int seasonsCompleted = 0;
        int advances = 0;
        String lastStatus = "";
        String lastButton = "";

        while (seasonsCompleted < TARGET_SEASONS && advances < MAX_ADVANCES) {
            int weekBefore = league.currentWeek;
            SeasonAdvanceResult result;
            try {
                result = controller.advanceWeek();
            } catch (Throwable e) {
                anomalies.add("CRASH s" + (seasonsCompleted + 1) + " wk" + weekBefore
                        + ": " + e + "\n" + stack(e));
                break;
            }
            advances++;

            // Trace the flow: week transition + dialog events + status.
            String weekMove = "wk" + weekBefore + "->" + result.weekAfter;
            List<String> dialogs = new ArrayList<>();
            String statusLine = "";
            String buttonLine = "";
            for (SeasonAdvanceResult.Event ev : result.events) {
                if (ev.type == SeasonAdvanceResult.EventType.NEEDS_DIALOG && ev.dialogType != null) {
                    dialogs.add(ev.dialogType.name());
                } else if (ev.type == SeasonAdvanceResult.EventType.STATUS_UPDATED) {
                    statusLine = ev.statusText != null ? ev.statusText : "";
                    buttonLine = ev.buttonText != null ? ev.buttonText : "";
                }
            }

            if (!dialogs.isEmpty()) {
                flowLog.add("S" + (seasonsCompleted + 1) + " " + weekMove
                        + " DIALOG[" + String.join(",", dialogs) + "]");
            }
            if (!statusLine.equals(lastStatus) || !buttonLine.equals(lastButton)) {
                flowLog.add("S" + (seasonsCompleted + 1) + " " + weekMove
                        + " status='" + statusLine + "' btn='" + buttonLine + "'");
                lastStatus = statusLine;
                lastButton = buttonLine;
            }

            // Anomaly checks on the flow itself. The season rollover
            // (recruiting gate at R+13 -> week 0 of the new season) is the
            // one legitimate backward week move.
            boolean isSeasonRollover = weekBefore >= league.regSeasonWeeks + 13
                    && result.weekAfter == 0;
            if (result.weekAfter < weekBefore && weekBefore != 0 && !isSeasonRollover) {
                anomalies.add("S" + (seasonsCompleted + 1) + " week went BACKWARD: "
                        + weekBefore + "->" + result.weekAfter);
            }
            if (result.weekAfter == weekBefore
                    && !result.hasEvent(SeasonAdvanceResult.EventType.RECRUITING_STARTED)
                    && !result.hasEvent(SeasonAdvanceResult.EventType.AWAITING_RECRUITING)) {
                // Only the recruiting gate legitimately doesn't advance.
                anomalies.add("S" + (seasonsCompleted + 1) + " week stuck at " + weekBefore
                        + " without a recruiting event");
            }

            if (league.getYear() > lastYear) {
                seasonsCompleted++;
                lastYear = league.getYear();
                flowLog.add("=== SEASON " + seasonsCompleted + " COMPLETE (year "
                        + league.getYear() + ") ===");
                checkSeasonHealth(seasonsCompleted);
            }
        }

        // Print the flow trace for the audit.
        System.out.println("=========== 20-SEASON FLOW TRACE (" + flowLog.size() + " events) ===========");
        for (String line : flowLog) {
            System.out.println(line);
        }
        System.out.println("=========== ANOMALIES (" + anomalies.size() + ") ===========");
        for (String a : anomalies) {
            System.out.println(a);
        }

        assertEquals("20 seasons should complete (got " + seasonsCompleted + ")",
                TARGET_SEASONS, seasonsCompleted);
        assertTrue("flow anomalies detected:\n" + String.join("\n", anomalies),
                anomalies.isEmpty());
    }

    private void checkSeasonHealth(int seasonNum) {
        String tag = "S" + seasonNum + ": ";
        try {
            assertNotNull(tag + "userTeam", league.userTeam);
            assertNotNull(tag + "HC", league.userTeam.getHeadCoach());
            assertTrue(tag + "team count", league.getTeamList().size() >= 80);
            assertTrue(tag + "roster", league.userTeam.getAllPlayers().size() >= 40);
            for (Team t : league.getTeamList()) {
                assertTrue(tag + t.name + " prestige>=0", t.teamPrestige >= 0);
                assertTrue(tag + t.name + " prestige<=200", t.teamPrestige <= Team.PRESTIGE_SOFT_MAX);
                assertTrue(tag + t.name + " wins>=0", t.wins >= 0);
                assertTrue(tag + t.name + " losses>=0", t.losses >= 0);
            }
        } catch (AssertionError ae) {
            anomalies.add(tag + ae.getMessage());
        }
    }

    private static String stack(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        return s.length() > 2000 ? s.substring(0, 2000) + "..." : s;
    }
}
