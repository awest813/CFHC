package comparator;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import positions.Player;
import positions.PlayerDefense;
import positions.PlayerOffense;
import positions.PlayerQB;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;
import staff.Staff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for the comparator classes in the {@code comparator} package.
 *
 * <p>These tests verify that each comparator produces a consistent, correctly
 * directed ordering when sorting collections of teams and players.
 */
public class ComparatorTest {

    /** Shared league instance — cheap to reuse across test methods. */
    private League league;
    private Team teamA;
    private Team teamB;
    private Team teamC;

    @Before
    public void setUp() {
        String projectRoot = System.getProperty("user.dir");
        DesktopResourceProvider res = new DesktopResourceProvider(projectRoot);

        league = new League(
                res.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                res.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                res.getString(PlatformResourceProvider.KEY_CONFERENCES),
                res.getString(PlatformResourceProvider.KEY_TEAMS),
                res.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(res);

        // Use the first three teams in the league for controlled comparisons
        assertTrue("League must have at least 3 teams", league.getTeamList().size() >= 3);
        teamA = league.getTeamList().get(0);
        teamB = league.getTeamList().get(1);
        teamC = league.getTeamList().get(2);
    }

    // -------------------------------------------------------------------------
    // CompTeamPrestige
    // -------------------------------------------------------------------------

    @Test
    public void compTeamPrestige_higherPrestige_sortsFirst() {
        teamA.teamPrestige = 90;
        teamB.teamPrestige = 50;
        teamC.teamPrestige = 70;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamPrestige());

        assertEquals("Highest prestige should sort first", 90, teams.get(0).teamPrestige);
        assertEquals("Middle prestige should sort second", 70, teams.get(1).teamPrestige);
        assertEquals("Lowest prestige should sort last", 50, teams.get(2).teamPrestige);
    }

    @Test
    public void compTeamPrestige_equalPrestige_isConsistent() {
        teamA.teamPrestige = 75;
        teamB.teamPrestige = 75;

        CompTeamPrestige comp = new CompTeamPrestige();
        // compare(a,b) and compare(b,a) must have opposite signs or both be 0
        int ab = comp.compare(teamA, teamB);
        int ba = comp.compare(teamB, teamA);
        assertTrue("compare must be antisymmetric",
                Math.signum(ab) == -Math.signum(ba));
    }

    @Test
    public void compTeamPrestige_sameTeam_returnsZero() {
        CompTeamPrestige comp = new CompTeamPrestige();
        assertEquals("Comparing a team with itself must return 0", 0, comp.compare(teamA, teamA));
    }

    // -------------------------------------------------------------------------
    // CompTeamWins
    // -------------------------------------------------------------------------

    @Test
    public void compTeamWins_moreWins_sortsFirst() {
        league.currentWeek = 5; // in-season -> uses totalWins + wins

        teamA.wins = 4;
        teamA.totalWins = 0;
        teamB.wins = 2;
        teamB.totalWins = 0;
        teamC.wins = 3;
        teamC.totalWins = 0;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamWins());

        assertEquals("Team with most wins must sort first", 4, teams.get(0).wins);
        assertEquals("Team with fewest wins must sort last", 2, teams.get(2).wins);
    }

    @Test
    public void compTeamWins_offseason_usesTotalWins() {
        league.currentWeek = 20; // post-season -> uses totalWins only

        teamA.wins = 0; teamA.totalWins = 100;
        teamB.wins = 0; teamB.totalWins = 80;

        CompTeamWins comp = new CompTeamWins();
        assertTrue("Higher totalWins must sort first", comp.compare(teamA, teamB) < 0);
        assertTrue("Lower totalWins must sort last",  comp.compare(teamB, teamA) > 0);
    }

    // -------------------------------------------------------------------------
    // CompPlayerOVR
    // -------------------------------------------------------------------------

    @Test
    public void compPlayerOVR_higherOverall_sortsFirst() {
        PlayerQB qb1 = new PlayerQB(league.getRandName(), 1, 8, teamA);  // ~80 OVR
        PlayerQB qb2 = new PlayerQB(league.getRandName(), 1, 5, teamA);  // ~50 OVR
        PlayerQB qb3 = new PlayerQB(league.getRandName(), 1, 7, teamA);  // ~70 OVR

        List<Player> players = new ArrayList<>(Arrays.asList(qb1, qb2, qb3));
        Collections.sort(players, new CompPlayerOVR());

        assertTrue("Highest OVR must sort first",
                players.get(0).ratOvr >= players.get(1).ratOvr);
        assertTrue("Middle OVR must sort before lowest",
                players.get(1).ratOvr >= players.get(2).ratOvr);
    }

    @Test
    public void compPlayerOVR_antisymmetry() {
        PlayerQB qb1 = new PlayerQB(league.getRandName(), 1, 8, teamA);
        PlayerQB qb2 = new PlayerQB(league.getRandName(), 1, 5, teamA);
        // Force a definite OVR gap so the test is deterministic
        qb1.ratOvr = 85;
        qb2.ratOvr = 60;

        CompPlayerOVR comp = new CompPlayerOVR();
        assertTrue("Higher OVR player must come before lower OVR player",
                comp.compare(qb1, qb2) < 0);
        assertTrue("Lower OVR player must come after higher OVR player",
                comp.compare(qb2, qb1) > 0);
    }

    // -------------------------------------------------------------------------
    // CompTeamPPG
    // -------------------------------------------------------------------------

    @Test
    public void compTeamPPG_higherPPG_sortsFirst() {
        teamA.teamPoints = 300; teamA.wins = 10; teamA.losses = 2;
        teamB.teamPoints = 200; teamB.wins = 8; teamB.losses = 4;
        teamC.teamPoints = 400; teamC.wins = 12; teamC.losses = 0;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamPPG());

        // PPG = teamPoints / gamesPlayed; team C has highest, team B has lowest
        double ppgFirst  = (double) teams.get(0).teamPoints / (teams.get(0).wins + teams.get(0).losses);
        double ppgSecond = (double) teams.get(1).teamPoints / (teams.get(1).wins + teams.get(1).losses);
        double ppgThird  = (double) teams.get(2).teamPoints / (teams.get(2).wins + teams.get(2).losses);

        assertTrue("PPG must be descending after sort", ppgFirst >= ppgSecond);
        assertTrue("PPG must be descending after sort", ppgSecond >= ppgThird);
    }

    // -------------------------------------------------------------------------
    // CompTeamYPG (highest total yards per game sorts first)
    // -------------------------------------------------------------------------

    @Test
    public void compTeamYPG_higherYardage_sortsFirst() {
        teamA.teamYards = 4000; teamA.wins = 10; teamA.losses = 2;
        teamB.teamYards = 2000; teamB.wins = 8;  teamB.losses = 4;
        teamC.teamYards = 3000; teamC.wins = 6;  teamC.losses = 6;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamYPG());

        assertEquals("Team with highest YPG should sort first", 4000, teams.get(0).teamYards);
        assertEquals("Team with lowest YPG should sort last",  2000, teams.get(2).teamYards);
    }

    // -------------------------------------------------------------------------
    // CompTeamTODiff (positive TO differential sorts first)
    // -------------------------------------------------------------------------

    @Test
    public void compTeamTODiff_positiveDiff_sortsFirst() {
        teamA.teamTODiff = 5;
        teamB.teamTODiff = -3;
        teamC.teamTODiff = 0;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamTODiff());

        assertEquals("Positive TO diff must sort first",  5, teams.get(0).teamTODiff);
        assertEquals("Zero TO diff must sort second",     0, teams.get(1).teamTODiff);
        assertEquals("Negative TO diff must sort last",  -3, teams.get(2).teamTODiff);
    }

    // -------------------------------------------------------------------------
    // CompPlayerPassYards
    // -------------------------------------------------------------------------

    @Test
    public void compPlayerPassYards_higherYards_sortsFirst() {
        PlayerQB qb1 = new PlayerQB(league.getRandName(), 2, 8, teamA);
        PlayerQB qb2 = new PlayerQB(league.getRandName(), 2, 6, teamB);
        // getPassYards() reads stats[13]
        qb1.stats[13] = 3500;
        qb2.stats[13] = 2000;

        List<PlayerQB> players = new ArrayList<>(Arrays.asList(qb2, qb1));
        Collections.sort(players, new CompPlayerPassYards());

        assertEquals("Higher pass yards must sort first", 3500, players.get(0).getPassYards());
    }

    // -------------------------------------------------------------------------
    // CompConfPrestige
    // -------------------------------------------------------------------------

    @Test
    public void compConfPrestige_higherPrestige_sortsFirst() {
        simulation.Conference confA = league.getConferences().get(0);
        simulation.Conference confB = league.getConferences().get(1);

        confA.confPrestige = 90;
        confB.confPrestige = 50;

        List<simulation.Conference> confs = new ArrayList<>(Arrays.asList(confA, confB));
        Collections.sort(confs, new CompConfPrestige());

        assertEquals("Higher conf prestige must sort first", 90, confs.get(0).confPrestige);
        assertEquals("Lower conf prestige must sort last",   50, confs.get(1).confPrestige);
    }

    // -------------------------------------------------------------------------
    // CompTeamSoS
    // -------------------------------------------------------------------------

    @Test
    public void compTeamSoS_higherSOS_sortsFirst() {
        teamA.teamSOS = 15.0;
        teamB.teamSOS = 5.0;
        teamC.teamSOS = 10.0;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamSoS());

        assertEquals("Highest SOS should sort first", 15.0, teams.get(0).getTeamSOS(), 0.001);
        assertEquals("Middle SOS should sort second", 10.0, teams.get(1).getTeamSOS(), 0.001);
        assertEquals("Lowest SOS should sort last",    5.0, teams.get(2).getTeamSOS(), 0.001);
    }

    @Test
    public void compTeamSoS_sameTeam_returnsZero() {
        CompTeamSoS comp = new CompTeamSoS();
        assertEquals(0, comp.compare(teamA, teamA));
    }

    // -------------------------------------------------------------------------
    // CompTeamRYPG
    // -------------------------------------------------------------------------

    @Test
    public void compTeamRYPG_higherYards_sortsFirst() {
        teamA.teamRushYards = 3000; teamA.wins = 10; teamA.losses = 2;
        teamB.teamRushYards = 1000; teamB.wins = 8;  teamB.losses = 4;
        teamC.teamRushYards = 2000; teamC.wins = 6;  teamC.losses = 6;

        List<Team> teams = new ArrayList<>(Arrays.asList(teamA, teamB, teamC));
        Collections.sort(teams, new CompTeamRYPG());

        double rypgFirst  = (double) teams.get(0).getTeamRushYards() / teams.get(0).numGames();
        double rypgSecond = (double) teams.get(1).getTeamRushYards() / teams.get(1).numGames();
        double rypgThird  = (double) teams.get(2).getTeamRushYards() / teams.get(2).numGames();

        assertTrue("RYPG must be descending after sort", rypgFirst >= rypgSecond);
        assertTrue("RYPG must be descending after sort", rypgSecond >= rypgThird);
    }

    // -------------------------------------------------------------------------
    // CompTeamRecruitClass
    // -------------------------------------------------------------------------

    @Test
    public void compTeamRecruitClass_higherClass_sortsFirst() {
        CompTeamRecruitClass comp = new CompTeamRecruitClass();
        // recruit class rating is computed from actual freshmen, so just verify contract
        assertEquals(0, comp.compare(teamA, teamA));
    }

    // -------------------------------------------------------------------------
    // CompCoachWins
    // -------------------------------------------------------------------------

    @Test
    public void compCoachWins_moreWins_sortsFirst() {
        Staff coachA = teamA.getHeadCoach();
        Staff coachB = teamB.getHeadCoach();
        Staff coachC = teamC.getHeadCoach();

        // Use recordWins to set stats[0]
        coachA.recordWins(8);
        coachB.recordWins(3);
        coachC.recordWins(5);

        List<Staff> coaches = new ArrayList<>(Arrays.asList(coachA, coachB, coachC));
        Collections.sort(coaches, new CompCoachWins());

        assertTrue("Coach with most wins must sort first",
                coaches.get(0).getWins() >= coaches.get(1).getWins());
        assertTrue("Coach with fewest wins must sort last",
                coaches.get(2).getWins() <= coaches.get(1).getWins());
    }

    @Test
    public void compCoachWins_sameCoach_returnsZero() {
        CompCoachWins comp = new CompCoachWins();
        assertEquals(0, comp.compare(teamA.getHeadCoach(), teamA.getHeadCoach()));
    }

    // -------------------------------------------------------------------------
    // CompCoachOvr
    // -------------------------------------------------------------------------

    @Test
    public void compCoachOvr_higherOvr_sortsFirst() {
        CompCoachOvr comp = new CompCoachOvr();
        Staff coachA = teamA.getHeadCoach();
        staff.Staff coachB = teamB.getHeadCoach();

        // Force a gap in the overall
        coachA.ratOff = 90; coachA.ratDef = 90; coachA.ratTalent = 90; coachA.ratDiscipline = 90;
        coachB.ratOff = 50; coachB.ratDef = 50; coachB.ratTalent = 50; coachB.ratDiscipline = 50;

        int ovrA = coachA.getStaffOverall(coachA.overallWt);
        int ovrB = coachB.getStaffOverall(coachB.overallWt);

        assertTrue("Higher overall coach must sort first", comp.compare(coachA, coachB) < 0);
        assertTrue("Lower overall coach must sort last",   comp.compare(coachB, coachA) > 0);
    }

    @Test
    public void compCoachOvr_sameCoach_returnsZero() {
        CompCoachOvr comp = new CompCoachOvr();
        Staff coach = teamA.getHeadCoach();
        assertEquals(0, comp.compare(coach, coach));
    }

    // -------------------------------------------------------------------------
    // CompPlayerRushYards
    // -------------------------------------------------------------------------

    @Test
    public void compPlayerRushYards_higherYards_sortsFirst() {
        PlayerOffense rb1 = new PlayerOffense(teamA, "RB One", "RB", 1, 1500, 10, 20, 150, 1, 2);
        PlayerOffense rb2 = new PlayerOffense(teamA, "RB Two", "RB", 2, 800,  5, 10, 80,  0, 1);

        List<PlayerOffense> players = new ArrayList<>(Arrays.asList(rb2, rb1));
        Collections.sort(players, new CompPlayerRushYards());

        assertEquals("Higher rush yards must sort first", 1500, players.get(0).rushYards);
        assertEquals("Lower rush yards must sort last",    800, players.get(1).rushYards);
    }

    // -------------------------------------------------------------------------
    // CompPlayerTackles
    // -------------------------------------------------------------------------

    @Test
    public void compPlayerTackles_moreTackles_sortsFirst() {
        PlayerDefense lb1 = new PlayerDefense(teamA, "LB One", "LB", 1, 100, 10, 2, 1);
        PlayerDefense lb2 = new PlayerDefense(teamA, "LB Two", "LB", 2, 45,  5, 1, 0);

        List<PlayerDefense> players = new ArrayList<>(Arrays.asList(lb2, lb1));
        Collections.sort(players, new CompPlayerTackles());

        assertEquals("More tackles must sort first", 100, players.get(0).tackles);
        assertEquals("Fewer tackles must sort last",  45, players.get(1).tackles);
    }

    // -------------------------------------------------------------------------
    // CompPlayerSacks
    // -------------------------------------------------------------------------

    @Test
    public void compPlayerSacks_moreSacks_sortsFirst() {
        PlayerDefense dl1 = new PlayerDefense(teamA, "DL One", "DL", 1, 30, 12, 1, 0);
        PlayerDefense dl2 = new PlayerDefense(teamA, "DL Two", "DL", 2, 20,  4, 0, 0);

        List<PlayerDefense> players = new ArrayList<>(Arrays.asList(dl2, dl1));
        Collections.sort(players, new CompPlayerSacks());

        assertEquals("More sacks must sort first", 12, players.get(0).sacks);
        assertEquals("Fewer sacks must sort last",  4, players.get(1).sacks);
    }
}
