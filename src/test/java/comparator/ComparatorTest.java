package comparator;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;
import positions.Player;
import positions.PlayerQB;
import simulation.League;
import simulation.PlatformResourceProvider;
import simulation.Team;

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
        assertTrue("League must have at least 3 teams", league.teamList.size() >= 3);
        teamA = league.teamList.get(0);
        teamB = league.teamList.get(1);
        teamC = league.teamList.get(2);
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
        simulation.Conference confA = league.conferences.get(0);
        simulation.Conference confB = league.conferences.get(1);

        confA.confPrestige = 90;
        confB.confPrestige = 50;

        List<simulation.Conference> confs = new ArrayList<>(Arrays.asList(confA, confB));
        Collections.sort(confs, new CompConfPrestige());

        assertEquals("Higher conf prestige must sort first", 90, confs.get(0).confPrestige);
        assertEquals("Lower conf prestige must sort last",   50, confs.get(1).confPrestige);
    }
}
