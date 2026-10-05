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

import positions.Player;
import positions.PlayerWR;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingPresentation;
import recruiting.RecruitingSessionData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Recruiting gate state across save/load, CPU class sizing, and board parity. */
public class RecruitingFlowTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    }

    @Test
    public void saveJustBeforeTheGate_stillRunsCpuRecruitingAfterReload() throws Exception {
        League league = newLeague();
        advanceTo(league, SeasonFlowOrder.recruitingWeek(league.regSeasonWeeks));
        assertFalse("the gate hasn't opened yet", league.recruitingPhaseActive);

        League loaded = load(save(league));

        assertFalse("reloaded league must still run the gate", loaded.recruitingPhaseActive);
        RecordingBridge bridge = new RecordingBridge();
        new SeasonController(loaded, bridge).advanceWeek();
        assertTrue("CPU recruiting runs on the next advance", bridge.recruitingStarted);
    }

    @Test
    public void saveAfterCpuRecruiting_reloadsInsideTheGate() throws Exception {
        League league = newLeague();
        advanceTo(league, SeasonFlowOrder.recruitingWeek(league.regSeasonWeeks));
        RecordingBridge bridge = new RecordingBridge(league);
        new SeasonController(league, bridge).advanceWeek();
        assertTrue(league.recruitingPhaseActive);

        League loaded = load(save(league));

        assertTrue(loaded.recruitingPhaseActive);
    }

    @Test
    public void savesWithoutTheGateField_inferItFromCpuRosters() throws Exception {
        League league = newLeague();
        advanceTo(league, SeasonFlowOrder.recruitingWeek(league.regSeasonWeeks));
        File beforeCpu = withoutGateField(save(league));
        SimulationFacade.prepareCpuRecruiting(league);
        league.recruitingPhaseActive = true;
        File afterCpu = withoutGateField(save(league));

        assertFalse("graduation left CPU rosters short: recruiting hasn't run", load(beforeCpu).recruitingPhaseActive);
        assertTrue("every CPU roster is filled: recruiting ran", load(afterCpu).recruitingPhaseActive);
    }

    @Test
    public void cpuRecruiting_fillsToTheMinimumWithoutInflatingSurplusRosters() {
        League league = newLeague();
        Team deep = league.getTeamList().get(5);
        for (int i = 0; i < 10; i++) {
            deep.addPlayerWR(new PlayerWR("Surplus " + i, 2, 5, deep));
        }
        java.util.Set<Player> existing = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        existing.addAll(deep.getAllPlayers());
        int before = existing.size();
        int shortfall = shortfall(deep);

        league.recruitPlayers();

        for (Team t : league.getTeamList()) {
            if (!t.isUserControlled()) {
                assertTrue(t.getName() + " below the roster minimum",
                        t.getAllPlayers().size() >= RosterRules.MIN_PLAYERS);
            }
        }
        int walkOns = 0;
        for (Player p : deep.getAllPlayers()) {
            if (!existing.contains(p) && p.isWalkOn) walkOns++;
        }
        assertEquals("ten surplus WRs used to add ten extra random recruits",
                Math.max(RosterRules.MIN_PLAYERS, before + shortfall),
                deep.getAllPlayers().size() - walkOns);
    }

    @Test
    public void desktopBoardBudget_matchesAndroid() {
        League league = newLeague();
        String payload = SimulationFacade.buildRecruitingPayload(league.userTeam);
        RecruitingSessionData desktop = SimulationFacade.prepareRecruitingSessionFromPayload(payload);
        RecruitingSessionData android = RecruitingSessionData.fromUserTeamInfo(payload);
        android.applyBudgetBonuses(RosterRules.MIN_PLAYERS);

        assertEquals(android.recruitingBudget, desktop.recruitingBudget);
        assertEquals(RosterRules.MIN_OLS - desktop.teamOLs.size(), desktop.calculateNeeds().ols);
    }

    @Test
    public void scoutingRevealsPotential() {
        League league = newLeague();
        RecruitingSessionData session = SimulationFacade.prepareRecruitingSessionFromPayload(
                SimulationFacade.buildRecruitingPayload(league.userTeam));
        RecruitingPlayerRecord recruit = session.availAll.get(0);

        String before = RecruitingPresentation.buildPotentialDetails(recruit, session.isScouted(recruit));
        assertTrue(before, before.contains("scout to reveal"));
        assertTrue(session.scoutPlayer(recruit));
        String after = RecruitingPresentation.buildPotentialDetails(recruit, session.isScouted(recruit));
        assertFalse(after, after.contains("scout to reveal"));
        assertTrue(after, after.split("\n")[0].matches("Potential: +\\*[ *]*"));
    }

    /** Players short of each position minimum, sitting transfers not counted (as CPUrecruiting does). */
    private static int shortfall(Team t) {
        int total = 0;
        total += Math.max(0, RosterRules.MIN_QBS - eligible(t.getTeamQBs()));
        total += Math.max(0, RosterRules.MIN_RBS - eligible(t.getTeamRBs()));
        total += Math.max(0, RosterRules.MIN_WRS - eligible(t.getTeamWRs()));
        total += Math.max(0, RosterRules.MIN_TES - eligible(t.getTeamTEs()));
        total += Math.max(0, RosterRules.MIN_OLS - eligible(t.getTeamOLs()));
        total += Math.max(0, RosterRules.MIN_KS - eligible(t.getTeamKs()));
        total += Math.max(0, RosterRules.MIN_DLS - eligible(t.getTeamDLs()));
        total += Math.max(0, RosterRules.MIN_LBS - eligible(t.getTeamLBs()));
        total += Math.max(0, RosterRules.MIN_CBS - eligible(t.getTeamCBs()));
        total += Math.max(0, RosterRules.MIN_SS - eligible(t.getTeamSs()));
        return total;
    }

    private static int eligible(List<? extends Player> players) {
        int n = 0;
        for (Player p : players) {
            if (!p.isTransfer) n++;
        }
        return n;
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
        league.userTeam.setupUserCoach("Recruiting Coach");
        league.userTeam.getHeadCoach().user = true;
        league.userTeam.setUserControlled(true);
        league.careerMode = false;
        return league;
    }

    private static void advanceTo(League league, int week) {
        SeasonController controller = new SeasonController(league, new RecordingBridge());
        int guard = 0;
        while (league.currentWeek < week && guard++ < 80) {
            controller.advanceWeek();
        }
        assertEquals(week, league.currentWeek);
    }

    private File save(League league) throws Exception {
        File file = tmp.newFile();
        assertTrue(league.saveLeague(file));
        return file;
    }

    /** Blanks L: field 7 (the gate flag): a save from before it was recorded. */
    private static File withoutGateField(File save) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(save.toPath(), StandardCharsets.UTF_8)) {
            if (line.startsWith("L:")) {
                String[] p = line.split("\t", -1);
                assertEquals("current L: lines carry 8 fields", 8, p.length);
                p[6] = "";  // keep field 8 (season length) in place
                line = String.join("\t", p);
            }
            lines.add(line);
        }
        Files.write(save.toPath(), lines, StandardCharsets.UTF_8);
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

    /** Headless bridge: records the gate; with a league, runs CPU recruiting like the real hosts. */
    private static final class RecordingBridge implements GameUiBridge {
        private final League league;
        boolean recruitingStarted;

        RecordingBridge() {
            this(null);
        }

        RecordingBridge(League league) {
            this.league = league;
        }

        @Override public void crash() {}
        @Override public void startRecruiting(File saveFile, Team userTeam) {}
        @Override public void transferPlayer(Player player) {}
        @Override public void updateSpinners() {}
        @Override public void disciplineAction(Player player, String issue, int gamesA, int gamesB) {}
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

        @Override
        public void startRecruitingFlow() {
            recruitingStarted = true;
            if (league != null) {
                SimulationFacade.prepareCpuRecruiting(league);
            }
        }
    }
}
