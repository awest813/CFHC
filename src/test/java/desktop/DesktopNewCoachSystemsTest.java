package desktop;

import org.junit.Before;
import org.junit.Test;
import positions.Player;
import recruiting.RecruitingController;
import recruiting.RecruitingPlayerRecord;
import recruiting.RecruitingSessionData;
import simulation.CoachSkills;
import simulation.GameFlowManager;
import simulation.GameUiBridge;
import simulation.League;
import simulation.LeagueLaunchCoordinator;
import simulation.PlatformResourceProvider;
import simulation.SeasonController;
import simulation.SimulationFacade;
import simulation.Team;
import staff.HeadCoach;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DesktopNewCoachSystemsTest {

    private static final int SEASONS_TO_RUN = 3;
    private static final int MAX_STEPS_PER_SEASON = 200;
    private static final int MIN_HEALTHY_ROSTER_SIZE = 40;

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
                false,
                false
        );
        league.setPlatformResourceProvider(resources);

        userTeam = league.getTeamList().get(0);
        userTeam.setupUserCoach("Desktop Test Coach");
        userTeam.setUserControlled(true);
        league.userTeam = userTeam;
        league.careerMode = false;
    }

    @Test
    public void freshCoachCanUseProgramRecruitingNilAndCompleteThreeSeasons() {
        exerciseCoachProgramSkillTreeAndNil();
        exerciseRecruitingOptions();
        simulateThreeCompleteSeasons();
    }

    private void exerciseCoachProgramSkillTreeAndNil() {
        HeadCoach headCoach = userTeam.getHeadCoach();
        assertNotNull(headCoach);
        assertEquals(0, CoachSkills.getRank(headCoach.coachSkillRanksBits, CoachSkills.RECRUITING));
        assertEquals(0, CoachSkills.getRank(headCoach.coachSkillRanksBits, CoachSkills.NIL_MARKETING));

        RecruitingSessionData baselineSession = SimulationFacade.prepareRecruitingSession(userTeam);
        int baselineBudget = baselineSession.recruitingBudget;

        headCoach.addCoachSkillXp(200);
        assertTrue(headCoach.tryPurchaseCoachSkillRank(CoachSkills.RECRUITING));
        assertTrue(headCoach.tryPurchaseCoachSkillRank(CoachSkills.NIL_MARKETING));
        assertTrue(headCoach.tryPurchaseCoachSkillRank(CoachSkills.DEVELOPMENT));
        userTeam.setNilCollectiveLevel(3);

        assertEquals(1, CoachSkills.getRank(headCoach.coachSkillRanksBits, CoachSkills.RECRUITING));
        assertEquals(1, CoachSkills.getRank(headCoach.coachSkillRanksBits, CoachSkills.NIL_MARKETING));
        assertEquals(1, CoachSkills.getRank(headCoach.coachSkillRanksBits, CoachSkills.DEVELOPMENT));
        assertEquals(22, headCoach.recruitingBudgetBonus());
        assertEquals(85, headCoach.nilMarketingWeeklyStipend());
        assertTrue(userTeam.getWeeklyCollectiveStipend() >= 3 * 95 + 85);

        RecruitingSessionData boostedSession = SimulationFacade.prepareRecruitingSession(userTeam);
        assertTrue("NIL tier and recruiting skill should increase available budget",
                boostedSession.recruitingBudget >= baselineBudget + 3 * 24 + 22);
        assertEquals("Android-style payload parsing should preserve NIL and coach budget effects",
                boostedSession.recruitingBudget, androidStyleRecruitingBudget());

        String programSummary = CoachSkills.buildProgramSummary(userTeam, headCoach);
        assertTrue(programSummary.contains("NIL / booster collective: Tier 3"));
        assertTrue(programSummary.contains("Recruiting pitch"));
        assertTrue(programSummary.contains("rank 1/3"));
        assertTrue(programSummary.contains("NIL & boosters"));
    }

    private void exerciseRecruitingOptions() {
        league.recruitPlayers();

        CapturingFlow flow = new CapturingFlow();
        RecruitingSessionData session = SimulationFacade.prepareRecruitingSession(userTeam);
        assertFalse("A generated recruiting board should have available players", session.availAll.isEmpty());

        RecruitingController controller = new RecruitingController(session, flow);
        RecruitingPlayerRecord initialTarget = session.availAll.get(0);
        session.recruitingBudget = Math.max(session.recruitingBudget, initialTarget.cost() + 200);

        int budgetBeforeScout = session.recruitingBudget;
        assertTrue(controller.scoutPlayer(initialTarget));
        RecruitingPlayerRecord scoutedTarget = session.availAll.get(0);
        assertTrue("Scouting should spend some budget", session.recruitingBudget < budgetBeforeScout);
        assertTrue("Scouting should mark portable recruit records in-session",
                session.isScouted(scoutedTarget));
        int budgetAfterScout = session.recruitingBudget;
        assertTrue(controller.scoutPlayer(scoutedTarget));
        assertEquals("Scouting the same portable recruit twice should not charge twice",
                budgetAfterScout, session.recruitingBudget);

        int rosterBefore = userTeam.getAllPlayers().size();
        session.recruitingBudget = Math.max(session.recruitingBudget, scoutedTarget.cost() + 200);
        controller.recruitPlayer(scoutedTarget, false);
        controller.finishRecruiting();

        assertEquals(1, session.playersRecruited.size());
        assertTrue(flow.finishedRecruitingData.contains("END_RECRUITS"));
        userTeam.recruitPlayersFromStr(flow.finishedRecruitingData);
        league.updateTeamTalentRatings();
        assertTrue("Signed recruit should apply to the user roster",
                userTeam.getAllPlayers().size() > rosterBefore);
    }

    private int androidStyleRecruitingBudget() {
        RecruitingSessionData session = RecruitingSessionData.fromUserTeamInfo(
                SimulationFacade.buildRecruitingPayload(userTeam));
        session.applyBudgetBonuses(SimulationFacade.MIN_ROSTER_SIZE);
        return session.recruitingBudget;
    }

    private void simulateThreeCompleteSeasons() {
        int expectedTeamCount = league.getTeamList().size();

        for (int season = 0; season < SEASONS_TO_RUN; season++) {
            int seasonYear = league.getYear();
            int historyBefore = league.getLeagueHistory().size();
            int championsBefore = countNationalChampionships();
            validateNewSeasonState(expectedTeamCount, seasonYear);

            HeadlessBridge bridge = new HeadlessBridge();
            SeasonController controller = new SeasonController(league, bridge);
            int steps = 0;
            while (!bridge.recruitingComplete && steps < MAX_STEPS_PER_SEASON) {
                controller.advanceWeek();
                steps++;
            }

            assertTrue("Recruiting should be reached within " + MAX_STEPS_PER_SEASON + " steps",
                    bridge.recruitingComplete);
            validateCompletedSeason(expectedTeamCount, seasonYear, historyBefore, championsBefore, steps);

            league.startNextSeason();
            validateNewSeasonState(expectedTeamCount, league.getYear());
        }
    }

    private void validateNewSeasonState(int expectedTeamCount, int expectedYear) {
        assertEquals(expectedTeamCount, league.getTeamList().size());
        assertEquals(expectedYear, league.getYear());
        assertEquals(0, league.currentWeek);
        assertNotNull(league.userTeam);
        assertTrue(league.userTeam.isUserControlled());
        validateRosterHealth();
        for (Team team : league.getTeamList()) {
            assertEquals("Unexpected schedule size for " + team.getName(),
                    league.regSeasonWeeks - 1, team.getGameSchedule().size());
            assertEquals("Wins should reset for " + team.getName(), 0, team.getWins());
            assertEquals("Losses should reset for " + team.getName(), 0, team.getLosses());
        }
    }

    private void validateCompletedSeason(int expectedTeamCount, int seasonYear,
                                         int historyBefore, int championsBefore, int steps) {
        assertTrue("Season " + seasonYear + " completed suspiciously quickly", steps > league.regSeasonWeeks);
        assertEquals(expectedTeamCount, league.getTeamList().size());
        assertTrue("League history should record season " + seasonYear,
                league.getLeagueHistory().size() > historyBefore);
        assertTrue("A national champion should be crowned for " + seasonYear,
                countNationalChampionships() > championsBefore);
        assertNotNull(league.userTeam);
        assertTrue(league.userTeam.isUserControlled());
        validateRosterHealth();

        int minGames = league.regSeasonWeeks - 2;
        for (Team team : league.getTeamList()) {
            int gamesPlayed = team.getWins() + team.getLosses();
            assertTrue(team.getName() + " played only " + gamesPlayed + " games",
                    gamesPlayed >= minGames);
        }
    }

    private void validateRosterHealth() {
        for (Team team : league.getTeamList()) {
            assertTrue(team.getName() + " roster depleted to " + team.getAllPlayers().size(),
                    team.getAllPlayers().size() >= MIN_HEALTHY_ROSTER_SIZE);
        }
    }

    private int countNationalChampionships() {
        int total = 0;
        for (Team team : league.getTeamList()) {
            total += team.totalNCs;
        }
        return total;
    }

    private final class HeadlessBridge implements GameUiBridge {
        private boolean recruitingComplete;

        @Override public void crash() {
            throw new RuntimeException("Bridge crash requested");
        }

        @Override public void startRecruiting(File saveFile, Team userTeam) throws InterruptedException, IOException {}
        @Override public void transferPlayer(Player player) {}
        @Override public void updateSpinners() {}
        @Override public void disciplineAction(Player player, String issue, int gamesA, int gamesB) {}
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

        @Override public void startRecruitingFlow() {
            league.recruitPlayers();
            league.userTeam.recruitPlayersFromStr("");
            league.updateTeamTalentRatings();
            recruitingComplete = true;
        }
    }

    private static final class CapturingFlow implements GameFlowManager {
        private String finishedRecruitingData = "";

        @Override public void startNewGame(LeagueLaunchCoordinator.LaunchRequest.PrestigeMode prestigeMode, String userTeamName) {}
        @Override public void loadGame(String saveData) {}
        @Override public void importSave(String userTeamName) {}
        @Override public void finishRecruiting(String recruitsStr) { finishedRecruitingData = recruitsStr; }
        @Override public void startRecruiting(String userTeamInfo) {}
        @Override public void showNotification(String title, String message) {}
        @Override public void returnToMainHub() {}
    }
}
