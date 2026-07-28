package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import positions.Player;
import positions.PlayerQB;

import java.util.List;

import static org.junit.Assert.*;

public class DepthChartTest {

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
    public void positionSubclasses_canBeRetrieved() {
        assertNotNull("Team should have QBs", team.getTeamQBs());
        assertNotNull("Team should have RBs", team.getTeamRBs());
        assertNotNull("Team should have WRs", team.getTeamWRs());
        assertNotNull("Team should have TEs", team.getTeamTEs());
        assertNotNull("Team should have OLs", team.getTeamOLs());
        assertNotNull("Team should have DLs", team.getTeamDLs());
        assertNotNull("Team should have LBs", team.getTeamLBs());
        assertNotNull("Team should have CBs", team.getTeamCBs());
        assertNotNull("Team should have Ss", team.getTeamSs());
        assertNotNull("Team should have Ks", team.getTeamKs());
    }

    @Test
    public void positionGroups_haveReasonableSizes() {
        assertTrue("Team should have at least 1 QB", team.getTeamQBs().size() >= 1);
        assertTrue("Team should have at least 1 RB", team.getTeamRBs().size() >= 1);
        assertTrue("Team should have at least 2 WRs", team.getTeamWRs().size() >= 2);
        assertTrue("Team should have at least 1 TE", team.getTeamTEs().size() >= 1);
        assertTrue("Team should have at least 3 OL", team.getTeamOLs().size() >= 3);
        assertTrue("Team should have at least 2 DL", team.getTeamDLs().size() >= 2);
        assertTrue("Team should have at least 2 LB", team.getTeamLBs().size() >= 2);
        assertTrue("Team should have at least 2 CB", team.getTeamCBs().size() >= 2);
        assertTrue("Team should have at least 1 S", team.getTeamSs().size() >= 1);
        assertTrue("Team should have at least 1 K", team.getTeamKs().size() >= 1);
    }

    @Test
    public void starterQB_canBeSelected() {
        List<PlayerQB> qbs = team.getTeamQBs();
        PlayerQB starter = qbs.get(0);
        assertNotNull("QB starter should not be null", starter);
        assertTrue("QB starter should have OVR >= 0", starter.ratOvr >= 0);
    }

    @Test
    public void allPlayers_haveValidPositions() {
        for (Player p : team.getAllPlayers()) {
            assertNotNull("Player should have a position", p.position);
            assertTrue("Position should be a valid abbreviation: '" + p.position + "'",
                    p.position.matches("QB|RB|WR|TE|OL|DL|LB|CB|S|K"));
        }
    }

    @Test
    public void starterAndBackup_areDifferentPlayers() {
        List<PlayerQB> qbs = team.getTeamQBs();
        if (qbs.size() >= 2) {
            PlayerQB starter = qbs.get(0);
            PlayerQB backup = qbs.get(1);
            assertNotEquals("Starter and backup should be different players",
                    starter.getName(), backup.getName());
        }
    }

    @Test
    public void getPositionPlayer_returnsValidPlayerByIndex() {
        assertNotNull("getQB(0) should not be null", team.getQB(0));
        assertNotNull("getRB(0) should not be null", team.getRB(0));
        assertNotNull("getWR(0) should not be null", team.getWR(0));
        assertNotNull("getTE(0) should not be null", team.getTE(0));
        assertNotNull("getOL(0) should not be null", team.getOL(0));
        assertNotNull("getDL(0) should not be null", team.getDL(0));
        assertNotNull("getLB(0) should not be null", team.getLB(0));
        assertNotNull("getCB(0) should not be null", team.getCB(0));
        assertNotNull("getS(0) should not be null", team.getS(0));
        assertNotNull("getK(0) should not be null", team.getK(0));
    }

    @Test
    public void getPositionStudentAthlete_teamHasPlayers() {
        List<Player> allPlayers = team.getAllPlayers();
        assertFalse("Team should have players", allPlayers.isEmpty());
    }

    @Test
    public void emptyPositionGroup_getQBReturnsNull() {
        team.teamQBs.clear();
        assertTrue(team.teamQBs.isEmpty());
        assertNull("Empty QB list should return null starter", team.getQB(0));
    }

    @Test
    public void rosterStatus_usesSharedInjuryWeeksLabel() {
        PlayerQB qb = team.getTeamQBs().get(0);
        qb.isInjured = true;
        qb.injury = new Injury(1, "Wrist", qb);
        String status = team.getRosterStatus(qb, 0, "QB");
        assertTrue(status.contains("INJ"));
        assertTrue("singular week label", status.contains("1 wk"));
        assertFalse("should not use old '1 wks' plural", status.contains("1 wks"));
    }
}
