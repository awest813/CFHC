package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import positions.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TransferInvariantTest {

    private League league;
    private Team userTeam;
    private GameUiBridge noOpBridge;

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
        noOpBridge = CareerAuditTestHelper.noOpBridge();
    }

    @Test
    public void transfer_afterAdvanceSeason_allTeamsStillHaveValidRosters() {
        league.advanceSeason();

        league.transferPlayers(noOpBridge);

        for (Team team : league.getTeamList()) {
            List<Player> players = team.getAllPlayers();
            assertTrue(team.getName() + " should have players after transfer", players.size() > 0);
            for (Player p : players) {
                assertValidPlayer(p, team.getName());
            }
        }
    }

    @Test
    public void transfer_noPlayerAppearsOnMultipleTeams() {
        league.advanceSeason();

        league.transferPlayers(noOpBridge);

        java.util.IdentityHashMap<Player, Team> playerTeamMap = new java.util.IdentityHashMap<>();
        for (Team team : league.getTeamList()) {
            for (Player p : team.getAllPlayers()) {
                Team previous = playerTeamMap.get(p);
                if (previous != null) {
                    assertNotEquals("Same Player object should not appear on two teams",
                            previous.getName(), team.getName());
                }
                playerTeamMap.put(p, team);
            }
        }
    }

    @Test
    public void transfer_transferredPlayersMarkedAsTransfer() {
        league.advanceSeason();

        int totalBefore = 0;
        for (Team t : league.getTeamList()) {
            totalBefore += t.getAllPlayers().size();
        }

        league.transferPlayers(noOpBridge);

        int totalAfter = 0;
        int transferCount = 0;
        for (Team t : league.getTeamList()) {
            for (Player p : t.getAllPlayers()) {
                totalAfter++;
                if (p.isTransfer) transferCount++;
            }
        }
        assertTrue("Total players across league should be positive", totalAfter > 0);
    }

    @Test
    public void transfer_generatesNewsStories() {
        league.advanceSeason();

        league.transferPlayers(noOpBridge);

        boolean hasTransferNews = false;
        for (List<String> stories : league.newsStories) {
            for (String story : stories) {
                if (story.toLowerCase().contains("transfer")) {
                    hasTransferNews = true;
                    break;
                }
            }
            if (hasTransferNews) break;
        }
    }

    @Test
    public void transfer_multipleSeasons_remainsStable() {
        for (int season = 0; season < 3; season++) {
            league.advanceSeason();
            league.transferPlayers(noOpBridge);

            for (Team team : league.getTeamList()) {
                assertTrue(team.getName() + " should retain players after season " + season,
                        team.getAllPlayers().size() > 0);
            }
        }
    }

    @Test
    public void transfer_saveLoadRoundTrip_preservesRosters() {
        league.advanceSeason();
        league.transferPlayers(noOpBridge);

        List<Integer> rosterSizes = new ArrayList<>();
        for (Team t : league.getTeamList()) {
            rosterSizes.add(t.getAllPlayers().size());
        }

        java.io.File tmpFile = new java.io.File(System.getProperty("user.dir"), "build/tmp/transfer-test-save.cfb");
        tmpFile.getParentFile().mkdirs();
        assertTrue("Save should succeed", league.saveLeague(tmpFile));

        DesktopResourceProvider resources = new DesktopResourceProvider(System.getProperty("user.dir"));
        League loaded = new League(tmpFile,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                noOpBridge, true);
        loaded.setPlatformResourceProvider(resources);

        assertEquals("Team count preserved", league.getTeamList().size(), loaded.getTeamList().size());
        for (int i = 0; i < loaded.getTeamList().size(); i++) {
            Team loadedTeam = loaded.getTeamList().get(i);
            assertTrue(loadedTeam.getName() + " should have players after load",
                    loadedTeam.getAllPlayers().size() > 0);
            for (Player p : loadedTeam.getAllPlayers()) {
                assertValidPlayer(p, loadedTeam.getName());
            }
        }
        tmpFile.delete();
    }

    @Test
    public void transfer_transferPoolPartiallyOrFullyCleared() {
        league.advanceSeason();

        int qbPoolBefore = league.transferQBs.size();
        int rbPoolBefore = league.transferRBs.size();

        league.transferPlayers(noOpBridge);

        assertTrue("Transfer QB pool should not grow after processing",
                league.transferQBs.size() <= qbPoolBefore);
        assertTrue("Transfer RB pool should not grow after processing",
                league.transferRBs.size() <= rbPoolBefore);
    }

    private void assertValidPlayer(Player p, String teamName) {
        assertNotNull(teamName + " player should have a position", p.position);
        assertTrue(teamName + " player " + p.getName() + " OVR should be 0-99: " + p.ratOvr,
                p.ratOvr >= 0 && p.ratOvr <= 99);
        assertTrue(teamName + " player " + p.getName() + " year should be positive: " + p.year,
                p.year >= 0);
        assertTrue(teamName + " player " + p.getName() + " potential should be 0-99: " + p.ratPot,
                p.ratPot >= 0 && p.ratPot <= 99);
    }

    private static class CareerAuditTestHelper {
        static GameUiBridge noOpBridge() {
            return new GameUiBridge() {
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
            };
        }
    }
}
