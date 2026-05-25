package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import positions.Player;

import static org.junit.Assert.*;

public class TransferTest {

    private League league;
    private Team userTeam;

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
        userTeam = league.getTeamList().get(0);
        userTeam.setUserControlled(true);
        league.userTeam = userTeam;
    }

    @Test
    public void transferPlayers_doesNotCrash() {
        league.transferPlayers(new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(Player player, String issue, int a, int b) {}
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
            @Override public void startRecruitingFlow() {}
        });
    }

    @Test
    public void transferPlayers_teamRosterRemainsValid() {
        int initialCount = userTeam.getAllPlayers().size();

        league.transferPlayers(new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(Player player, String issue, int a, int b) {}
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
            @Override public void startRecruitingFlow() {}
        });

        for (Player p : userTeam.getAllPlayers()) {
            assertNotNull("Player should have valid position", p.position);
            assertTrue("Player OVR should be in valid range: " + p.ratOvr,
                    p.ratOvr >= 0 && p.ratOvr <= 99);
        }
    }

    @Test
    public void transferPlayers_advanceSeasonThenTransfer() {
        int[] playerCounts = new int[league.getTeamList().size()];
        for (int t = 0; t < league.getTeamList().size(); t++) {
            playerCounts[t] = league.getTeamList().get(t).getAllPlayers().size();
        }

        league.advanceSeason();

        league.transferPlayers(new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team userTeam) {}
            @Override public void transferPlayer(Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(Player player, String issue, int a, int b) {}
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
            @Override public void startRecruitingFlow() {}
        });

        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                assertTrue("Player OVR should be 0-99 after transfer: " + p.ratOvr,
                        p.ratOvr >= 0 && p.ratOvr <= 99);
            }
        }
    }
}
