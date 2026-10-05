package simulation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** NIL collective economy: tier cost curve, offseason upgrades, UI snapshot. */
public class NilCollectiveTest {

    private League league;

    @Before
    public void setUp() {
        FileSystemResourceProvider resources = new FileSystemResourceProvider(System.getProperty("user.dir"));
        league = new League(
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_PLAYER_NAMES),
                resources.getString(PlatformResourceProvider.KEY_LEAGUE_LAST_NAMES),
                resources.getString(PlatformResourceProvider.KEY_CONFERENCES),
                resources.getString(PlatformResourceProvider.KEY_TEAMS),
                resources.getString(PlatformResourceProvider.KEY_BOWLS),
                false, false);
        league.setPlatformResourceProvider(resources);
    }

    @Test
    public void costCurve_isLinearAndBounded() {
        assertEquals(League.NIL_TIER_COST_STEP, League.nilCollectiveUpgradeCost(1));
        assertEquals(4 * League.NIL_TIER_COST_STEP, League.nilCollectiveUpgradeCost(4));
        assertEquals(0, League.nilCollectiveUpgradeCost(0));
        assertEquals(0, League.nilCollectiveUpgradeCost(League.NIL_MAX_TIER + 1));
    }

    @Test
    public void typicalSeasonBudget_canFundAnEarlyTier() {
        // Season-end athletic budgets land around $8K-$25K for most programs;
        // the first two tiers must be reachable or the collective never matters.
        assertTrue(League.nilCollectiveUpgradeCost(1) < 10_000);
        assertTrue(League.nilCollectiveUpgradeCost(2) < 20_000);
    }

    @Test
    public void upgrade_spendsBudgetOnlyWhenAffordable() {
        Team rich = league.getTeamList().get(0);
        Team poor = league.getTeamList().get(1);
        rich.nilCollectiveLevel = 1;
        rich.setTeamBudget(League.nilCollectiveUpgradeCost(2) + 500);
        poor.nilCollectiveLevel = 1;
        poor.setTeamBudget(League.nilCollectiveUpgradeCost(2) - 1);

        league.upgradeNilCollectives();

        assertEquals(2, rich.nilCollectiveLevel);
        assertTrue(rich.nilCollectiveUpgrade);
        assertEquals(500, rich.getTeamBudget());
        assertEquals(1, poor.nilCollectiveLevel);
        assertFalse(poor.nilCollectiveUpgrade);
    }

    @Test
    public void upgrade_stopsAtMaxTier() {
        Team t = league.getTeamList().get(0);
        t.nilCollectiveLevel = League.NIL_MAX_TIER;
        t.setTeamBudget(10_000_000);
        league.upgradeNilCollectives();
        assertEquals(League.NIL_MAX_TIER, t.nilCollectiveLevel);
        assertEquals(10_000_000, t.getTeamBudget());
    }

    @Test
    public void status_reportsRealEffectsAndNextTier() {
        Team t = league.getTeamList().get(0);
        t.nilCollectiveLevel = 3;
        t.setTeamBudget(League.nilCollectiveUpgradeCost(4) / 2);
        NilCollectiveStatus s = NilCollectiveStatus.of(t);

        assertEquals(3, s.tier);
        assertEquals(4, s.nextTier);
        assertEquals(League.nilCollectiveUpgradeCost(4), s.nextTierCost);
        assertEquals(50, s.progressPercent());
        assertFalse(s.canAffordNextTier());
        assertEquals(t.getWeeklyCollectiveStipend(), s.weeklyStipend);
        assertEquals(3 * SimulationFacade.RECRUITING_BUDGET_PER_NIL_TIER, s.recruitingBonus);
        assertEquals((t.getHomeGameRevenueMultiplier() - 1.0) * 100.0, s.homeRevenueBonusPct, 1e-9);
        assertEquals("Tier 3 / " + League.NIL_MAX_TIER, s.tierLabel());
        assertTrue(s.effectsLine(), s.effectsLine().contains("/wk boosters"));
        assertTrue(s.nextTierLine(), s.nextTierLine().startsWith("Next: Tier 4 at $"));
    }

    @Test
    public void status_tierZeroAndMaxedCopy() {
        Team t = league.getTeamList().get(0);
        t.nilCollectiveLevel = 0;
        if (t.getHeadCoach() != null) t.getHeadCoach().coachSkillRanksBits = 0;
        t.setTeamBudget(0);
        NilCollectiveStatus zero = NilCollectiveStatus.of(t);
        assertTrue(zero.effectsLine(), zero.effectsLine().startsWith("No collective yet"));
        assertEquals(0, zero.progressPercent());

        t.nilCollectiveLevel = League.NIL_MAX_TIER;
        NilCollectiveStatus max = NilCollectiveStatus.of(t);
        assertTrue(max.isMaxed());
        assertEquals(100, max.progressPercent());
        assertTrue(max.nextTierLine(), max.nextTierLine().startsWith("Maxed out"));
    }

    @Test
    public void status_nullTeamIsSafe() {
        NilCollectiveStatus s = NilCollectiveStatus.of(null);
        assertEquals(0, s.tier);
        assertEquals(1, s.nextTier);
    }

    @Test
    public void newsStory_capsListAndAlwaysNamesUserProgram() {
        java.util.List<Team> teams = league.getTeamList();
        league.userTeam = teams.get(teams.size() - 1);
        for (Team t : teams) {
            t.nilCollectiveUpgrade = true;
            t.nilCollectiveLevel = 1;
        }
        teams.get(0).nilCollectiveLevel = 5;
        String story = league.buildNilCollectiveStory();
        assertTrue(story, story.startsWith(teams.size() + " programs expanded"));
        assertTrue(story, story.contains(teams.get(0).getName() + " : NIL Tier 5"));
        assertTrue(story, story.contains("Your program: " + league.userTeam.getName()));
        assertTrue(story, story.contains("...and " + (teams.size() - League.NIL_STORY_MAX_LISTED) + " more."));
        int listed = story.split(" : NIL Tier ", -1).length - 1;
        assertEquals(League.NIL_STORY_MAX_LISTED, listed);
    }

    @Test
    public void newsStory_nullWhenNobodyExpanded() {
        for (Team t : league.getTeamList()) t.nilCollectiveUpgrade = false;
        assertEquals(null, league.buildNilCollectiveStory());
    }
}
