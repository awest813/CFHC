package simulation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import positions.Player;
import positions.PlayerQB;
import positions.PlayerRB;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Player development: potential, mentors, training camp, weekly practice, and how growth is shown. */
public class PlayerDevelopmentTest {

    private final FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
    private League league;
    private Team team;
    private ArrayList<PlayerQB> savedQBs;

    @Before
    public void setUp() {
        SimRandom.pinNextSeed(20261001L);
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
        savedQBs = new ArrayList<>(team.teamQBs);
    }

    @After
    public void tearDown() {
        team.teamQBs.clear();
        team.teamQBs.addAll(savedQBs);
    }

    @Test
    public void potentialSetsHowFastAPlayerDevelops() {
        PlayerQB qb = new PlayerQB("Pot QB", 1, 3, team);
        qb.ratPot = 74;
        assertEquals(1.0, qb.potentialGrowthFactor(), 1e-9);
        qb.ratPot = 95;
        assertTrue(qb.potentialGrowthFactor() > 1.3);
        qb.ratPot = 50;
        assertTrue(qb.potentialGrowthFactor() < 0.65);
        qb.ratPot = 0;
        assertEquals(0.5, qb.potentialGrowthFactor(), 1e-9);
        qb.ratPot = 200;
        assertEquals(1.5, qb.potentialGrowthFactor(), 1e-9);
    }

    @Test
    public void highPotentialProspectsOutgrowLowPotentialOnes() {
        double high = averageOffseasonGrowth(95);
        double low = averageOffseasonGrowth(50);
        // Same player, coaches and playing time; only potential differs. The yearly
        // jump used to be flat, so the gap was a fraction of this.
        assertTrue("pot 95 grew " + high + ", pot 50 grew " + low, high > low * 2);
    }

    private double averageOffseasonGrowth(int pot) {
        PlayerQB qb = new PlayerQB("Growth QB", 1, 3, team);
        int runs = 600;
        long total = 0;
        for (int i = 0; i < runs; i++) {
            qb.year = 1;
            qb.ratPot = pot;
            qb.character = 70;
            qb.ratAttr1 = 55; qb.ratAttr2 = 55; qb.ratAttr3 = 55; qb.ratAttr4 = 55;
            qb.ratOvr = qb.getOverall();
            qb.genericAdvanceSeason();
            total += qb.ratAttr1 + qb.ratAttr2 + qb.ratAttr3 + qb.ratAttr4 - 4 * 55;
        }
        return total / (double) runs;
    }

    @Test
    public void mentorsTakeTheYoungestHighestPotentialPlayers() {
        team.teamQBs.clear();
        PlayerQB vet = qb("Vet QB", 4, 70);
        vet.ratOvr = 90;
        vet.character = 85;
        PlayerQB junior = qb("Junior QB", 3, 80);
        PlayerQB soph = qb("Soph QB", 2, 95);
        PlayerQB frLow = qb("Fr Low QB", 1, 55);
        PlayerQB frHigh = qb("Fr High QB", 1, 90);
        frLow.mentorName = "Last Year's Mentor";

        team.assignMentors();

        assertEquals("Vet QB", frHigh.mentorName);
        assertEquals("Vet QB", frLow.mentorName);
        assertFalse("a mentor takes two players", soph.hasMentor());
        assertFalse("upperclassmen are not mentored", junior.hasMentor());
        assertFalse(vet.hasMentor());
    }

    @Test
    public void mentorsArePairedWithUnderclassmenAtTheStartOfEachSeason() {
        assertTrue("a new league pairs mentors", menteeCount() > 0);
        league.userTeam = team;
        team.setupUserCoach("Development Coach");
        team.setUserControlled(true);
        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                p.mentorName = "";
            }
        }

        league.startNextSeason();

        assertTrue("startNextSeason pairs mentors once the roster is set", menteeCount() > 0);
        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                if (p.hasMentor()) assertTrue(p.name + " year " + p.year, p.year <= Team.MENTEE_MAX_YEAR);
            }
        }
    }

    private int menteeCount() {
        int n = 0;
        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                if (p.hasMentor()) n++;
            }
        }
        return n;
    }

    @Test
    public void theUsersCampSinglesOutProspectsWithoutAPick() {
        team.setUserControlled(true);
        assertTrue(team.trainingCampFocusNames.isEmpty());

        String report = team.trainingCamp();

        assertTrue(report, report.contains("(FOCUS)"));
        assertFalse(Team.trainingCampFocusReport(report).isEmpty());
    }

    @Test
    public void campFocusReportListsOnlyTheFocusProspects() {
        String report = "QB A. Smith (FOCUS): +5 OVR\nRB B. Jones: +1 OVR\nWR C. Lee (FOCUS): +3 OVR\n";
        assertEquals("QB A. Smith +5 OVR, WR C. Lee +3 OVR.", Team.trainingCampFocusReport(report));
        assertEquals("", Team.trainingCampFocusReport("RB B. Jones: +1 OVR\n"));
        assertEquals("", Team.trainingCampFocusReport(""));
    }

    @Test
    public void weeklyPracticeBuildsTheFocusedTraits() {
        PlayerQB qb = new PlayerQB("Practice QB", 1, 3, team);
        resetRatings(qb);
        for (int week = 0; week < 200; week++) {
            qb.applyWeeklyPractice(PracticeFocus.FUNDAMENTALS, PracticeFocus.PositionGroup.ALL,
                    PracticeFocus.FocusIntensity.NORMAL);
        }
        int gained = qb.ratAttr1 - 40;
        assertTrue("200 weeks of fundamentals: +" + gained, gained >= 4 && gained <= 24);
        assertTrue(qb.ratAttr2 > 40);
        assertEquals(40, qb.ratAttr3);
        assertEquals(40, qb.ratAttr4);
        assertEquals(40, qb.ratIntelligence);

        resetRatings(qb);
        for (int week = 0; week < 100; week++) {
            qb.applyWeeklyPractice(PracticeFocus.BALANCED, PracticeFocus.PositionGroup.ALL,
                    PracticeFocus.FocusIntensity.INTENSE);
        }
        assertEquals(40, qb.ratAttr1);
        assertEquals(40, qb.ratIntelligence);
    }

    @Test
    public void intensePracticeGrowsFasterThanNormal() {
        PlayerQB qb = new PlayerQB("Intense QB", 1, 3, team);
        int normal = practiceGain(qb, PracticeFocus.FocusIntensity.NORMAL);
        int intense = practiceGain(qb, PracticeFocus.FocusIntensity.INTENSE);
        assertTrue("normal +" + normal + ", intense +" + intense, intense > normal);
    }

    private static int practiceGain(PlayerQB qb, PracticeFocus.FocusIntensity intensity) {
        int total = 0;
        for (int season = 0; season < 40; season++) {
            resetRatings(qb);
            for (int week = 0; week < 25; week++) {
                qb.applyWeeklyPractice(PracticeFocus.ATHLETICISM, PracticeFocus.PositionGroup.QB, intensity);
            }
            total += qb.ratAttr3 + qb.ratAttr4 - 80;
        }
        return total;
    }

    private static void resetRatings(PlayerQB qb) {
        qb.ratAttr1 = 40; qb.ratAttr2 = 40; qb.ratAttr3 = 40; qb.ratAttr4 = 40;
        qb.ratIntelligence = 40;
    }

    @Test
    public void intensePracticeHurtsAboutOnePlayerInAHundredEachWeek() {
        team.setUserControlled(true);
        team.practiceFocus = PracticeFocus.FUNDAMENTALS;
        league.currentWeek = 1;

        team.focusIntensity = PracticeFocus.FocusIntensity.INTENSE;
        double intenseRate = practiceInjuryRate(40);
        assertTrue("intense practice injury rate " + intenseRate, intenseRate > 0.004 && intenseRate < 0.02);

        team.focusIntensity = PracticeFocus.FocusIntensity.NORMAL;
        assertEquals(0.0, practiceInjuryRate(10), 0.0);
    }

    private double practiceInjuryRate(int weeks) {
        int hurt = 0;
        int players = 0;
        for (int week = 0; week < weeks; week++) {
            for (Player p : team.getAllPlayers()) {
                p.injury = null;
                p.isInjured = false;
            }
            team.applyWeeklyPractice();
            for (Player p : team.getAllPlayers()) {
                players++;
                if (p.injury != null) hurt++;
            }
        }
        return hurt / (double) players;
    }

    @Test
    public void lastYearPlayersProjectAtTheirCurrentRating() {
        league.showPotential = true;
        PlayerQB qb = new PlayerQB("Senior QB", 4, 3, team);
        qb.ratOvr = 80;
        qb.ratPot = 90;
        assertEquals(80, qb.getPotRating(80));
        qb.year = 5;
        assertEquals("a fifth-year player used to project below their rating", 80, qb.getPotRating(80));
        qb.year = 1;
        assertTrue(qb.getPotRating(80) > 80);
    }

    @Test
    public void lineupRowsShowPotentialOnlyWhenTheLeagueDoes() {
        PlayerRB rb = new PlayerRB("Row RB", 1, 3, team);
        rb.ratOvr = 77;

        league.showPotential = false;
        assertTrue(rb.getInfoForLineup(), rb.getInfoForLineup().contains("] 77 ("));
        assertFalse(rb.getInfoLineupInjury().contains("Pot:"));

        league.showPotential = true;
        int talent = team.getHeadCoach() != null ? team.getHeadCoach().ratTalent : 0;
        assertTrue(rb.getInfoForLineup(), rb.getInfoForLineup().contains("] 77/" + rb.getPotRating(talent) + " ("));
        assertTrue(rb.getInfoLineupInjury().contains("Pot:"));
    }

    @Test
    public void ratingChangesShowOnceGrowthHasRun() {
        int r = league.regSeasonWeeks;
        league.currentWeek = SeasonFlowOrder.midseasonWeek(r);
        assertFalse("mid-season growth has not run yet", team.showsPlayerRatingChanges());
        league.currentWeek = SeasonFlowOrder.midseasonWeek(r) + 1;
        assertTrue(team.showsPlayerRatingChanges());
        league.currentWeek = SeasonFlowOrder.graduationWeek(r);
        assertFalse("offseason growth has not run yet", team.showsPlayerRatingChanges());
        league.currentWeek = SeasonFlowOrder.graduationWeek(r) + 1;
        assertTrue(team.showsPlayerRatingChanges());

        PlayerQB qb = new PlayerQB("Improved QB", 2, 3, team);
        qb.ratImprovement = 3;
        assertEquals(" (+3)", team.getRatImprovement(qb));
    }

    private PlayerQB qb(String name, int year, int pot) {
        PlayerQB p = new PlayerQB(name, year, 3, team);
        p.year = year;
        p.ratPot = pot;
        p.ratOvr = 70;
        p.character = 60;
        p.mentorName = "";
        team.teamQBs.add(p);
        return p;
    }
}
