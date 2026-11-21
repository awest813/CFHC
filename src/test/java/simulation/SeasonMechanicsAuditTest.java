package simulation;

import org.junit.Before;
import org.junit.Test;
import positions.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Season/career mechanics found by the multi-season audit: odd-conference
 * scheduling, the career-mode firing gate, eager and single-counted awards,
 * and the CPU roster cap.
 */
public class SeasonMechanicsAuditTest {

    private League league;

    @Before
    public void setUp() {
        SimRandom.bind(12345L);
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        league.userTeam.getHeadCoach().user = true;
    }

    // ---------------------------------------------------------------- schedule

    @Test
    public void roundRobin_everySize_distinctOpponentsAndBalancedHomeAway() {
        for (int n = 4; n <= 18; n++) {
            int full = (n % 2 == 0) ? n - 1 : n;
            int rounds = Math.min(8, full);
            List<List<int[]>> rr = Conference.roundRobinRounds(n, rounds);
            assertEquals(rounds, rr.size());
            Set<Long> pairs = new HashSet<>();
            int[] home = new int[n];
            int[] games = new int[n];
            for (List<int[]> round : rr) {
                boolean[] seen = new boolean[n];
                for (int[] g : round) {
                    assertFalse("n=" + n + " team plays twice in a round", seen[g[0]] || seen[g[1]]);
                    seen[g[0]] = seen[g[1]] = true;
                    long key = Math.min(g[0], g[1]) * 100L + Math.max(g[0], g[1]);
                    assertTrue("n=" + n + " rematch " + g[0] + "-" + g[1], pairs.add(key));
                    home[g[0]]++;
                    games[g[0]]++;
                    games[g[1]]++;
                }
                assertEquals("n=" + n + " games per round", n / 2, round.size());
            }
            for (int t = 0; t < n; t++) {
                int away = games[t] - home[t];
                assertTrue("n=" + n + " team " + t + " home " + home[t] + " away " + away,
                        Math.abs(home[t] - away) <= 2);
                assertTrue("n=" + n + " team " + t + " plays " + games[t] + " of " + rounds,
                        games[t] >= rounds - 1 && games[t] <= rounds);
            }
        }
    }

    /** Scheduling is random; check the conference-game cap holds across seeds. */
    @Test
    public void conferenceGameCount_neverExceedsIntended_acrossSeeds() {
        FileSystemResourceProvider r = new FileSystemResourceProvider(System.getProperty("user.dir"));
        for (long seed = 1; seed <= 40; seed++) {
            SimRandom.pinNextSeed(seed);
            League l = new League(
                    r.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    r.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    r.getString(PlatformResourceProvider.KEY_CONFERENCES),
                    r.getString(PlatformResourceProvider.KEY_TEAMS),
                    r.getString(PlatformResourceProvider.KEY_BOWLS),
                    false, false);
            for (Conference c : l.getConferences()) {
                if (c.confTeams.size() < c.minConfTeams) continue;
                int confWeeks = League.REGULAR_SEASON_GAMES - c.getOOCGames();
                for (Team t : c.confTeams) {
                    int conf = 0;
                    for (Game g : t.getGameSchedule()) {
                        if (g.isByeWeek()) continue;
                        Team opp = g.homeTeam == t ? g.awayTeam : g.homeTeam;
                        if (opp.getConference().equals(t.getConference())) conf++;
                    }
                    assertTrue("seed " + seed + " " + t.getName() + " conference games " + conf,
                            conf <= confWeeks);
                }
            }
        }
    }

    @Test
    public void leagueSchedule_twelveRealGames_noRematches_conferenceCountsAsIntended() {
        Set<String> oddChecked = new HashSet<>();
        for (Conference c : league.getConferences()) {
            if (c.confTeams.size() < c.minConfTeams) continue;
            int confWeeks = League.REGULAR_SEASON_GAMES - c.getOOCGames();
            for (Team t : c.confTeams) {
                List<Game> sched = t.getGameSchedule();
                assertEquals(t.getName() + " slots", League.REGULAR_SEASON_GAMES, sched.size());
                Set<Team> opponents = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
                int real = 0;
                int conf = 0;
                for (Game g : sched) {
                    if (g.isByeWeek()) continue;
                    real++;
                    Team opp = g.homeTeam == t ? g.awayTeam : g.homeTeam;
                    if (opp.getConference().contains("FCS")) continue;
                    assertTrue(t.getName() + " plays " + opp.getName() + " twice", opponents.add(opp));
                    if (opp.getConference().equals(t.getConference())) conf++;
                }
                assertEquals(t.getName() + " real games", League.REGULAR_SEASON_GAMES, real);
                if (c.confTeams.size() % 2 == 0) {
                    assertEquals(t.getName() + " conference games", confWeeks, conf);
                } else {
                    // One team in each round sits out and plays an OOC game instead.
                    assertTrue(t.getName() + " conference games " + conf,
                            conf == confWeeks || conf == confWeeks - 1);
                    oddChecked.add(c.confName);
                }
            }
        }
        assertFalse("bundled data should include an odd-sized conference", oddChecked.isEmpty());
    }

    // ---------------------------------------------------------------- firing

    @Test
    public void userIsNeverFired_withoutCareerMode_butCanBeWithIt() {
        Team user = league.userTeam;
        league.careerMode = false;
        for (int i = 0; i < 40; i++) {
            primeForFiring(user);
            user.coachContracts(-10);
            assertFalse("user fired with career mode off", user.fired);
        }
        league.careerMode = true;
        boolean firedOnce = false;
        for (int i = 0; i < 40 && !firedOnce; i++) {
            primeForFiring(user);
            user.coachContracts(-10);
            firedOnce = user.fired;
        }
        assertTrue("a struggling user coach can be fired in career mode", firedOnce);
    }

    private static void primeForFiring(Team t) {
        t.fired = false;
        t.newContract = false;
        t.retired = false;
        t.getHeadCoach().contractLength = 3;
        t.getHeadCoach().contractYear = 3; // contract up for review
        t.rankTeamPollScore = 90;
        t.rankTeamPrestige = 90;
        t.teamPrestigeStart = t.teamPrestige;
        t.natChampWL = "";
    }

    // ---------------------------------------------------------------- awards

    @Test
    public void awards_decidedByEngine_readersIdempotent_countedOnce() {
        SeasonController controller = new SeasonController(league, noOpBridge());
        int ccgWeek = SeasonFlowOrder.conferenceChampionshipWeek(league.regSeasonWeeks);
        while (league.currentWeek < ccgWeek) controller.advanceWeek();

        Map<Player, Integer> potyBefore = new IdentityHashMap<>();
        for (Team t : league.getTeamList()) for (Player p : t.getAllPlayers()) potyBefore.put(p, p.getHeismans());
        assertFalse(league.seasonAwardsDecided);

        controller.advanceWeek(); // conference championships: the engine decides every award
        assertTrue(league.seasonAwardsDecided);
        Player poty = league.heisman;
        assertNotNull(poty);
        assertNotNull(league.defPOTY);
        assertNotNull(league.linemanPOTY);
        assertNotNull(league.coachWinner);
        assertTrue(poty.wonHeisman);

        String allAm = league.getAllAmericanStr();
        String coach = league.getCoachAwardStr();
        String allConf = league.getAllConfStr(0);
        assertTrue(allAm.length() > 50);
        assertEquals(allAm, league.getAllAmericanStr());
        assertEquals(coach, league.getCoachAwardStr());
        assertEquals(allConf, league.getAllConfStr(0));

        Map<Player, Integer> wins = new IdentityHashMap<>();
        for (Player p : new Player[]{league.heisman, league.defPOTY, league.linemanPOTY}) wins.merge(p, 1, Integer::sum);
        for (Map.Entry<Player, Integer> e : potyBefore.entrySet()) {
            int delta = e.getKey().getHeismans() - e.getValue();
            assertEquals(e.getKey().name + " POTY count", (int) wins.getOrDefault(e.getKey(), 0), delta);
        }

        controller.advanceWeek(); // a bowl week runs the weekly player spotlight again
        assertSame("winner must survive the weekly spotlight", poty, league.heisman);

        // Season advance counts team honors once and never re-counts POTY.
        Player allAmerican = null;
        for (Team t : league.getTeamList()) for (Player p : t.getAllPlayers()) if (p.wonAllAmerican && allAmerican == null) allAmerican = p;
        assertNotNull(allAmerican);
        int aa = allAmerican.getAllAmericans();
        int heis = poty.getHeismans();
        allAmerican.addSeasonAwards();
        poty.addSeasonAwards();
        assertEquals(aa + 1, allAmerican.getAllAmericans());
        assertEquals(heis, poty.getHeismans());
        allAmerican.addSeasonAwards(); // next season, no new honor
        assertEquals(aa + 1, allAmerican.getAllAmericans());
        assertFalse(allAmerican.wonAllAmerican);
    }

    @Test
    public void awardReaders_beforeDecision_doNotDecide() {
        String s = league.getDefensePOTYStr();
        assertNotNull(s);
        assertFalse(league.defPOTYDecided);
        assertFalse(league.seasonAwardsDecided);
        assertEquals("", league.getAllConfStr(0));
    }

    // ---------------------------------------------------------------- roster cap

    @Test
    public void cpuRosterCap_trimsTo75_keepsPositionMinimumsAndBestPlayers() {
        Team cpu = league.getTeamList().get(5);
        assertFalse(cpu.isUserControlled());
        cpu.recruitPlayersFreshman(2, 2, 6, 2, 4, 1, 3, 3, 3, 2);
        assertTrue("setup should overfill the roster", cpu.getAllPlayers().size() > RosterRules.MAX_PLAYERS);
        Map<String, Player> best = new HashMap<>();
        for (Player p : cpu.getAllPlayers()) {
            Player b = best.get(p.position);
            if (b == null || p.ratOvr > b.ratOvr) best.put(p.position, p);
        }

        cpu.enforceRosterCap();

        List<Player> after = new ArrayList<>(cpu.getAllPlayers());
        assertTrue("roster " + after.size(), after.size() <= RosterRules.MAX_PLAYERS);
        for (String pos : new String[]{"QB", "RB", "WR", "TE", "OL", "K", "DL", "LB", "CB", "S"}) {
            assertTrue(pos + " below minimum", cpu.getPositionList(pos).size() >= RosterRules.minForPosition(pos));
            assertTrue("best " + pos + " cut", after.contains(best.get(pos)));
        }
    }

    @Test
    public void userRoster_isNotTrimmed() {
        Team user = league.userTeam;
        user.recruitPlayersFreshman(2, 2, 6, 2, 4, 1, 3, 3, 3, 2);
        int size = user.getAllPlayers().size();
        assertEquals(0, user.enforceRosterCap());
        assertEquals(size, user.getAllPlayers().size());
    }

    // ---------------------------------------------------------------- misc

    @Test
    public void conferenceUsaAlias_resolvesAndUnknownIsMinusOne() {
        int cusa = league.getConfNumber("Conference USA");
        assertTrue(cusa >= 0);
        assertEquals(cusa, league.getConfNumber("CUSA"));
        assertEquals(-1, league.getConfNumber("No Such Conference"));
        for (Conference c : league.getConferences()) {
            for (Team t : c.confTeams) assertEquals(t.getName(), c.confName, t.getConference());
        }
    }

    @Test
    public void prestigeGain_compressedNearTop_neverAboveMax() {
        assertEquals(50, Team.applyPrestigeGain(45, 5));
        assertEquals(Team.PRESTIGE_SOFT_MAX, Team.applyPrestigeGain(Team.PRESTIGE_SOFT_MAX, 10));
        int p = 90;
        for (int i = 0; i < 20; i++) p = Team.applyPrestigeGain(p, 3);
        assertTrue("p=" + p, p <= Team.PRESTIGE_SOFT_MAX);
        assertEquals(0, Team.applyPrestigeGain(3, -10));
    }

    @Test
    public void budget_neverNegative() {
        Team t = league.getTeamList().get(3);
        t.setTeamBudget(100);
        t.setTeamBudget(t.getTeamBudget() - 5000);
        assertEquals(0, t.getTeamBudget());
        t.setTeamBudget(500000);
        league.applyOffseasonBudgetExpenses();
        assertTrue(t.getTeamBudget() < 500000);
        assertTrue(t.getTeamBudget() > League.budgetSoftCap(t.getTeamPrestige()));
    }

    private static GameUiBridge noOpBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int gamesA, int gamesB) {}
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
            @Override public void startRecruitingFlow() {}
        };
    }
}
