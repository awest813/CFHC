package simulation;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Phase 4 career depth (see docs/game-flow-audit-and-premium-plan.md): coach
 * salaries and buyouts, the AD approval meter, the stadium arc, media polls,
 * the Lineman of the Year award, Draft Night, Signing Day, and TV payouts.
 */
public class CareerDepthTest {

    private League league;
    private Team userTeam;
    private FileSystemResourceProvider resources;

    @Before
    public void setUp() {
        resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        SimRandom.pinNextSeed(0xCA3EE1L);
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
        league.userTeam = userTeam;
        userTeam.setUserControlled(true);
    }

    private boolean hasStoryStarting(String prefix) {
        for (int w = 0; w < league.newsStories.size(); w++) {
            for (String story : league.newsStories.get(w)) {
                if (story.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    public void coachSalaries_inRange_andBuyoutsChargedOnFiring() {
        double salary = userTeam.teamFinance.headCoachSalary();
        assertTrue("Salary should be within the designed band: " + salary,
                salary >= 0.5 && salary <= 12.0);

        // Force a mid-career firing on a CPU team and confirm the buyout hits the budget.
        Team cpu = league.getTeamList().get(1);
        cpu.setUserControlled(false);
        cpu.getHeadCoach().contractLength = 3;
        cpu.getHeadCoach().contractYear = 3;
        cpu.teamPrestigeStart = cpu.teamPrestige;
        cpu.setRankTeamPollScore(20);
        cpu.setTeamBudget(5_000_000);
        int budgetBefore = cpu.getTeamBudget();
        int coachPoolBefore = league.getCoachList().size();

        cpu.teamFinance.coachContracts(-3);

        assertTrue("CPU coach should have been fired", cpu.fired || cpu.getHeadCoach() == null);
        assertTrue("Firing should charge a buyout to the budget",
                cpu.getTeamBudget() < budgetBefore);
        assertTrue("Firing should free the coach into the pool",
                league.getCoachList().size() > coachPoolBefore);
        boolean buyoutMentioned = false;
        for (int w = 0; w < league.newsStories.size() && !buyoutMentioned; w++) {
            for (String story : league.newsStories.get(w)) {
                if (story.contains("buyout is estimated at")) {
                    buyoutMentioned = true;
                    break;
                }
            }
        }
        assertTrue("Firing news should mention the buyout", buyoutMentioned);
    }

    @Test
    public void adApproval_boundedAndResponsive() {
        userTeam.teamPrestigeStart = userTeam.teamPrestige - 10;
        userTeam.setTeamBudget(userTeam.getTeamBudget()); // no-op, keeps state explicit
        userTeam.wins = 9;
        userTeam.losses = 3;
        int strong = userTeam.teamFinance.getAdApproval();

        userTeam.teamPrestigeStart = userTeam.teamPrestige + 10;
        userTeam.wins = 3;
        userTeam.losses = 9;
        int weak = userTeam.teamFinance.getAdApproval();

        assertTrue("Approval should be bounded: " + strong, strong >= 0 && strong <= 100);
        assertTrue("Approval should be bounded: " + weak, weak >= 0 && weak <= 100);
        assertTrue("A winning, rising program should out-approve a falling one ("
                + strong + " vs " + weak + ")", strong > weak);
    }

    @Test
    public void stadium_capacityExpandsAndPersists() throws Exception {
        assertEquals("Base capacity from tier 1", 35000 + 12000, userTeam.getStadiumCapacity());

        userTeam.setTeamBudget(50_000_000);
        userTeam.teamPrestige = 85;
        league.upgradeStadiums();
        assertTrue("Rich prestigious program should expand", userTeam.teamStadium >= 2);
        assertTrue("Expansion should cost money", userTeam.getTeamBudget() < 50_000_000);

        File tmp = File.createTempFile("cfhc-stadium-roundtrip", ".cfb");
        try {
            assertTrue(league.saveLeague(tmp));
            League loaded = new League(tmp,
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                    resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                    GameUiBridge.NO_OP,
                    true);
            loaded.rebuildScheduleIfNeeded();
            assertEquals("Stadium tier must round-trip",
                    userTeam.teamStadium, loaded.getTeamList().get(0).teamStadium);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    @Test
    public void mediaPolls_rankedWeeklyWithStory() {
        league.currentWeek = 1;
        league.playWeek();
        league.currentWeek = 2;
        league.playWeek();

        assertTrue("AP ranks should be populated", league.getApRank(userTeam) >= 1);
        assertTrue("Coaches ranks should be populated", league.getCoachesRank(userTeam) >= 1);
        assertTrue("Media poll story should publish", hasStoryStarting("Media Polls>"));
    }

    @Test
    public void offseasonCeremonies_awardsDraftAndSigningDay() {
        // Season walkthrough: regular season + postseason + offseason to the gate.
        league.currentWeek = 1;
        for (int w = 1; w <= league.regSeasonWeeks - 1; w++) {
            league.playWeek();
        }

        // Lineman of the Year latches on first call.
        String lineman = league.getLinemanPOTYStr();
        assertTrue("Lineman ceremony should run", lineman.contains("Lineman of the Year"));
        assertTrue(league.linemanPOTYDecided);

        // Postseason (weeks R..R+3) then offseason through graduation (R+9) fires Draft Night.
        for (int w = 0; w < 4; w++) {
            league.playWeek();
        }
        SeasonController controller = new SeasonController(league, GameUiBridge.NO_OP);
        for (int w = 0; w < 6; w++) {
            controller.advanceWeek(); // R+4 summary .. R+9 graduation
        }
        assertTrue("Draft Night should publish after graduation", hasStoryStarting("Draft Night>"));

        // Recruiting gate + headless rollover fires Signing Day for the new season.
        league.currentWeek = league.regSeasonWeeks + 13;
        assertTrue(controller.autoCompleteRecruiting());
        assertTrue("Signing Day should publish as the new season begins",
                hasStoryStarting("Signing Day>"));
    }

    @Test
    public void tvProfitSharing_paysMemberBudgets() {
        Conference conf = league.getConferences().get(0);
        Team member = conf.confTeams.get(0);
        conf.confTV = true;
        conf.confTVContract = 2;
        conf.confTVBonus = 500;
        int before = member.getTeamBudget();

        conf.reviewConfTVDeal();

        assertEquals("Profit sharing should deposit the TV bonus",
                before + 500, member.getTeamBudget());
    }
}
