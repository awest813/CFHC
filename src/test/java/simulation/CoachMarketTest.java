package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import staff.DC;
import staff.HeadCoach;
import staff.OC;
import staff.Staff;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The offseason coaching market: carousel, rising stars, free agents, job moves. */
public class CoachMarketTest {

    private static final long SEED = 20260930L;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    private League league;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(SEED);
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setupUserCoach("Market Coach");
        league.userTeam.getHeadCoach().user = true;
        league.userTeam.setUserControlled(true);
        league.currentWeek = league.regSeasonWeeks + 7;
        league.coachList.clear();
        league.coachStarList.clear();
        league.coachFreeAgents.clear();
    }

    @Test
    public void carouselConsidersEveryFreeAgentAfterAHire() {
        List<Team> openings = cpuTeams(20);
        for (Team t : openings) {
            t.setHeadCoach(null);
        }
        Staff first = freeAgentHeadCoach(92);
        Staff second = freeAgentHeadCoach(91);

        league.coachCarousel();

        // Removing the first hire from the pool used to shift the second past the loop index.
        assertFalse("first free agent hired", league.coachFreeAgents.contains(first));
        assertFalse("second free agent hired", league.coachFreeAgents.contains(second));
        assertEquals(1, staffedBy(first.history).size());
        assertEquals(1, staffedBy(second.history).size());
    }

    @Test
    public void risingHeadCoachOnlyMovesUp() {
        List<Team> byPrestige = new ArrayList<>(cpuTeams(3));
        Team mid = byPrestige.get(1);
        Team better = byPrestige.get(0);
        Team worse = byPrestige.get(2);
        mid.teamPrestige = 60;
        better.teamPrestige = 70;
        worse.teamPrestige = 50;
        mid.confPrestige = 60;
        better.confPrestige = 60;
        worse.confPrestige = 60;
        HeadCoach star = mid.getHeadCoach();

        assertTrue(League.isRisingStarMove(star, better));
        assertFalse("same conference, less prestigious school", League.isRisingStarMove(star, worse));

        worse.confPrestige = 75;
        assertTrue("clearly stronger conference", League.isRisingStarMove(star, worse));
        worse.teamPrestige = 45;
        assertFalse("but not a far weaker school", League.isRisingStarMove(star, worse));

        OC coordinator = mid.getOC();
        worse.confPrestige = 40;
        assertTrue("any head job is a step up for a coordinator", League.isRisingStarMove(coordinator, worse));
    }

    @Test
    public void risingStarWhoLostHisJobIsNotHiredOnTopOfTheFiredCopy() {
        Team school = cpuTeams(1).get(0);
        OC star = school.getOC();
        league.coachStarList.add(star);
        // Fired in the same review that listed him: he is in the fired pool now.
        league.addCoach(new HeadCoach(star, school));
        school.setOC(null);
        assertFalse(League.holdsPost(star));

        for (Team t : cpuTeams(20)) {
            if (t != school) t.setHeadCoach(null);
        }
        league.coachCarousel();
        league.coordinatorCarousel();

        assertTrue("at most one staff job per coach", staffedBy(star.history).size() <= 1);
        assertNoCoachOnTwoStaffs();
    }

    @Test
    public void newHeadCoachKeepsADefensiveCoordinatorWhoFitsHisDefence() {
        Team school = cpuTeams(1).get(0);
        HeadCoach hc = school.getHeadCoach();
        for (int i = 0; i < 25; i++) {
            DC dc = school.getDC() != null ? school.getDC() : new DC(league.getRandName(), 6, 0, school);
            school.setDC(dc);
            school.getOC().offStrat = hc.offStrat;
            dc.defStrat = hc.defStrat;
            dc.offStrat = (hc.offStrat + 1) % 6;
            school.newCoachDecisions();
            assertSame("scheme fit decided by the defence, not the offence", dc, school.getDC());
        }
    }

    @Test
    public void replacementAssistantsMatchTheProgram() {
        List<Team> teams = new ArrayList<>(league.getTeamList());
        teams.sort((a, b) -> Integer.compare(b.teamPrestige, a.teamPrestige));
        Team top = teams.get(0);
        Team bottom = teams.get(teams.size() - 1);
        top.teamPrestige = 92;
        bottom.teamPrestige = 31;
        assertEquals("the level the league generated its staff at", 9, top.assistantCoachStars());
        assertEquals(3, bottom.assistantCoachStars());
        bottom.teamPrestige = 4;
        assertEquals(1, bottom.assistantCoachStars());
        bottom.teamPrestige = 31;

        top.setOC(null);
        bottom.setOC(null);
        league.OCCarousel();
        assertSame("fresh hire belongs to his school", top, top.getOC().team);
        assertEquals("fresh hire runs the head coach's scheme", top.getHeadCoach().offStrat, top.getOC().offStrat);
        assertSame(bottom, bottom.getOC().team);
    }

    @Test
    public void coordinatorSearchTakesAFreeAgentOnlyIfHeBeatsAFreshHire() {
        Team school = cpuTeams(1).get(0);
        int stars = school.assistantCoachStars();
        Staff weak = freeAgentCoordinator(school.getHeadCoach().offStrat, League.freshCoordinatorRating(stars) - 10);
        school.setOC(null);
        league.OCCarousel();
        assertTrue("weaker than a fresh hire: stays in the pool", league.coachFreeAgents.contains(weak));

        Staff strong = freeAgentCoordinator(school.getHeadCoach().offStrat, League.freshCoordinatorRating(stars) + 5);
        school.setOC(null);
        league.OCCarousel();
        assertFalse(league.coachFreeAgents.contains(strong));
        assertSame(strong.history, school.getOC().history);
    }

    @Test
    public void freeAgentPoolTakesUnplacedFiredCoachesAndRetiresTheOld() {
        Team school = cpuTeams(1).get(0);
        HeadCoach fired = new HeadCoach(school.getHeadCoach(), school);
        fired.stats[0] = 40;
        fired.age = 45;
        league.addCoach(fired);

        HeadCoach oldHeadCoach = freeAgentHeadCoach(70);
        oldHeadCoach.age = 79;
        Staff oldAssistant = freeAgentCoordinator(0, 70);
        oldAssistant.age = 79;

        league.advanceCoachFreeAgents();

        assertTrue(league.coachList.isEmpty());
        assertTrue("an unplaced fired coach joins the pool", league.coachFreeAgents.contains(fired));
        assertNull("pool members are unattached", fired.team);
        assertEquals(46, fired.age);
        assertTrue(oldHeadCoach.retired);
        assertTrue("former head coaches stay listed for the coach database", league.coachFreeAgents.contains(oldHeadCoach));
        assertFalse("career assistants drop out when they retire", league.coachFreeAgents.contains(oldAssistant));
    }

    @Test
    public void coordinatorTheUserReplacesJoinsThePool() {
        Team school = league.userTeam;
        OC replaced = school.getOC();
        league.coachStarList.add(replaced);

        league.releaseCoordinator(replaced, school);

        assertFalse(league.coachStarList.contains(replaced));
        assertEquals(1, league.coachFreeAgents.size());
        Staff pooled = league.coachFreeAgents.get(0);
        assertSame(replaced.history, pooled.history);
        assertNull(pooled.team);
        assertFalse(pooled.retired);
    }

    @Test
    public void hotSeatIsTheFinalContractYearBelowBaseline() {
        Team school = cpuTeams(1).get(0);
        HeadCoach hc = school.getHeadCoach();
        hc.contractLength = 4;
        hc.contractYear = 3;
        hc.baselinePrestige = school.teamPrestige + 4;
        assertTrue(League.onHotSeat(school));

        hc.baselinePrestige = school.teamPrestige - 4;
        assertFalse("raised the program", League.onHotSeat(school));

        hc.baselinePrestige = school.teamPrestige + 4;
        hc.contractYear = 1;
        assertFalse("years left on the deal", League.onHotSeat(school));
    }

    @Test
    public void takingAFilledJobReleasesTheSittingCoach() {
        Team target = cpuTeams(1).get(0);
        HeadCoach sitting = target.getHeadCoach();

        league.newJobtransfer(target.getName());

        assertSame(target, league.userTeam);
        assertTrue(target.isUserControlled());
        assertNull(target.getHeadCoach());
        assertEquals(1, league.coachList.size());
        assertSame(sitting.history, league.coachList.get(0).history);
    }

    @Test
    public void firedCoachIsNotOfferedHisOldJobWhenNothingIsOpen() {
        Team userTeam = league.userTeam;
        List<Team> offers = league.getCoachListFired(40, userTeam.getName());
        assertFalse(offers.isEmpty());
        for (Team t : offers) {
            assertFalse(t.getName().equals(userTeam.getName()));
        }
    }

    @Test
    public void promotionOffersAreMadeBeforeTheCarouselFillsTheOpenings() {
        List<Integer> offered = new ArrayList<>();
        List<Integer> open = new ArrayList<>();
        GameUiBridge bridge = new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() {}
            @Override public void showPromotionsDialog() {
                int vacancies = 0;
                for (Team t : league.getTeamList()) {
                    if (t.getHeadCoach() == null) vacancies++;
                }
                open.add(vacancies);
                offered.add(league.getCoachPromotionList(95, 2.0, league.userTeam.getName()).size());
            }
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void startRecruitingFlow() {}
        };
        league.currentWeek = 0;
        league.careerMode = true;
        SeasonController controller = new SeasonController(league, bridge);
        int guard = 0;
        while (offered.isEmpty() && !league.recruitingPhaseActive && guard++ < 80) {
            controller.advanceWeek();
        }
        if (league.userTeam.fired) {
            return; // job offers instead; covered elsewhere
        }
        assertEquals(1, offered.size());
        assertTrue("this offseason's openings are still open", open.get(0) > 0);
        assertEquals(open.get(0), offered.get(0));
    }

    @Test
    public void saveKeepsCoachHistoryAndTheFreeAgentPool() throws Exception {
        Team school = cpuTeams(1).get(0);
        school.getHeadCoach().history.add("2024: [HC] #12 " + school.getName() + " (9-3)>Bowl: W 24 - 21 vs Texas A&M #20");
        school.getHeadCoach().history.add("2025: [HC] #30 " + school.getName() + " (7-5)");
        school.getHeadCoach().history.add(""); // promotion placeholder
        school.getOC().history.add("2025: [OC] #12 " + school.getName() + " (9-3)");
        HeadCoach free = freeAgentHeadCoach(75);
        free.history.add("2024: [HC] #40 Somewhere (6-6)");

        File save = tmp.newFile();
        assertTrue(league.saveLeague(save));
        League loaded = new League(save,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                false);

        Team reloaded = null;
        for (Team t : loaded.getTeamList()) {
            if (t.getName().equals(school.getName())) reloaded = t;
        }
        assertNotNull(reloaded);
        assertEquals(school.getHeadCoach().history, reloaded.getHeadCoach().history);
        assertEquals(school.getOC().history, reloaded.getOC().history);
        assertEquals(1, loaded.getCoachFreeAgents().size());
        Staff back = loaded.getCoachFreeAgents().get(0);
        assertEquals(free.name, back.name);
        assertEquals(free.history, back.history);
        assertNull(back.team);
        assertTrue(League.hasHeadCoachingRecord(back));
    }

    // ---------------------------------------------------------------------

    /** CPU teams, most prestigious first. */
    private List<Team> cpuTeams(int n) {
        List<Team> teams = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            if (!t.isUserControlled() && t.getHeadCoach() != null && t.getOC() != null && t.getDC() != null) teams.add(t);
        }
        teams.sort((a, b) -> Integer.compare(b.teamPrestige, a.teamPrestige));
        return teams.subList(0, Math.min(n, teams.size()));
    }

    private HeadCoach freeAgentHeadCoach(int rating) {
        Team anchor = cpuTeams(1).get(0);
        HeadCoach c = new HeadCoach(league.getRandName(), 6, 50, anchor);
        c.ratOff = rating;
        c.ratDef = rating;
        c.ratTalent = rating;
        c.ratDiscipline = rating;
        c.stats[0] = 30;
        c.stats[1] = 20;
        league.addCoachFreeAgent(c);
        return c;
    }

    private Staff freeAgentCoordinator(int offStrat, int ratOff) {
        OC c = new OC(league.getRandName(), 6);
        c.offStrat = offStrat;
        c.ratOff = ratOff;
        c.ratDef = Math.min(c.ratDef, ratOff);
        league.addCoachFreeAgent(c);
        return c;
    }

    private List<Team> staffedBy(List<String> history) {
        List<Team> at = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            for (Staff s : new Staff[]{t.getHeadCoach(), t.getOC(), t.getDC()}) {
                if (s != null && s.history == history) at.add(t);
            }
        }
        return at;
    }

    private void assertNoCoachOnTwoStaffs() {
        Map<List<String>, String> seen = new IdentityHashMap<>();
        for (Team t : league.getTeamList()) {
            for (Staff s : new Staff[]{t.getHeadCoach(), t.getOC(), t.getDC()}) {
                if (s == null) continue;
                String prev = seen.put(s.history, s.position + "@" + t.getName());
                assertNull(s.name + " on two staffs: " + prev + " and " + s.position + "@" + t.getName(), prev);
            }
        }
    }
}
