package simulation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import staff.DC;
import staff.HeadCoach;
import staff.OC;
import staff.Staff;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Headless substitutes for manual career UI QA in {@code docs/cleanup-audit.md}:
 * fired coach job offers, team reassignment, coordinator hiring, and post-switch playability.
 */
public class CareerUiPlayabilityTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private League league;
    private Team userTeam;
    private HeadCoach userHc;
    private SeasonController controller;
    private final boolean[] jobOffersShown = {false};
    private final boolean[] coordinatorHiringShown = {false};
    private final boolean[] recruitingStarted = {false};

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
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
        league.careerMode = true;
        userTeam = league.getTeamList().get(0);
        userTeam.setUserControlled(true);
        league.userTeam = userTeam;
        userTeam.setupUserCoach("Career QA Coach");
        userHc = userTeam.getHeadCoach();
        userHc.user = true;

        controller = new SeasonController(league, new GameUiBridge() {
            @Override public void crash() {}
            @Override public void startRecruiting(java.io.File saveFile, Team t) {}
            @Override public void transferPlayer(positions.Player player) {}
            @Override public void updateSpinners() {}
            @Override public void disciplineAction(positions.Player player, String issue, int a, int b) {}
            @Override public void updateSimStatus(String s, String b, boolean m) {}
            @Override public void showNotification(String t, String m) {}
            @Override public void refreshCurrentPage() {}
            @Override public void showAwardsSummary(String s) {}
            @Override public void showMidseasonSummary() {}
            @Override public void showSeasonSummary() {}
            @Override public void showContractDialog() {}
            @Override public void showJobOffersDialog() { jobOffersShown[0] = true; }
            @Override public void showPromotionsDialog() {}
            @Override public void showRedshirtList() {}
            @Override public void showTransferList() {}
            @Override public void showRealignmentSummary() {}
            @Override public void showCoordinatorHiringDialog() { coordinatorHiringShown[0] = true; }
            @Override public void startRecruitingFlow() {
                recruitingStarted[0] = true;
                SimulationFacade.prepareCpuRecruiting(league);
                league.finishRecruitingSeason("");
            }
        });
    }

    @Test
    public void firedCoach_jobOffersWeek_showsDialogAndListsOpenings() {
        createHeadCoachVacancy(league.getTeamList().get(5));
        userTeam.fired = true;
        league.currentWeek = league.regSeasonWeeks + 6;

        controller.advanceWeek();

        assertTrue(jobOffersShown[0]);
        ArrayList<Team> offers = league.getCoachListFired(
                userHc.getStaffOverall(userHc.overallWt), userTeam.getName());
        assertFalse("Fired coach should have at least one opening", offers.isEmpty());
    }

    @Test
    public void firedCoach_teamSwitch_remainsPlayableThroughNextSeason() {
        Team destination = league.getTeamList().get(5);
        createHeadCoachVacancy(destination);
        userTeam.fired = true;
        String oldTeamName = userTeam.getName();

        ArrayList<Team> offers = league.getCoachListFired(
                userHc.getStaffOverall(userHc.overallWt), oldTeamName);
        assertFalse(offers.isEmpty());
        Team newTeam = offers.get(0);

        simulateUserJobChange(league, userTeam, userHc, newTeam);

        assertEquals(newTeam.getName(), league.userTeam.getName());
        assertFalse(league.userTeam.fired);
        assertEquals(userHc, league.userTeam.getHeadCoach());
        assertTrue(league.userTeam.isUserControlled());

        league.currentWeek = league.regSeasonWeeks + 4;
        int guard = 0;
        while (!recruitingStarted[0] && guard++ < 20) {
            controller.advanceWeek();
        }
        assertTrue(recruitingStarted[0]);
        assertEquals(0, league.currentWeek);
        assertFalse(league.userTeam.getGameSchedule().isEmpty());
    }

    @Test
    public void coordinatorHiringWeek_missingOc_promptsAndCanHire() {
        userTeam.setOC(null);
        league.currentWeek = league.regSeasonWeeks + 8;

        controller.advanceWeek();

        assertTrue(coordinatorHiringShown[0]);
        ArrayList<Staff> candidates = league.getOCList(userTeam.getHeadCoach());
        assertFalse(candidates.isEmpty());
        userTeam.setOC(new OC(candidates.get(0), userTeam));
        assertNotNull(userTeam.getOC());
        assertEquals("OC", userTeam.getOC().position);
    }

    @Test
    public void promotionCandidate_canFindPromotionOpenings() {
        userHc.promotionCandidate = true;
        userHc.ratTalent = 90;
        userHc.ratOvr = 90;
        createHeadCoachVacancy(league.getTeamList().get(7));

        ArrayList<Team> promotions = league.getCoachPromotionList(
                userHc.getStaffOverall(userHc.overallWt), 2.0, userTeam.getName());
        if (promotions.isEmpty()) {
            promotions = league.getCoachListFired(
                    userHc.getStaffOverall(userHc.overallWt), userTeam.getName());
        }
        assertFalse(promotions.isEmpty());
        Team promotedTo = promotions.get(0);
        simulateUserJobChange(league, userTeam, userHc, promotedTo);

        assertEquals(promotedTo.getName(), league.userTeam.getName());
        assertFalse(userHc.promotionCandidate);
    }

    @Test
    public void androidStyleSaveSlots_surviveMidCareerReload() throws Exception {
        File filesDir = tmp.newFolder("android-files");
        SaveLoadService saveLoadService = new SaveLoadService(filesDir);
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));

        for (int i = 0; i < 6; i++) {
            controller.advanceWeek();
        }
        int expectedWeek = league.currentWeek;
        String teamName = league.userTeam.getName();
        assertTrue(saveLoadService.saveToSlot(league, 1));

        File saveFile = LeagueSaveStorage.getSlotFile(filesDir, 1);
        League loaded = new League(
                saveFile,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                GameUiBridge.NO_OP,
                true
        );
        loaded.setPlatformResourceProvider(resources);
        loaded.rebuildScheduleIfNeeded();

        assertEquals(expectedWeek, loaded.currentWeek);
        assertNotNull(loaded.userTeam);
        assertEquals(teamName, loaded.userTeam.getName());
        assertTrue(loaded.userTeam.getHeadCoach().user);
    }

    @Test
    public void recruitingCheckpoint_saveAndDoneRecruitingReload_startsNewSeason() throws Exception {
        File filesDir = tmp.newFolder("android-recruiting");
        SaveLoadService saveLoadService = new SaveLoadService(filesDir);
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));

        league.currentWeek = league.regSeasonWeeks + 13;
        league.recruitingPhaseActive = true;
        SimulationFacade.prepareCpuRecruiting(league);
        SimulationFacade.saveForUserRecruitingUi(league, league.userTeam, saveLoadService);

        LeagueLaunchCoordinator.LaunchResult result = LeagueLaunchCoordinator.load(
                LeagueLaunchCoordinator.LaunchRequest.doneRecruiting(""),
                filesDir,
                GameUiBridge.NO_OP,
                SimulationFacade.SEASON_START,
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                null,
                null
        );

        assertEquals(0, result.league.currentWeek);
        assertFalse(result.league.recruitingPhaseActive);
        assertFalse(result.league.userTeam.getGameSchedule().isEmpty());
    }

    private static void createHeadCoachVacancy(Team team) {
        team.setHeadCoach(null);
    }

    /** Engine equivalent of {@code MainActivity.changeTeams} without UI. */
    private static void simulateUserJobChange(League league, Team oldUserTeam, HeadCoach coach, Team destination) {
        oldUserTeam.newCoachTeamChanges();
        oldUserTeam.setUserControlled(false);
        oldUserTeam.setHeadCoach(null);
        league.coachHiringSingleTeam(oldUserTeam);
        if (oldUserTeam.getHeadCoach() == null) {
            oldUserTeam.promoteCoach();
        }
        league.newJobtransfer(destination.getName());

        Team newUserTeam = league.userTeam;
        newUserTeam.setHeadCoach(coach);
        coach.team = newUserTeam;
        coach.promotionCandidate = false;
        newUserTeam.fired = false;
        coach.contractYear = 0;
    }
}
