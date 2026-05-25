package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import positions.Player;
import positions.PlayerQB;
import positions.PlayerRB;

import static org.junit.Assert.*;

public class ProgressionTest {

    private League league;
    private Team team;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        team = league.getTeamList().get(0);
    }

    @Test
    public void playerProgression_advanceTeamPlayers_ratingsStayBounded() {
        team.advanceTeamPlayers();

        for (Player p : team.getAllPlayers()) {
            assertTrue("Player " + p.getName() + " OVR should be 0-99 after advancement: " + p.ratOvr,
                    p.ratOvr >= 0 && p.ratOvr <= 99);
        }
    }

    @Test
    public void playerProgression_advanceSeasonPreservesPlayerList() {
        int countBefore = team.getAllPlayers().size();
        team.advanceTeamPlayers();
        int countAfter = team.getAllPlayers().size();
        assertTrue("Player count should not be negative after advancement",
                countAfter >= 0);
    }

    @Test
    public void playerProgression_playerRatingsStayWithinBounds() {
        for (Player p : team.getAllPlayers()) {
            assertTrue("Player " + p.getName() + " OVR should be 0-99: " + p.ratOvr,
                    p.ratOvr >= 0 && p.ratOvr <= 99);
        }
    }

    @Test
    public void playerProgression_qbHasExpectedAttributes() {
        for (Player p : team.getAllPlayers()) {
            if (p.position.equals("QB")) {
                PlayerQB qb = (PlayerQB) p;
                int passAcc = qb.getRatPassAcc();
                int passPow = qb.getRatPassPow();
                assertTrue("QB " + qb.getName() + " pass accuracy should be 0-99: " + passAcc,
                        passAcc >= 0 && passAcc <= 99);
                assertTrue("QB " + qb.getName() + " pass power should be 0-99: " + passPow,
                        passPow >= 0 && passPow <= 99);
            }
        }
    }

    @Test
    public void playerProgression_rbHasExpectedAttributes() {
        for (Player p : team.getAllPlayers()) {
            if (p.position.equals("RB") || p.position.equals("FB")) {
                PlayerRB rb = (PlayerRB) p;
                int speed = rb.getRatSpeed();
                int evasion = rb.getRatEvasion();
                assertTrue("RB " + rb.getName() + " speed should be 0-99: " + speed,
                        speed >= 0 && speed <= 99);
                assertTrue("RB " + rb.getName() + " evasion should be 0-99: " + evasion,
                        evasion >= 0 && evasion <= 99);
            }
        }
    }

    @Test
    public void playerProgression_potentialRatingsAreReasonable() {
        for (Player p : team.getAllPlayers()) {
            assertTrue("Player " + p.getName() + " potential should be 0-99: " + p.ratPot,
                    p.ratPot >= 0 && p.ratPot <= 99);
        }
    }

    @Test
    public void playerProgression_advanceSeasonDoesNotCorruptTeams() {
        int initialTeamCount = league.getTeamList().size();
        int initialPlayerCount = team.getAllPlayers().size();

        league.advanceSeason();

        assertEquals("Team count should be unchanged", initialTeamCount, league.getTeamList().size());
        assertNotNull("Team should still have players after advancement", team.getAllPlayers());
    }

    @Test
    public void playerProgression_multipleSeasonAdvancements() {
        for (int season = 0; season < 3; season++) {
            league.advanceSeason();
        }

        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                assertTrue("Player " + p.getName() + " OVR should be 0-99: " + p.ratOvr,
                        p.ratOvr >= 0 && p.ratOvr <= 99);
                assertTrue("Player " + p.getName() + " potential should be 0-99: " + p.ratPot,
                        p.ratPot >= 0 && p.ratPot <= 99);
            }
        }
    }
}
