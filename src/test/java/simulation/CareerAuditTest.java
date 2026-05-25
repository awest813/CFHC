package simulation;

import desktop.DesktopResourceProvider;
import org.junit.Before;
import org.junit.Test;

import staff.HeadCoach;
import staff.Staff;

import java.io.File;

import static org.junit.Assert.*;

public class CareerAuditTest {

    private League league;
    private Team userTeam;
    private SeasonController controller;

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

        controller = new SeasonController(league, noOpBridge());
    }

    @Test
    public void career_headCoachExistsAndHasValidRatings() {
        HeadCoach hc = userTeam.getHeadCoach();
        assertNotNull("User team should have a head coach", hc);
        assertNotNull("Head coach should have a name", hc.name);
        assertTrue("HC overall rating should be 0-99: " + hc.ratOvr,
                hc.ratOvr >= 0 && hc.ratOvr <= 99);
        assertTrue("HC offensive rating should be 0-99: " + hc.ratOff,
                hc.ratOff >= 0 && hc.ratOff <= 99);
        assertTrue("HC defensive rating should be 0-99: " + hc.ratDef,
                hc.ratDef >= 0 && hc.ratDef <= 99);
    }

    @Test
    public void career_coordinatorExists() {
        Staff oc = userTeam.getOC();
        Staff dc = userTeam.getDC();
        assertNotNull("Team should have an OC", oc);
        assertNotNull("Team should have a DC", dc);
    }

    @Test
    public void career_coachAdvanceSeason_doesNotCrash() {
        league.advanceSeason();
        HeadCoach hc = userTeam.getHeadCoach();
        assertNotNull("Head coach should still exist after season advance", hc);
    }

    @Test
    public void career_teamPrestigeStaysInBounds() {
        assertTrue("Team prestige should be 0-100: " + userTeam.getTeamPrestige(),
                userTeam.getTeamPrestige() >= 0 && userTeam.getTeamPrestige() <= 100);
    }

    @Test
    public void career_multipleSeasons_headCoachRetained() {
        for (int season = 0; season < 3; season++) {
            league.advanceSeason();
            league.startNextSeason();
        }

        HeadCoach hc = userTeam.getHeadCoach();
        assertNotNull("Head coach should exist after multiple seasons", hc);
        assertTrue("HC overall should be 0-99: " + hc.ratOvr,
                hc.ratOvr >= 0 && hc.ratOvr <= 99);
    }

    private static GameUiBridge noOpBridge() {
        return new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(File saveFile, Team userTeam) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int a, int b) {}
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
