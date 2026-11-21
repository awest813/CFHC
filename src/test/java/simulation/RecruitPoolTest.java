package simulation;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import positions.Player;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingPresentation;
import recruiting.RecruitingSessionData;

import static org.junit.Assert.*;

/**
 * The shared national recruiting pool ({@link RecruitPool}): one deterministic
 * class per cycle that CPU programs and the user sign from, with an early wave
 * before the user's board opens, decision points while the user signs, and a late
 * wave after. One league is played to the recruiting gate once; each test loads
 * that save.
 */
public class RecruitPoolTest {

    private static final long SEED = 0xB00C1A55L;
    private static final FileSystemResourceProvider RES =
            new FileSystemResourceProvider(System.getProperty("user.dir"));
    /** The league at the recruiting gate, phase active, before {@link League#recruitPlayers()}. */
    private static byte[] gateSave;

    @BeforeClass
    public static void playToTheGate() throws Exception {
        PlatformLog.setDebugEnabled(false);
        SimRandom.pinNextSeed(SEED);
        League league = new League(
                RES.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                RES.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                RES.getString(PlatformResourceProvider.KEY_CONFERENCES),
                RES.getString(PlatformResourceProvider.KEY_TEAMS),
                RES.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(RES);
        // A mid-table program, so some top prospects prefer rivals and some prefer the user.
        List<Team> byPrestige = new ArrayList<>(league.getTeamList());
        byPrestige.sort((a, b) -> Integer.compare(b.getTeamPrestige(), a.getTeamPrestige()));
        Team user = byPrestige.get(byPrestige.size() / 3);
        user.setUserControlled(true);
        user.getHeadCoach().user = true;
        league.userTeam = user;
        SeasonController c = new SeasonController(league, GameUiBridge.NO_OP);
        for (int guard = 0; guard < 80; guard++) {
            if (league.recruitingPhaseActive
                    && SeasonFlowOrder.isRecruitingGate(league.currentWeek, league.regSeasonWeeks)) {
                break;
            }
            c.advanceWeek();
        }
        assertTrue("reached the recruiting gate", league.recruitingPhaseActive);
        assertNull("no pool before recruitPlayers()", league.getRecruitPool());
        gateSave = league.serializeSave();
    }

    private static League load(byte[] save) throws Exception {
        File f = File.createTempFile("cfhc-pool", ".cfb");
        try {
            Files.write(f.toPath(), save);
            League loaded = new League(f,
                    RES.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    RES.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    GameUiBridge.NO_OP, true);
            loaded.setPlatformResourceProvider(RES);
            loaded.rebuildScheduleIfNeeded();
            loaded.refreshAfterLoad();
            return loaded;
        } finally {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    /** The gate league with the pool built and the early wave run. */
    private static League openBoard() throws Exception {
        League l = load(gateSave);
        l.recruitPlayers();
        assertNotNull(l.getRecruitPool());
        return l;
    }

    private static RecruitingSessionData session(League l) {
        return SimulationFacade.prepareRecruitingSessionFromPayload(SimulationFacade.buildRecruitingPayload(l.userTeam));
    }

    private static List<String> poolKeys(RecruitPool pool) {
        List<String> out = new ArrayList<>();
        for (RecruitPool.Prospect p : pool.prospects()) {
            out.add(p.key + "@" + (p.committedTo() == null ? "-" : p.committedTo().getName())
                    + "/" + (p.rival() == null ? "-" : p.rival().getName()) + "/" + p.decisionPoint());
        }
        return out;
    }

    private static boolean onRoster(Team t, Player p) {
        for (Player x : t.getAllPlayers()) {
            if (x == p) return true;
        }
        return false;
    }

    private static boolean onRosterByKey(Team t, String key) {
        for (Player x : t.getAllPlayers()) {
            if (RecruitPool.keyOf(Persistence.toCsv(x.toRecord())).equals(key)) return true;
        }
        return false;
    }

    // ---- generation ---------------------------------------------------------

    @Test
    public void pool_isDeterministic_andBuildingItNeverMovesTheSeasonStream() throws Exception {
        League a = openBoard();
        String rngA = SimRandom.exportState();
        League b = openBoard();
        assertEquals("same save, same pool and commitments", poolKeys(a.getRecruitPool()), poolKeys(b.getRecruitPool()));
        assertEquals("same stream position after the early wave", rngA, SimRandom.exportState());

        String before = SimRandom.exportState();
        RecruitPool p1 = RecruitPool.generate(b, false);
        RecruitPool p2 = RecruitPool.generate(b, false);
        assertEquals("a board-only pool draws nothing from the season's stream", before, SimRandom.exportState());
        assertEquals(poolKeys(p1), poolKeys(p2));
    }

    @Test
    public void pool_coversEveryNeedWithSurplus_andHasARealisticStarSpread() throws Exception {
        League l = openBoard();
        RecruitPool pool = l.getRecruitPool();
        int[] perPos = new int[Team.RECRUIT_POSITIONS.length];
        int[] wanted = new int[perPos.length];
        int[] grades = new int[6];
        for (RecruitPool.Prospect p : pool.prospects()) {
            int i = Team.recruitPositionIndex(p.player().position);
            perPos[i]++;
            if (p.committedTo() != null) wanted[i]++; // early commits came out of the needs
            grades[Math.max(0, Math.min(5, p.player().recruitRating))]++;
        }
        for (Team t : l.getTeamList()) {
            int[] n = pool.needs.get(t);
            if (n == null) continue;
            for (int i = 0; i < n.length; i++) wanted[i] += n[i];
        }
        int programs = l.getTeamList().size();
        assertTrue("about a class per program: " + pool.prospects().size(), pool.prospects().size() > programs * 15);
        for (int i = 0; i < perPos.length; i++) {
            assertTrue(Team.RECRUIT_POSITIONS[i] + ": " + perPos[i] + " prospects for " + wanted[i] + " CPU spots",
                    perPos[i] >= wanted[i] * 1.25);
        }
        // Few elite prospects, many more at the bottom (grade 5 = five stars on the board).
        assertTrue("five-star prospects exist " + java.util.Arrays.toString(grades), grades[5] > 0);
        assertTrue("five-stars are rare " + java.util.Arrays.toString(grades),
                grades[5] < pool.prospects().size() / 8);
        assertTrue("a wide spread of grades " + java.util.Arrays.toString(grades),
                grades[1] + grades[2] > grades[4] + grades[5]);
    }

    // ---- interest -----------------------------------------------------------

    @Test
    public void interest_risesWithPrestigeNeedAndPlayingTime() {
        double base = RecruitPool.interest(50, 1, 70, 72, 2, 2, 70, 3, 0.5);
        assertTrue("more prestige", RecruitPool.interest(80, 1, 70, 72, 2, 2, 70, 3, 0.5) > base);
        assertTrue("less prestige", RecruitPool.interest(20, 1, 70, 72, 2, 2, 70, 3, 0.5) < base);
        assertTrue("an open spot beats none", base > RecruitPool.interest(50, 0, 70, 72, 2, 2, 70, 3, 0.5));
        assertTrue("more open spots", RecruitPool.interest(50, 2, 70, 72, 2, 2, 70, 3, 0.5) > base);
        assertTrue("weaker rotation = more playing time", RecruitPool.interest(50, 1, 70, 60, 2, 2, 70, 3, 0.5) > base);
        assertTrue("closer to home", base > RecruitPool.interest(50, 1, 70, 72, 0, 4, 70, 3, 0.5));
        assertTrue("better recruiter", RecruitPool.interest(50, 1, 70, 72, 2, 2, 90, 3, 0.5) > base);
        assertTrue("richer collective", RecruitPool.interest(50, 1, 70, 72, 2, 2, 70, 9, 0.5) > base);
        for (int pres = 0; pres < 100; pres += 5) {
            for (int need = 0; need < 4; need++) {
                double x = RecruitPool.interest(pres, need, 65, 70, 1, 3, 60, 2, 0.3);
                assertTrue(RecruitPool.interest(pres + 5, need, 65, 70, 1, 3, 60, 2, 0.3) >= x);
                assertTrue(RecruitPool.interest(pres, need + 1, 65, 70, 1, 3, 60, 2, 0.3) >= x);
            }
        }
    }

    // ---- waves ----------------------------------------------------------------

    @Test
    public void waves_respectNeedsMinimumsAndTheCap() throws Exception {
        League l = openBoard();
        RecruitPool pool = l.getRecruitPool();
        IdentityHashMap<Team, int[]> earlyCount = new IdentityHashMap<>();
        for (RecruitPool.Prospect p : pool.prospects()) {
            if (p.committedTo() == null) continue;
            assertTrue("early commits are early", p.isEarlyCommit());
            assertFalse("the user never signs in the early wave", p.committedTo().isUserControlled());
            assertTrue("an early commit is on his program's roster", onRoster(p.committedTo(), p.player()));
            earlyCount.computeIfAbsent(p.committedTo(), t -> new int[10])[Team.recruitPositionIndex(p.player().position)]++;
        }
        assertTrue("an early wave happened", !earlyCount.isEmpty());
        for (Team t : earlyCount.keySet()) {
            int[] left = pool.needs.get(t);
            int[] early = earlyCount.get(t);
            for (int i = 0; i < 10; i++) {
                assertTrue(t.getName() + " " + Team.RECRUIT_POSITIONS[i] + " early " + early[i] + " of " + (early[i] + left[i]),
                        early[i] <= Math.ceil((early[i] + left[i]) * RecruitPool.EARLY_SHARE));
            }
        }

        l.applyRecruitingSignings("");
        assertEquals(RecruitPool.Stage.CLOSED, pool.stage());
        Set<Player> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Team t : l.getTeamList()) {
            for (Player p : t.getAllPlayers()) assertTrue("one roster per player", seen.add(p));
            if (t.isUserControlled()) continue;
            assertArrayEquals(t.getName() + " needs filled", new int[10], pool.needs.get(t));
            assertTrue(t.getName() + " at the roster minimum", t.getAllPlayers().size() >= RosterRules.MIN_PLAYERS);
            for (String pos : Team.RECRUIT_POSITIONS) {
                assertTrue(t.getName() + " " + pos + " minimum", countAt(t, pos) >= RosterRules.minForPosition(pos));
            }
            int newKs = 0;
            int youngQbs = 0;
            for (Player p : t.getTeamKs()) if (p.year == 1 && !p.wasRedshirt && !p.isWalkOn) newKs++;
            for (Player p : t.getTeamQBs()) if (!p.isTransfer && p.year <= 2) youngQbs++;
            assertTrue(t.getName() + " kicker hoard: " + newKs, newKs <= 2);
            assertTrue(t.getName() + " QB pipeline: " + youngQbs, youngQbs >= 2);
        }
        l.startNextSeason();
        for (Team t : l.getTeamList()) {
            if (!t.isUserControlled()) {
                assertTrue(t.getName() + " over the cap", t.getAllPlayers().size() <= RosterRules.MAX_PLAYERS);
            }
        }
        assertNull("the pool is dropped with the new season", l.getRecruitPool());
    }

    private static int countAt(Team t, String pos) {
        int n = 0;
        for (Player p : t.getAllPlayers()) if (p.position.equals(pos)) n++;
        return n;
    }

    // ---- the user -------------------------------------------------------------

    @Test
    public void userLosesAContestedProspectInTheEarlyWave() throws Exception {
        League l = openBoard();
        RecruitingSessionData s = session(l);
        assertTrue(s.isSharedPool());
        RecruitingPlayerRecord gone = null;
        for (RecruitingPlayerRecord r : s.availAll) {
            if (s.isCommittedElsewhere(r)) {
                gone = r;
                break;
            }
        }
        assertNotNull("rivals signed some of the user's board early", gone);
        assertTrue(RecruitingPresentation.statusLine(s, gone).startsWith("Committed to "));
        String why = s.signingBlockReason(gone);
        assertNotNull(why);
        assertTrue(why, why.startsWith("Committed to "));
        s.recruitingBudget = 100000;
        try {
            s.recruitPlayer(gone, false, 0);
            fail("signed a prospect committed elsewhere");
        } catch (IllegalStateException expected) {
            // blocked
        }
        // A forged signing of him is dropped by the engine too: nobody ends up on two rosters.
        String forged = gone.raw() + "%\nEND_RECRUITS%\n";
        String key = RecruitPool.keyOf(gone.raw());
        Team rival = null;
        for (RecruitPool.Prospect p : l.getRecruitPool().prospects()) {
            if (p.key.equals(key)) rival = p.committedTo();
        }
        assertNotNull(rival);
        l.applyRecruitingSignings(forged);
        assertFalse(onRosterByKey(l.userTeam, key));
        assertTrue(onRosterByKey(rival, key));
    }

    @Test
    public void contestedProspect_commitsToHisRivalAtHisDecisionPoint() throws Exception {
        League l = openBoard();
        RecruitingSessionData s = session(l);
        s.recruitingBudget = 1_000_000;
        RecruitingPlayerRecord target = null;
        for (RecruitingPlayerRecord r : s.availAll) {
            if (s.signingsUntilDecision(r) == 1) {
                target = r;
                break;
            }
        }
        assertNotNull("a prospect who decides on the user's next signing", target);
        String rival = s.rivalFor(target);
        assertNotNull(rival);
        assertTrue(RecruitingPresentation.statusLine(s, target), RecruitingPresentation.statusLine(s, target).contains(rival));
        RecruitingPlayerRecord other = null;
        for (RecruitingPlayerRecord r : s.availAll) {
            if (r != target && !s.isCommittedElsewhere(r) && s.signingBlockReason(r) == null) {
                other = r;
                break;
            }
        }
        assertNotNull(other);
        s.recruitPlayer(other, false, 0);
        assertEquals("the user waited: he signed with his rival", rival, s.committedTo(target));
        assertTrue(s.lastDecisions().contains(target));
        assertTrue(RecruitingPresentation.buildLostProspectsNotice(s).contains(target.name()));

        l.applyRecruitingSignings(s.buildRecruitsSaveData());
        assertTrue("on his rival's roster", onRosterByKey(l.findTeam(rival), RecruitPool.keyOf(target.raw())));
        assertTrue("the user's signing stands", onRosterByKey(l.userTeam, RecruitPool.keyOf(other.raw())));
    }

    @Test
    public void userSigning_isImmediateAndReachesTheRoster() throws Exception {
        League l = openBoard();
        RecruitingSessionData s = session(l);
        s.recruitingBudget = 1_000_000;
        List<RecruitingPlayerRecord> signed = new ArrayList<>();
        for (RecruitingPlayerRecord r : new ArrayList<>(s.availAll)) {
            if (signed.size() == 4) break;
            if (!s.isCommittedElsewhere(r) && s.signingBlockReason(r) == null) {
                s.recruitPlayer(r, false, 0);
                signed.add(r);
                assertTrue("immediate", s.playersRecruited.contains(r));
            }
        }
        assertEquals(4, signed.size());
        int rosterBefore = l.userTeam.getAllPlayers().size();
        l.applyRecruitingSignings(s.buildRecruitsSaveData());
        for (RecruitingPlayerRecord r : signed) {
            String key = RecruitPool.keyOf(r.raw());
            assertTrue(r.name() + " on the user's roster", onRosterByKey(l.userTeam, key));
            for (Team t : l.getTeamList()) {
                if (t != l.userTeam) assertFalse(r.name() + " also at " + t.getName(), onRosterByKey(t, key));
            }
        }
        assertTrue(l.userTeam.getAllPlayers().size() >= rosterBefore + 4);
    }

    // ---- saves ---------------------------------------------------------------

    @Test
    public void midRecruitingSave_reloadsTheSameBoardAndPlaysOnIdentically() throws Exception {
        League a = openBoard();
        RecruitingSessionData s = session(a);
        s.recruitingBudget = 1_000_000;
        int n = 0;
        for (RecruitingPlayerRecord r : new ArrayList<>(s.availAll)) {
            if (n == 3) break;
            if (!s.isCommittedElsewhere(r) && s.signingBlockReason(r) == null) {
                s.recruitPlayer(r, false, 0);
                n++;
            }
        }
        String data = s.buildRecruitsSaveData();
        String payloadA = SimulationFacade.buildRecruitingPayload(a.userTeam);
        byte[] mid = a.serializeSave();
        assertTrue(new String(mid, StandardCharsets.UTF_8).contains("LL:" + RecruitPool.LIST_INFO));

        finishAndPlay(a, data);
        String expected = SaveDeterminismTest.fingerprint(a);

        // Loading puts the season's stream back where the save left it.
        League b = load(mid);
        assertEquals("the same board after a reload", payloadA, SimulationFacade.buildRecruitingPayload(b.userTeam));
        assertNotNull(b.getRecruitPool());
        assertTrue(b.recruitingPhaseActive);
        RecruitingSessionData resumed = session(b);
        assertEquals("the same open and committed prospects", s.committedElsewhereCount() - lostDuring(s),
                resumed.committedElsewhereCount());
        finishAndPlay(b, data);
        assertEquals(expected, SaveDeterminismTest.fingerprint(b));
    }

    /** Board prospects that committed to rivals during the session (a fresh board shows them open). */
    private static int lostDuring(RecruitingSessionData s) {
        int n = 0;
        for (RecruitingPlayerRecord r : s.lostToRivals()) {
            if (s.availAll.contains(r)) n++;
        }
        return n;
    }

    private static void finishAndPlay(League l, String data) {
        l.recruitPlayers(); // a host resuming the gate: must not run the early wave again
        l.finishRecruitingSeason(data);
        SeasonController c = new SeasonController(l, GameUiBridge.NO_OP);
        for (int i = 0; i < 3; i++) c.advanceWeek();
    }

    @Test
    public void legacySaveAtTheGate_getsAPoolOnLoad() throws Exception {
        // A v1.5d dynasty saved at the gate after the old CPU recruiting pass.
        League old = load(gateSave);
        for (Team t : old.getTeamList()) t.CPUrecruiting();
        String text = new String(old.serializeSave(), StandardCharsets.UTF_8);
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.startsWith("LL:" + RecruitPool.LIST_INFO) || line.startsWith("END_SAVE\t")) continue;
            lines.add(line.startsWith("V:") ? "V:" + SaveSchema.V1_5D : line);
        }
        lines.add("END_SAVE\t" + lines.size());
        byte[] legacy = (String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8);

        League l = load(legacy);
        assertTrue(l.recruitingPhaseActive);
        RecruitPool pool = l.getRecruitPool();
        assertNotNull("a pool is built on load for the user's board", pool);
        assertFalse("CPU classes were signed already", pool.isLive());
        RecruitingSessionData s = session(l);
        assertTrue(s.isSharedPool());
        assertEquals("nobody committed before the board opened", 0, s.committedElsewhereCount());
        assertFalse(s.availAll.isEmpty());

        IdentityHashMap<Team, Integer> sizes = new IdentityHashMap<>();
        for (Team t : l.getTeamList()) sizes.put(t, t.getAllPlayers().size());
        l.recruitPlayers(); // a host at the gate: no second CPU pass
        s.recruitingBudget = 1_000_000;
        RecruitingPlayerRecord pick = null;
        for (RecruitingPlayerRecord r : s.availAll) {
            if (s.signingBlockReason(r) == null) {
                pick = r;
                break;
            }
        }
        assertNotNull(pick);
        s.recruitPlayer(pick, false, 0);
        l.applyRecruitingSignings(s.buildRecruitsSaveData());
        for (Team t : l.getTeamList()) {
            if (!t.isUserControlled()) assertEquals(t.getName(), sizes.get(t), (Integer) t.getAllPlayers().size());
        }
        assertTrue(onRosterByKey(l.userTeam, RecruitPool.keyOf(pick.raw())));
    }

    // ---- reads ---------------------------------------------------------------

    @Test
    public void buildingTheBoard_changesNothing() throws Exception {
        League l = openBoard();
        byte[] before = l.serializeSave();
        String rng = SimRandom.exportState();
        String p1 = SimulationFacade.buildRecruitingPayload(l.userTeam);
        String p2 = SimulationFacade.buildRecruitingPayload(l.userTeam);
        RecruitingSessionData s = session(l);
        RecruitingPresentation.buildOverviewSummary(s);
        RecruitingPresentation.buildBoardStatus(s);
        assertEquals(p1, p2);
        assertEquals(rng, SimRandom.exportState());
        assertArrayEquals(before, l.serializeSave());
    }
}
