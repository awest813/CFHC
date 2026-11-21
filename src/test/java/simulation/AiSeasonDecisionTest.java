package simulation;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import positions.Player;
import positions.PlayerQB;
import positions.PlayerWR;
import staff.HeadCoach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * AI tests for the decisions the sim makes for CPU programs over a career:
 * depth charts, roster building, recruiting classes, the transfer portal,
 * head-coach hires and playbooks. One league plays five full seasons through
 * the {@link SeasonController} (offseasons included) while the probes below
 * tally what the CPU staffs did; each test asserts one property of the tally.
 */
public class AiSeasonDecisionTest {

    private static final int SEASONS = 5;
    private static final String[] POS = {"QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"};
    private static final int[] STARTERS = {
            RosterRules.STARTERS_QB, RosterRules.STARTERS_RB, RosterRules.STARTERS_WR, RosterRules.STARTERS_TE,
            RosterRules.STARTERS_OL, RosterRules.STARTERS_K, RosterRules.STARTERS_DL, RosterRules.STARTERS_LB,
            RosterRules.STARTERS_CB, RosterRules.STARTERS_S};

    private static long starterSlots, bestStarterSlots, redshirtStarters, weeksWithoutQb, slowOptionBooks;
    private static long rosterChecks, belowMinimum, overCap;
    private static long classes, kickerHoards, qbDroughts3;
    private static final List<double[]> prestigeVsClassOvr = new ArrayList<>();
    private static long hires, downgrades;
    private static double hireDelta;
    private static long transfersPlaced, transfersToFcs;
    private static final List<String> notes = new ArrayList<>();

    @BeforeClass
    public static void playFiveSeasons() {
        PlatformLog.setDebugEnabled(false);
        SimRandom.pinNextSeed(0xA15EA5L);
        FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
        League league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(res);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        league.careerMode = false;
        SeasonController controller = new SeasonController(league, new HeadlessBridge(league));

        Map<Team, HeadCoach> lastCoach = new IdentityHashMap<>();
        Map<Team, Set<Player>> rosterAtGate = new IdentityHashMap<>();
        Map<Team, Integer> qbDrought = new IdentityHashMap<>();
        int lastWeekMeasured = -1;
        int seasonsDone = 0;
        int advances = 0;
        while (seasonsDone < SEASONS && advances++ < 400) {
            int week = league.currentWeek;
            if (week >= 1 && week < league.regSeasonWeeks - 1 && week != lastWeekMeasured) {
                lastWeekMeasured = week;
                for (Team t : league.getTeamList()) {
                    if (!t.isUserControlled()) probeDepthChart(t, week);
                }
            }
            if (SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks) && rosterAtGate.isEmpty()) {
                for (Team t : league.getTeamList()) rosterAtGate.put(t, new HashSet<>(t.getAllPlayers()));
            }

            controller.advanceWeek();

            for (Team t : league.getTeamList()) {
                HeadCoach now = t.getHeadCoach();
                if (now == null) continue;
                HeadCoach prev = lastCoach.put(t, now);
                if (prev != null && prev != now && !t.isUserControlled()) {
                    hires++;
                    hireDelta += now.ratOvr - prev.ratOvr;
                    if (now.ratOvr < prev.ratOvr - 5) downgrades++;
                }
            }
            if (league.currentWeek == 0 && lastWeekMeasured != -1) {
                // A new season has started: the offseason's work is done.
                lastWeekMeasured = -1;
                seasonsDone++;
                for (Team t : league.getTeamList()) {
                    if (!t.isUserControlled()) probeNewSeason(t, rosterAtGate.get(t), qbDrought);
                }
                if (league.transfersList != null) {
                    for (String line : league.transfersList) {
                        if (line.contains("(FCS)")) transfersToFcs++;
                        else transfersPlaced++;
                    }
                }
                rosterAtGate.clear();
            }
        }
        assertEquals("seasons completed", SEASONS, seasonsDone);
    }

    private static List<? extends Player> depth(Team t, int pos) {
        switch (pos) {
            case 0: return t.getTeamQBs();
            case 1: return t.getTeamRBs();
            case 2: return t.getTeamWRs();
            case 3: return t.getTeamTEs();
            case 4: return t.getTeamOLs();
            case 5: return t.getTeamKs();
            case 6: return t.getTeamDLs();
            case 7: return t.getTeamLBs();
            case 8: return t.getTeamCBs();
            default: return t.getTeamSs();
        }
    }

    /** Kickoff lineup (fresh legs: the first healthy players on the chart) against the best eligible players. */
    private static void probeDepthChart(Team t, int week) {
        for (int p = 0; p < POS.length; p++) {
            List<? extends Player> chart = depth(t, p);
            List<Player> lineup = new ArrayList<>();
            List<Integer> eligible = new ArrayList<>();
            for (Player x : chart) {
                if (Game.canPlay(x) && lineup.size() < STARTERS[p]) lineup.add(x);
                if (Game.canPlay(x) && !x.isRedshirt && !x.isTransfer) eligible.add(x.ratOvr);
            }
            eligible.sort(Collections.reverseOrder());
            int cutoff = eligible.size() >= STARTERS[p] ? eligible.get(STARTERS[p] - 1) : Integer.MIN_VALUE;
            for (Player s : lineup) {
                starterSlots++;
                if (s.ratOvr >= cutoff) bestStarterSlots++;
                else if (notes.size() < 10) notes.add(t.getAbbr() + " wk" + week + " " + POS[p] + " " + s.ratOvr + " < " + cutoff);
                if (s.isRedshirt) redshirtStarters++;
            }
            if (p == 0 && lineup.isEmpty()) weeksWithoutQb++;
        }
        if (t.getPlaybookOffense().featuresQbRuns() && t.starterQbSpeed() < Team.READ_OPTION_QB_SPEED
                && week == 1) {
            slowOptionBooks++;
        }
    }

    private static void probeNewSeason(Team t, Set<Player> before, Map<Team, Integer> qbDrought) {
        rosterChecks++;
        if (t.getAllPlayers().size() > RosterRules.MAX_PLAYERS) overCap++;
        int[] cls = new int[POS.length];
        double ovr = 0;
        int signed = 0;
        for (int p = 0; p < POS.length; p++) {
            List<? extends Player> chart = depth(t, p);
            if (chart.size() < RosterRules.minForPosition(POS[p])) belowMinimum++;
            if (before == null) continue;
            for (Player x : chart) {
                if (!before.contains(x) && x.year <= 1) {
                    cls[p]++;
                    ovr += x.ratOvr;
                    signed++;
                }
            }
        }
        if (before == null) return;
        classes++;
        if (cls[5] > 2) kickerHoards++;
        int drought = cls[0] == 0 ? qbDrought.getOrDefault(t, 0) + 1 : 0;
        qbDrought.put(t, drought);
        if (drought >= 3) qbDroughts3++;
        if (signed > 0) prestigeVsClassOvr.add(new double[]{t.getTeamPrestige(), ovr / signed});
    }

    // ---- depth charts -------------------------------------------------------

    @Test
    public void depthCharts_startTheBestHealthyPlayers() {
        double share = bestStarterSlots / (double) starterSlots;
        assertTrue("best healthy player starts " + share + " " + notes, share >= 0.95);
        assertTrue("redshirts starting: " + redshirtStarters, redshirtStarters <= starterSlots / 1000);
        assertEquals("CPU teams without a healthy QB on game week", 0, weeksWithoutQb);
    }

    // ---- rosters and recruiting --------------------------------------------

    @Test
    public void rosters_keepPositionMinimumsAndTheCap() {
        assertTrue(rosterChecks >= 100L * (SEASONS - 1));
        assertEquals("position groups below minimum", 0, belowMinimum);
        assertEquals("CPU rosters over " + RosterRules.MAX_PLAYERS, 0, overCap);
    }

    @Test
    public void recruiting_keepsAQuarterbackPipeline_andNoKickerHoards() {
        assertTrue(classes > 300);
        assertEquals("classes with 3+ kickers", 0, kickerHoards);
        assertEquals("programs three straight classes without a QB", 0, qbDroughts3);
    }

    @Test
    public void recruiting_classQualityRisesWithPrestige() {
        List<double[]> rows = new ArrayList<>(prestigeVsClassOvr);
        rows.sort((a, b) -> Double.compare(a[0], b[0]));
        int q = rows.size() / 5;
        double[] mean = new double[5];
        for (int i = 0; i < 5; i++) {
            double sum = 0;
            for (int j = i * q; j < (i + 1) * q; j++) sum += rows.get(j)[1];
            mean[i] = sum / q;
        }
        String table = java.util.Arrays.toString(mean);
        assertTrue("top quintile signs clearly better classes " + table, mean[4] > mean[0] + 3);
        for (int i = 1; i < 5; i++) {
            assertTrue("monotone-ish by prestige " + table, mean[i] > mean[i - 1] - 1);
        }
    }

    @Test
    public void portal_placesMostTransfersAtFbsSchools() {
        assertTrue("transfers placed " + transfersPlaced + " vs to FCS " + transfersToFcs,
                transfersPlaced > 0 && transfersToFcs < transfersPlaced * 0.10);
    }

    // ---- staff --------------------------------------------------------------

    @Test
    public void coachHires_doNotSystematicallyDowngrade() {
        assertTrue("head-coach changes seen: " + hires, hires >= 10);
        assertTrue("average rating change on a hire " + hireDelta / hires, hireDelta / hires >= 0);
        assertTrue("hires 5+ points worse: " + downgrades + " of " + hires, downgrades <= hires / 4);
    }

    @Test
    public void playbooks_noReadOptionWithoutARunningQuarterback() {
        assertEquals("CPU read-option books with a slow starting QB in week 1", 0, slowOptionBooks);
    }

    // ---- unit rows for the helpers ------------------------------------------

    @Test
    public void transferWouldStart_comparesAgainstTheLastStarter() {
        Team t = new ArrayList<>(sharedTeams()).get(5);
        List<PlayerWR> wrs = new ArrayList<>(t.getTeamWRs());
        wrs.sort((a, b) -> b.ratOvr - a.ratOvr);
        PlayerWR probe = wrs.get(wrs.size() - 1);
        int wr1 = wrs.get(0).ratOvr, wr3 = wrs.get(2).ratOvr;
        probe.ratOvr = wr3 + 1;
        assertTrue("beats WR3: starts", League.wouldStart(wrs, 3, probe));
        probe.ratOvr = Math.min(wr1, wr3) - 1;
        assertFalse(League.wouldStart(wrs, 3, probe));
        assertTrue("short room", League.wouldStart(wrs.subList(0, 2), 3, probe));
    }

    @Test
    public void pipelineNeed_signsWhenTheRoomIsAllUpperclassmen() {
        Team t = new ArrayList<>(sharedTeams()).get(7);
        List<PlayerQB> qbs = new ArrayList<>(t.getTeamQBs());
        int[] years = new int[qbs.size()];
        for (int i = 0; i < qbs.size(); i++) years[i] = qbs.get(i).year;
        try {
            for (PlayerQB qb : qbs) qb.year = 3;
            assertEquals(1, Team.pipelineNeed(qbs, 2));
            qbs.get(0).year = 1;
            assertEquals(0, Team.pipelineNeed(qbs, 2));
            for (PlayerQB qb : qbs) qb.year = 4;
            assertEquals("capped at two a year", 2, Team.pipelineNeed(qbs, 7));
        } finally {
            for (int i = 0; i < qbs.size(); i++) qbs.get(i).year = years[i];
        }
    }

    private static List<Team> sharedTeamsCache;

    private static List<Team> sharedTeams() {
        if (sharedTeamsCache == null) {
            SimRandom.pinNextSeed(0xA15EA6L);
            FileSystemResourceProvider res = new FileSystemResourceProvider(System.getProperty("user.dir"));
            League l = new League(
                    res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                    res.getString(PlatformResourceProvider.KEY_TEAMS),
                    res.getString(PlatformResourceProvider.KEY_BOWLS),
                    false, false);
            sharedTeamsCache = l.getTeamList();
        }
        return sharedTeamsCache;
    }

    /** No UI: recruiting runs the CPU pass and rolls into the next season. */
    private static final class HeadlessBridge implements GameUiBridge {
        private final League league;

        HeadlessBridge(League league) {
            this.league = league;
        }

        @Override public void crash() {}
        @Override public void startRecruiting(File f, Team t) {}
        @Override public void transferPlayer(Player p) {}
        @Override public void updateSpinners() {}
        @Override public void disciplineAction(Player p, String issue, int a, int b) {}
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
        @Override public void startRecruitingFlow() {
            league.recruitPlayers();
            league.finishRecruitingSeason("");
        }
    }
}
