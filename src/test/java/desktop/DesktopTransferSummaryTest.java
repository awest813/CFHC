package desktop;

import org.junit.Before;
import org.junit.Test;
import simulation.League;
import simulation.PlatformResourceProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DesktopTransferSummaryTest {

    private League league;

    @Before
    public void setUp() {
        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false,
                false
        );
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
    }

    @Test
    public void emptyTransfers_showFriendlyCopy() {
        league.userTransfers = "";
        league.sumTransfers = "";
        assertTrue(TransferPortalDialog.userTransferSummary(league).contains("No transfers"));
        assertTrue(TransferPortalDialog.leagueTransferSummary(league).contains("No league-wide"));
    }

    @Test
    public void populatedTransfers_areReturnedLiterally() {
        league.userTransfers = "QB Jane Doe [Jr] Ovr: 88 (State)\n";
        league.sumTransfers = "88 QB Jane Doe [transfer] Your Team (ST)\n";
        assertTrue(TransferPortalDialog.userTransferSummary(league).contains("Jane Doe"));
        assertTrue(TransferPortalDialog.leagueTransferSummary(league).contains("88 QB"));
        assertFalse(TransferPortalDialog.userTransferSummary(league).contains("No transfers"));
    }
}
