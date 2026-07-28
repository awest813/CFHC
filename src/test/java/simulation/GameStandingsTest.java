package simulation;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import simulation.Conference;

import static org.junit.Assert.*;

public class GameStandingsTest {

    private League league;
    private SeasonController controller;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false
        );
        league.setPlatformResourceProvider(resources);
        league.userTeam = league.getTeamList().get(0);
        league.userTeam.setUserControlled(true);
        controller = new SeasonController(league, noOpBridge());
    }

    @Test
    public void standings_teamWinsLossesAreConsistent() {
        advanceToRegularSeason();

        for (Team t : league.getTeamList()) {
            int total = t.wins + t.losses;
            assertTrue("Team " + t.name + " should have played at least 1 game: "
                    + total, total >= 0);
        }
    }

    @Test
    public void standings_conferenceWinsDoNotExceedTotalWins() {
        advanceToRegularSeason();

        for (Team t : league.getTeamList()) {
            int confWins = t.getConfWins();
            assertTrue("Team " + t.name + " conf wins (" + confWins
                    + ") should not exceed total wins (" + t.wins + ")",
                    confWins <= t.wins);
        }
    }

    @Test
    public void standings_confLossesDoNotExceedTotalLosses() {
        advanceToRegularSeason();

        for (Team t : league.getTeamList()) {
            int confLosses = t.getConfLosses();
            assertTrue("Team " + t.name + " conf losses (" + confLosses
                    + ") should not exceed total losses (" + t.losses + ")",
                    confLosses <= t.losses);
        }
    }

    @Test
    public void standings_divWinsDoNotExceedConfWins() {
        advanceToRegularSeason();

        for (Team t : league.getTeamList()) {
            int divWins = t.getDivWins();
            int confWins = t.getConfWins();
            assertTrue("Team " + t.name + " div wins (" + divWins
                    + ") should not exceed conf wins (" + confWins + ")",
                    divWins <= confWins);
        }
    }

    @Test
    public void standings_noTeamHasNegativeWins() {
        advanceToRegularSeason();

        for (Team t : league.getTeamList()) {
            assertTrue("Team " + t.name + " should not have negative wins: " + t.wins,
                    t.wins >= 0);
            assertTrue("Team " + t.name + " should not have negative losses: " + t.losses,
                    t.losses >= 0);
        }
    }

    @Test
    public void standings_conferenceTeamCountsAreReasonable() {
        List<Conference> conferences = league.getConferences();
        assertNotNull("Conferences should not be null", conferences);
        assertTrue("Should have at least 1 conference", conferences.size() >= 1);
        for (Conference c : conferences) {
            List<Team> teams = c.getTeams();
            assertTrue("Conference should have at least 2 teams",
                    teams.size() >= 2);
            for (Team t : teams) {
                assertNotNull("Team in conference should not be null", t);
            }
        }
    }

    private void advanceToRegularSeason() {
        controller.advanceWeek();
        for (int w = 0; w < league.regSeasonWeeks / 2; w++) {
            controller.advanceWeek();
        }
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
